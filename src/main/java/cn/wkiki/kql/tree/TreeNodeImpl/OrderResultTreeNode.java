package cn.wkiki.kql.tree.TreeNodeImpl;

import cn.wkiki.kql.Token;
import cn.wkiki.kql.TranslateContext;
import cn.wkiki.kql.exception.DSLSemanticsException;
import cn.wkiki.kql.model.DetailQueryParamBody;
import cn.wkiki.kql.queryUnit.QueryUnit;
import cn.wkiki.kql.tree.OrderResultStatement;
import cn.wkiki.kql.tree.TreeNode;
import org.apache.commons.collections4.CollectionUtils;

import java.util.*;

/**
 * 结果排序树节点类型
 */
public class OrderResultTreeNode extends TreeNode implements OrderResultStatement {

    private TreeNode searchTree;

    private Token orderPropToken;

    private Token orderMethodToken;

    @Override
    public void setSearchTree(TreeNode searchTree) {
        if(searchTree != null){
            this.searchTree = searchTree;
        }else{
            throw new RuntimeException("searchTree 为null");
        }
    }

    @Override
    public void setOrderPropToken(Token orderPropToken) {
        if(orderPropToken != null){
            if(orderPropToken.getType().equals(Token.Type.identifier)){
                this.orderPropToken = orderPropToken;
            }else{
                throw new DSLSemanticsException("order tree 能接受的order prop只能是字面量 token");
            }
        }else{
            throw new RuntimeException("orderPropToken 为null");
        }
    }

    @Override
    public void setOrderMethodToken(Token orderMethodToken) {
        if(orderMethodToken != null){
            if(orderMethodToken.getType().equals(Token.Type.desc)
                    || orderMethodToken.getType().equals(Token.Type.asc)){
                this.orderMethodToken = orderMethodToken;
            }else{
                throw new DSLSemanticsException("order tree 能接受的order method方法只能是 asc 或 desc");
            }
        }else{
            throw new RuntimeException("orderMethodToken 为null");
        }
    }

    @Override
    public String toSourceStr() {
        return String.format("%s order by(%s %s)", searchTree.toSourceStr(),orderPropToken.getValue(),orderMethodToken.getValue());
    }

    @Override
    public QueryUnit toTranslateUnit(TranslateContext translateContext) {
        return searchTree.toTranslateUnit(translateContext);
    }

    @Override
    public DetailQueryParamBody toQueryParamBody(TranslateContext translateContext) {
        DetailQueryParamBody result = this.searchTree.toQueryParamBody(translateContext);
        List<Map<String,String>> maps = result.getSort();
        String orderProp = this.orderPropToken.getValue();
        String orderMethod = this.orderMethodToken.getValue();
        if(CollectionUtils.isNotEmpty(maps)){
            Optional<Map<String,String>> orderedPropOpt = maps.stream().filter(t->t.containsKey(orderProp)).findFirst();
            if(orderedPropOpt.isPresent()){
                orderedPropOpt.get().put(orderMethod, orderMethod);
            }else{
                Map<String,String> orderMap = new HashMap<>();
                orderMap.put(orderProp,orderMethod);
                maps.add(orderMap);
            }
        }else{
            List<Map<String,String >> newMaps = new ArrayList<>();
            Map<String,String> orderMap = new HashMap<>();
            orderMap.put(orderProp,orderMethod);
            newMaps.add(orderMap);
            result.setSort(newMaps);
        }
        return result;
    }
}
