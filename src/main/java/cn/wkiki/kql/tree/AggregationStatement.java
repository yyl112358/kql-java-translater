package cn.wkiki.kql.tree;


import cn.wkiki.kql.Token;
import cn.wkiki.kql.tree.TreeNodeImpl.LiteralValueTreeNode;

/**
 * 聚合计算表达式
 */
public interface AggregationStatement {

    void setFilterTree(TreeNode filterTree);

    /**
     * 设置聚合字段选择的字面量节点
     * @param literalValueTreeNode
     */
    void setAggregationFiledToken(LiteralValueTreeNode literalValueTreeNode);

    /**
     * 设置聚合函数token
     * @param aggregationFunctionToken
     */
    void setAggregationFunctionToken(Token aggregationFunctionToken);
}
