package cn.wkiki.kql.tree.TreeNodeImpl;

import cn.wkiki.kql.Token;
import cn.wkiki.kql.TranslateContext;
import cn.wkiki.kql.queryUnit.MultiMatchQuery;
import cn.wkiki.kql.queryUnit.QueryUnit;
import cn.wkiki.kql.tree.TreeNode;
import lombok.Getter;

/**
 * 字面量节点
 */
@Getter
public class LiteralValueTreeNode extends TreeNode {
    /**
     * 字面量
     */
    @Getter
    Token literalValueToken;

    public LiteralValueTreeNode(Token literalValue) {
        this.literalValueToken = literalValue;
    }

    @Override
    public String toSourceStr() {
        return literalValueToken.getValue();
    }

    @Override
    public QueryUnit toTranslateUnit(TranslateContext translateContext) {
        return new MultiMatchQuery(literalValueToken.getValue(), false);
    }
}
