package cn.wkiki.kql.tree.TreeNodeImpl;

import cn.wkiki.kql.TranslateContext;
import cn.wkiki.kql.queryUnit.MultiMatchQuery;
import cn.wkiki.kql.queryUnit.QueryUnit;
import cn.wkiki.kql.tree.TreeNode;
import lombok.Getter;
import lombok.Setter;

/**
 * 多字段查询节点
 */
public class MultiFieldMatchTreeNode extends TreeNode {
    /**
     * 要匹配的字面量
     */
    @Getter
    @Setter
    private LiteralValueTreeNode literalValueTreeNode;

    @Override
    public String toSourceStr() {
        return literalValueTreeNode.toSourceStr();
    }

    @Override
    public QueryUnit toTranslateUnit(TranslateContext translateContext) {
        return new MultiMatchQuery(getLiteralValueTreeNode().getLiteralValueToken().getValue(), false);
    }
}
