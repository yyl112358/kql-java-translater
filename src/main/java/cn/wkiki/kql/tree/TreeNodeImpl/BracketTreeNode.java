package cn.wkiki.kql.tree.TreeNodeImpl;

import cn.wkiki.kql.Token;
import cn.wkiki.kql.TranslateContext;
import cn.wkiki.kql.model.DetailQueryParamBody;
import cn.wkiki.kql.queryUnit.QueryUnit;
import cn.wkiki.kql.tree.QuotesStatement;
import cn.wkiki.kql.tree.TreeNode;
import lombok.Getter;
import lombok.Setter;

/**
 * 括号节点
 */
public class BracketTreeNode extends TreeNode implements QuotesStatement {

    /**
     * 左括号token
     */
    @Getter
    @Setter
    Token lbracketToken;
    /**
     * 右括号token
     */
    @Getter
    @Setter
    Token rbracketToken;

    @Getter
    TreeNode innerNode;

    @Override
    public void setInnerStatement(TreeNode innerNode) {
        this.innerNode = innerNode;
    }

    @Override
    public String toSourceStr() {
        return lbracketToken.getValue() + innerNode.toSourceStr() + rbracketToken.getValue();
    }

    @Override
    public String toTranslateStr() {
        return lbracketToken.getValue() + innerNode.toTranslateStr() + rbracketToken.getValue();
    }

    @Override
    public QueryUnit toTranslateUnit(TranslateContext translateContext) {
        return innerNode.toTranslateUnit(translateContext);
    }

    @Override
    public DetailQueryParamBody toQueryParamBody(TranslateContext translateContext) {
        return innerNode.toQueryParamBody(translateContext);
    }
}
