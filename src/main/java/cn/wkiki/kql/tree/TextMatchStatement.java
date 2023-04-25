package cn.wkiki.kql.tree;

/**
 * 全文查询表达式
 */
public interface TextMatchStatement {

    /**
     * 设置字段名节点
     * @param fieldNameTreeNode
     */
    void setFieldNameTreeNode(TreeNode.FieldNameTreeNode fieldNameTreeNode);

    /**
     * 设置字面量节点
     * @param literalValueTreeNode
     */
    void setLiteralValueTreeNode(TreeNode.LiteralValueTreeNode literalValueTreeNode);
}
