package cn.wkiki.kql.queryUnit;

/**
 * 空节点翻译单元，用来处理空节点
 * {@link cn.wkiki.kql.tree.TreeNode.EmptyTreeNode}
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
