package cn.wkiki.kql.tree.TreeNodeImpl;

import cn.wkiki.kql.Token;
import cn.wkiki.kql.TranslateContext;
import cn.wkiki.kql.exception.DSLSemanticsException;
import cn.wkiki.kql.exception.DSLSyntaxException;
import cn.wkiki.kql.model.DetailQueryParamBody;
import cn.wkiki.kql.queryUnit.QueryUnit;
import cn.wkiki.kql.tree.AggregationStatement;
import cn.wkiki.kql.tree.TreeNode;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * 聚合查询节点
 */
public class AggregationTreeNode extends TreeNode implements AggregationStatement {

    TreeNode filterTree;

    /**
     * 聚合选择使用的字段 字符串常量节点
     */
    @Getter
    LiteralValueTreeNode literalValueTreeNode;

    /**
     * 聚合函数token
     */
    @Getter
    Token aggregationFunctionToken;

    @Override
    public void setFilterTree(TreeNode filterTree) {
        if (filterTree == null) {
            throw new DSLSemanticsException("聚合语句必须要有过滤子树");
        }
        if (filterTree.getClass().equals(BracketTreeNode.class)) {
            this.filterTree = filterTree;
        } else {
            throw new DSLSemanticsException("聚合语句的过滤子句必须为括号子树");
        }
    }

    @Override
    public void setAggregationFiledToken(LiteralValueTreeNode literalValueTreeNode) {
        this.literalValueTreeNode = literalValueTreeNode;
    }

    @Override
    public void setAggregationFunctionToken(Token aggregationFunctionToken) {
        switch (aggregationFunctionToken.getType()) {
            case avg:
            case max:
            case min:
            case sum:
            case count:
            case terms:
                this.aggregationFunctionToken = aggregationFunctionToken;
                break;
            default:
                String errMsg = String.format("未知的聚合类型[type:%s,value:%s]", aggregationFunctionToken.getType(), aggregationFunctionToken.getValue());
                throw new DSLSyntaxException(aggregationFunctionToken, aggregationFunctionToken.getValue(), errMsg);
        }
    }

    @Override
    public String toSourceStr() {
        return "group by(" + literalValueTreeNode.getLiteralValueToken().getValue() + ") " + aggregationFunctionToken.getValue();
    }

    @Override
    public QueryUnit toTranslateUnit(TranslateContext translateContext) {
        return this.filterTree.toTranslateUnit(translateContext);
    }

    @Override
    public DetailQueryParamBody toQueryParamBody(TranslateContext translateContext) {
        DetailQueryParamBody result = filterTree.toQueryParamBody(translateContext);
        Token aggFunctionToken = this.getAggregationFunctionToken();
        String aggFieldName = this.getLiteralValueTreeNode().getLiteralValueToken().getValue();
        if (translateContext != null && translateContext.getFields().stream().noneMatch(t -> t.getName().equals(aggFieldName))) {
            String errMsg = String.format("要聚合的字段[%s]不在索引的属性列表中", aggFieldName);
            throw new DSLSemanticsException(errMsg);
        }
        if (result.getAggs() == null) {
            result.setAggs(new HashMap<>());
        }
        String aggName = "";
        Map<String, Map<String, String>> aggBody = new HashMap();
        switch (aggFunctionToken.getType()) {
            case avg:
                aggName = "avg";
                break;
            case stat:
                aggName = "stats";
            case max:
                aggName = "max";
                break;
            case min:
                aggName = "min";
                break;
            case sum:
                aggName = "sum";
                break;
            case count:
                aggName = "value_count";
                break;
            case terms:
                aggName = "terms";
                break;
            default:
                throw new DSLSemanticsException("未知的聚合类型:" + aggFunctionToken.getValue());

        }
        HashMap<String, String> aggsConfigMap = new HashMap<>();
        aggsConfigMap.put("field", aggFieldName);
        aggBody.put(aggName, aggsConfigMap);
        result.getAggs().put(aggName, aggBody);
        return result;
    }
}
