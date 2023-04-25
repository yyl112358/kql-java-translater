package cn.wkiki.kql.tree;


import cn.wkiki.kql.Token;

/**
 * 关系查询表达式
 */
public interface RelationCalcStatement {

    /**
     * 设置字段名节点
     * @param fieldNameTreeNode
     */
    void setFieldNameTreeNode(TreeNode.FieldNameTreeNode fieldNameTreeNode);

    /**
     * 设置关系运算符节点
     * @param relationCalcToken
     */
    void setRelationCalcToken(Token relationCalcToken);

    /**
     * 设置字面量值节点
     * @param literalValueTreeNode
     */
    void setLiteralValueTreeNode(TreeNode.LiteralValueTreeNode literalValueTreeNode);

    /**
     * 设置短语查询类型的字面量节点
     * @param phraseLiteralValueTreeNode
     */
    void setPhraseLiteralValueTreeNode(TreeNode.PhraseLiteralValueTreeNode phraseLiteralValueTreeNode);
}
