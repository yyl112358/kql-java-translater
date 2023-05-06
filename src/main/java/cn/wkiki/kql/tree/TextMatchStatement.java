package cn.wkiki.kql.tree;

import cn.wkiki.kql.tree.TreeNodeImpl.FieldNameTreeNode;
import cn.wkiki.kql.tree.TreeNodeImpl.LiteralValueTreeNode;

/**
 * 全文查询表达式
 */
public interface TextMatchStatement {

    /**
     * 设置字段名节点
     * @param fieldNameTreeNode
     */
    void setFieldNameTreeNode(FieldNameTreeNode fieldNameTreeNode);

    /**
     * 设置字面量节点
     * @param literalValueTreeNode
     */
    void setLiteralValueTreeNode(LiteralValueTreeNode literalValueTreeNode);
}
