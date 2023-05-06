package cn.wkiki.kql.tree.TreeNodeImpl;

import cn.wkiki.kql.TranslateContext;
import cn.wkiki.kql.queryUnit.MultiMatchQuery;
import cn.wkiki.kql.queryUnit.QueryUnit;
import cn.wkiki.kql.tree.TreeNode;
import lombok.Getter;

/**
 * 短语查询字面量节点
 */
public class PhraseLiteralValueTreeNode extends TreeNode {
    /**
     * 字面量
     */
    @Getter
    String literalValue;

    public PhraseLiteralValueTreeNode(String literalValue) {
        this.literalValue = literalValue;
    }

    @Override
    public String toSourceStr() {
        return "\"" + literalValue + "\"";
    }

    @Override
    public QueryUnit toTranslateUnit(TranslateContext translateContext) {
        return new MultiMatchQuery(literalValue, true);
    }
}
