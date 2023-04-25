package cn.wkiki.kql.tree;

/**
 * 括号提升优先级表达式
 */
public interface QuotesStatement {

    /**
     * 设置内部节点
     * @param innerNode
     */
    void setInnerStatement(TreeNode innerNode);
}
