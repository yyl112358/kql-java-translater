package cn.wkiki.kql.tree;

/**
 * 短语查询表达式
 */
public interface MatchPhraseStatement {

    /**
     * 设置属性名节点
     * @param fieldNameTreeNode
     */
    void setFieldNameTreeNode(TreeNode.FieldNameTreeNode fieldNameTreeNode);
    /**
     * 设置短语查询字面量节点
     * @param literalValueTreeNode
     */
    void setPhaseLiteralValueTreeNode(TreeNode.PhraseLiteralValueTreeNode literalValueTreeNode);
}
