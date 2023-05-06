package cn.wkiki.kql.queryUnit;

import cn.wkiki.kql.tree.TreeNodeImpl.EmptyTreeNode;

/**
 * 空节点翻译单元，用来处理空节点
 * {@link EmptyTreeNode}
 */
public class EmptyUnit implements QueryUnit{
    final String EMPTY_STR ="";

    @Override
    public String toESQueryJsonEntity() {
        return EMPTY_STR;
    }

    @Override
    public String prettyToESQueryJsonEntity() {
        return EMPTY_STR;
    }
}
