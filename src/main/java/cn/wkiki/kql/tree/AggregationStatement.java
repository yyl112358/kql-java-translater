package cn.wkiki.kql.tree;


import cn.wkiki.kql.Token;

public interface AggregationStatement {

    void setFilterTree(TreeNode filterTree);

    /**
     * 设置聚合字段选择的字面量节点
     * @param literalValueTreeNode
     */
    void setAggregationFiledToken(TreeNode.LiteralValueTreeNode literalValueTreeNode);

    /**
     * 设置聚合函数token
     * @param aggregationFunctionToken
     */
    void setAggregationFunctionToken(Token aggregationFunctionToken);
}
