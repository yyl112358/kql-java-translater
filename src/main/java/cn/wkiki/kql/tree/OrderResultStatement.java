package cn.wkiki.kql.tree;

import cn.wkiki.kql.Token;

/**
 * 结果排序表达式
 */
public interface OrderResultStatement {
    /**
     * 设置前置的查询树
     * @param searchTree
     */
    void setSearchTree(TreeNode searchTree);

    /**
     * 设置排序属性token
     * @param orderPropToken
     */
    void setOrderPropToken(Token orderPropToken);

    /**
     * 设置指定排序方法的
     * @param orderMethodToken
     */
    void setOrderMethodToken(Token orderMethodToken);
}
