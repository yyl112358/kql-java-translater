package cn.wkiki.kql.tree.TreeNodeImpl;

import cn.wkiki.kql.Token;
import cn.wkiki.kql.TranslateContext;
import cn.wkiki.kql.queryUnit.BooleanQuery;
import cn.wkiki.kql.queryUnit.QueryUnit;
import cn.wkiki.kql.tree.LogicCalcStatement;
import cn.wkiki.kql.tree.TreeNode;
import lombok.Getter;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 逻辑查询节点
 */
public class LogicCalcTreeNode extends TreeNode implements LogicCalcStatement {

    @Getter
    SubTreeNode leftSubNode;
    @Getter
    Token logicCalcToken;
    @Getter
    SubTreeNode rightSubNode;

    @Override
    public void setLeftSubNode(SubTreeNode leftSubNode) {
        this.leftSubNode = leftSubNode;
    }

    @Override
    public void setLogicCalcToken(Token logicCalcToken) {
        switch (logicCalcToken.getType()) {
            case and:
            case or:
            case not:
                this.logicCalcToken = logicCalcToken;
                break;
            default:
                String errMsg = String.format("关系运算树节点，只允许使用[and,or,not]运算符，提交的运算符[%s]非法", logicCalcToken.getType().getValue());
                throw new RuntimeException(errMsg);
        }
    }

    @Override
    public void setRightSubNode(SubTreeNode rightSubNode) {
        this.rightSubNode = rightSubNode;
    }

    /**
     * 逻辑查询节点的子节点
     */
    public static class SubTreeNode extends TreeNode {

        public SubTreeNode() {
        }

        public SubTreeNode(TreeNode subTreeNode) {
            this.setSubTreeNode(subTreeNode);
        }

        final Class[] acceptSubTreeNodeClazz = new Class[]{TextMatchTreeNode.class,
                MatchPhraseTreeNode.class,
                MultiFieldMatchTreeNode.class,
                RelationCalcTreeNode.class,
                LogicCalcTreeNode.class,
                BracketTreeNode.class,
                PhraseLiteralValueTreeNode.class,
                MatchWithBracketTreeNode.class};

        @Getter
        private TreeNode subTreeNode;

        @Getter
        private Class subTreeNodeClazz;

        public void setSubTreeNode(TreeNode subTreeNode) {
            if (this.subTreeNodeClazz == null) {
                final Class subTreeNodeClazz = subTreeNode.getClass();
                if (Arrays.stream(acceptSubTreeNodeClazz).anyMatch(t -> t.equals(subTreeNodeClazz))) {
                    this.subTreeNode = subTreeNode;
                    this.subTreeNodeClazz = subTreeNodeClazz;
                } else {
                    String acceptClassStr = Arrays.stream(acceptSubTreeNodeClazz).map(Class::getSimpleName).collect(Collectors.joining(","));
                    String errMsg = String.format("逻辑查询语句子树仅支持接受[%s]类型的节点,设置的结点类型为[%s]",
                            acceptClassStr,
                            subTreeNode.getClass().getSimpleName());
                    throw new RuntimeException(errMsg);
                }
            } else {
                String errMessage = String.format("当前逻辑查询子节点已被设置类型为[type:%s]的值", subTreeNodeClazz.getName());
                throw new RuntimeException(errMessage);
            }
        }

        @Override
        public String toSourceStr() {
            return subTreeNode.toSourceStr();
        }

        @Override
        public String toTranslateStr() {
            return subTreeNode.toTranslateStr();
        }

        @Override
        public QueryUnit toTranslateUnit(TranslateContext translateContext) {
            return getSubTreeNode().toTranslateUnit(translateContext);
        }
    }

    @Override
    public String toSourceStr() {
        String result = "";
        if (leftSubNode != null) {
            result += leftSubNode.toSourceStr();
        }
        result += " " + logicCalcToken.getValue();
        if (rightSubNode != null) {
            result += " " + rightSubNode.toSourceStr();
        }
        return result;
    }

    @Override
    public String toTranslateStr() {
        String result = "";
        if (leftSubNode != null) {
            if (leftSubNode.getSubTreeNode().getClass().equals(LogicCalcTreeNode.class)) {
                result += "(" + leftSubNode.toTranslateStr() + ")";
            } else {
                result += leftSubNode.toTranslateStr();
            }
        }
        result += " " + logicCalcToken.getValue() + " ";
        if (rightSubNode != null) {
            if (rightSubNode.getSubTreeNode().getClass().equals(LogicCalcTreeNode.class)) {
                result += "(" + rightSubNode.toTranslateStr() + ")";
            } else {
                result += rightSubNode.toTranslateStr();
            }
        }
        return result;
    }

    @Override
    public QueryUnit toTranslateUnit(TranslateContext translateContext) {
        BooleanQuery result = new BooleanQuery();
        if (getLogicCalcToken().getType().equals(Token.Type.and)) {
            result.addMust(getLeftSubNode().toTranslateUnit(translateContext));
            result.addMust(getRightSubNode().toTranslateUnit(translateContext));
        }
        if (getLogicCalcToken().getType().equals(Token.Type.or)) {
            result.addShould(getLeftSubNode().toTranslateUnit(translateContext));
            result.addShould(getRightSubNode().toTranslateUnit(translateContext));
        }
        if (getLogicCalcToken().getType().equals(Token.Type.not)) {
            result.addMustNot(getRightSubNode().toTranslateUnit(translateContext));
        }
        return result;
    }
}
