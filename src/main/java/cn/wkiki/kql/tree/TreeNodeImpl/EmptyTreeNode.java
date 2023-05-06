package cn.wkiki.kql.tree.TreeNodeImpl;

import cn.wkiki.kql.TranslateContext;
import cn.wkiki.kql.queryUnit.EmptyUnit;
import cn.wkiki.kql.queryUnit.QueryUnit;
import cn.wkiki.kql.tree.TreeNode;

/**
 * 空节点，用于处理 “()” 空括号内需要有子节点的逻辑
 */
public class EmptyTreeNode extends TreeNode {
    final String EMPTY_STR = "";

    @Override
    public String toSourceStr() {
        return EMPTY_STR;
    }

    @Override
    public String toTranslateStr() {
        return EMPTY_STR;
    }

    @Override
    public QueryUnit toTranslateUnit(TranslateContext translateContext) {
        return new EmptyUnit();
    }
}
