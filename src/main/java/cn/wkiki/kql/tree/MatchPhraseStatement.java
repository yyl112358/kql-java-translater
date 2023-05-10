package cn.wkiki.kql.tree;

import cn.wkiki.kql.tree.TreeNodeImpl.FieldSearchTreeNode.FieldNameTreeNode;
import cn.wkiki.kql.tree.TreeNodeImpl.PhraseLiteralValueTreeNode;

/**
 * 短语查询表达式
 */
public interface MatchPhraseStatement {

    /**
     * 设置属性名节点
     * @param fieldNameTreeNode
     */
    void setFieldNameTreeNode(FieldNameTreeNode fieldNameTreeNode);
    /**
     * 设置短语查询字面量节点
     * @param literalValueTreeNode
     */
    void setPhaseLiteralValueTreeNode(PhraseLiteralValueTreeNode literalValueTreeNode);
}
