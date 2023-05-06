package cn.wkiki.kql.tree;


import cn.wkiki.kql.Token;
import cn.wkiki.kql.tree.TreeNodeImpl.FieldNameTreeNode;
import cn.wkiki.kql.tree.TreeNodeImpl.LiteralValueTreeNode;
import cn.wkiki.kql.tree.TreeNodeImpl.PhraseLiteralValueTreeNode;

/**
 * 关系查询表达式
 */
public interface RelationCalcStatement {

    /**
     * 设置字段名节点
     * @param fieldNameTreeNode
     */
    void setFieldNameTreeNode(FieldNameTreeNode fieldNameTreeNode);

    /**
     * 设置关系运算符节点
     * @param relationCalcToken
     */
    void setRelationCalcToken(Token relationCalcToken);

    /**
     * 设置字面量值节点
     * @param literalValueTreeNode
     */
    void setLiteralValueTreeNode(LiteralValueTreeNode literalValueTreeNode);

    /**
     * 设置短语查询类型的字面量节点
     * @param phraseLiteralValueTreeNode
     */
    void setPhraseLiteralValueTreeNode(PhraseLiteralValueTreeNode phraseLiteralValueTreeNode);
}
