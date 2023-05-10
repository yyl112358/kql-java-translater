package cn.wkiki.kql.tree.TreeNodeImpl.FieldSearchTreeNode;

import cn.wkiki.kql.Token;
import cn.wkiki.kql.TranslateContext;
import cn.wkiki.kql.exception.DSLSemanticsException;
import cn.wkiki.kql.optimizer.LogicReductionOptimizer;
import cn.wkiki.kql.queryUnit.BooleanQuery;
import cn.wkiki.kql.queryUnit.NestedFieldQuery;
import cn.wkiki.kql.queryUnit.QueryUnit;
import cn.wkiki.kql.tree.NestedMatchStatement;
import cn.wkiki.kql.tree.TreeNode;
import cn.wkiki.kql.tree.TreeNodeImpl.LogicCalcTreeNode;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.stream.Collectors;

/**
 * 内部属性查询节点
 */
public class NestedMatchTreeNode extends FieldSearchTreeNode implements NestedMatchStatement {
    private Token outFieldNameToken;
    private TreeNode nestedFilterTree;

    @Override
    public void setOutFieldNameToken(Token outFieldNameToken) {
        if(outFieldNameToken.getType().equals(Token.Type.identifier)){
            this.outFieldNameToken =  outFieldNameToken;
            this.fieldNameTreeNode = new FieldNameTreeNode(outFieldNameToken);
        }else{
            String errMsg = String.format("内部属性查询树的外部属性名token需为 identifier类型，给定token为[%s]", outFieldNameToken);
            throw new DSLSemanticsException(errMsg);
        }
    }

    @Override
    public void setNestedFilterTree(TreeNode innerFilterTreeNode) {
        validateNestedTree(innerFilterTreeNode);
        this.nestedFilterTree = innerFilterTreeNode;
    }

