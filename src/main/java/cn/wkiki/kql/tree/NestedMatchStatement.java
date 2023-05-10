package cn.wkiki.kql.tree;

import cn.wkiki.kql.Token;

/**
 * 内部属性查询表达式
 */
public interface NestedMatchStatement {

    /**
     * 设置外部属性名token
     * @param outFieldNameToken
     */
    void setOutFieldNameToken(Token outFieldNameToken);

    /**
     * 内部查询使用的filterTree
     * @param innerFilterTreeNode
     */
    void setNestedFilterTree(TreeNode innerFilterTreeNode);
}