    /**
     * 检查语句中内部属性查询子树是否合法
     * @param innerFilterTreeNode 内部属性查询子树的子树过滤树
     */
    protected void validateNestedTree(TreeNode innerFilterTreeNode){
        Class<?>[] directAcceptedClazz = new Class[]{
                LogicCalcTreeNode.class,
                MatchPhraseTreeNode.class,
                RelationCalcTreeNode.class,
                TextMatchTreeNode.class,
                NestedMatchTreeNode.class  // 允许内部属性查询内嵌套内部属性查询,及执行 a.b.c 查询
        };
        Class<?>[] leafNodeAcceptedClazz =Arrays.stream(directAcceptedClazz)
                .filter(t->!t.equals(LogicCalcTreeNode.class)).toArray(Class[]::new);
        boolean directMatch = Arrays.stream(directAcceptedClazz).anyMatch(t->t.equals(innerFilterTreeNode.getClass()));
        if(!directMatch){
            String supportClazz = Arrays.stream(directAcceptedClazz).map(Class::getSimpleName).collect(Collectors.joining(";"));
            String errMsg = String.format("内部属性查询{}内接受的子树类型为[%s],当前提交的树类型为[%s]", supportClazz,innerFilterTreeNode.getClass().getSimpleName());
            throw new DSLSemanticsException(errMsg);
        }
        Queue<TreeNode> queue = new LinkedList<>();
        if(innerFilterTreeNode.getClass().equals(LogicCalcTreeNode.class)){
            // 查询子节点是否都是简单查询
            LogicCalcTreeNode logicCalcTreeNode =  (LogicCalcTreeNode)innerFilterTreeNode;
            if(logicCalcTreeNode.getLeftSubNode()!=null){
                queue.add(logicCalcTreeNode.getLeftSubNode().getSubTreeNode());
            }
            queue.add(logicCalcTreeNode.getRightSubNode().getSubTreeNode());
        }else if (innerFilterTreeNode.getClass().equals(NestedMatchTreeNode.class)){
            if(((NestedMatchTreeNode) innerFilterTreeNode).nestedFilterTree != null){
                queue.add(((NestedMatchTreeNode) innerFilterTreeNode).nestedFilterTree);
            }
        }else{
            queue.add(innerFilterTreeNode);
        }
        do{
            TreeNode tail = queue.poll();
            if(tail!=null){
                if(tail.getClass().equals(LogicCalcTreeNode.class)){
                    LogicCalcTreeNode tailAsLogic = (LogicCalcTreeNode)tail;
                    if(tailAsLogic.getLeftSubNode()!=null){
                        queue.add(tailAsLogic.getLeftSubNode().getSubTreeNode());
                    }
                    queue.add(tailAsLogic.getRightSubNode().getSubTreeNode());
                }else{
                    boolean leafMatch = Arrays.stream(leafNodeAcceptedClazz).anyMatch(t->t.equals(tail.getClass()));
                    if(!leafMatch){
                        String supportClazz = Arrays.stream(leafNodeAcceptedClazz).map(Class::getName).collect(Collectors.joining(";"));
                        String errMsg = String.format("内部属性查询{}内仅接受叶子节点类型类型为[%s]的树,扫描到非法的叶子结点[%s],类型[%s]", supportClazz,tail.toSourceStr(),tail.getClass().getName());
                        throw new DSLSemanticsException(errMsg);
                    }
                    if(NestedMatchTreeNode.class.isAssignableFrom(tail.getClass())){
                        ((NestedMatchTreeNode) tail).setNestedProp(true);
                        String nestPropName = "";
                        if(this.isNestedProp()){
                            nestPropName = String.format("%s.%s", this.getNestedPropName(),((FieldSearchTreeNode) tail).getFieldNameTreeNode().getFieldNameToken().getValue());
                        }else{
                            nestPropName = String.format("%s.%s", outFieldNameToken.getValue(),((FieldSearchTreeNode) tail).getFieldNameTreeNode().getFieldNameToken().getValue());
                        }
                        ((NestedMatchTreeNode) tail).setNestedPropName(nestPropName);
                        // nested 查询内部的nested查询 在自己的上下文中单独处理，且处理后，整个子树均跳外层处理逻辑
                        ((NestedMatchTreeNode) tail).validateNestedTree(((NestedMatchTreeNode) tail).nestedFilterTree);
                        continue;
                    }
                    if(FieldSearchTreeNode.class.isAssignableFrom(tail.getClass())){
                        ((FieldSearchTreeNode) tail).setNestedProp(true);
                        String nestPropName = "";
                        if(this.isNestedProp()){
                            nestPropName = String.format("%s.%s", this.getNestedPropName(),((FieldSearchTreeNode) tail).getFieldNameTreeNode().getFieldNameToken().getValue());
                        }else{
                            nestPropName = String.format("%s.%s", outFieldNameToken.getValue(),((FieldSearchTreeNode) tail).getFieldNameTreeNode().getFieldNameToken().getValue());
                        }
                        ((FieldSearchTreeNode) tail).setNestedPropName(nestPropName);
                    }
                }
            }
        }while (queue.size() > 0);
    }

    @Override
    public String toSourceStr() {
        return String.format("%s:{%s}", this.outFieldNameToken.getValue(), this.nestedFilterTree.toSourceStr());
    }

    @Override
    public QueryUnit toTranslateUnit(TranslateContext translateContext) {
        QueryUnit nestedQueryUnit = nestedFilterTree.toTranslateUnit(translateContext);
        // 规约逻辑运算树，压缩树高度
        if(nestedQueryUnit.getClass().equals(BooleanQuery.class)){
            LogicReductionOptimizer logicReductionOptimizer = new LogicReductionOptimizer();
            nestedQueryUnit = logicReductionOptimizer.optimizeQuery(nestedQueryUnit);
        }
        String fieldName = isNestedProp()?getNestedPropName():outFieldNameToken.getValue();
        return new NestedFieldQuery(fieldName,nestedQueryUnit);
    }
}
