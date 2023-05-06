package cn.wkiki.kql.tree.TreeNodeImpl;

import cn.wkiki.kql.Token;
import cn.wkiki.kql.TranslateContext;
import cn.wkiki.kql.exception.DSLSemanticsException;
import cn.wkiki.kql.exception.LimitSizeFormatException;
import cn.wkiki.kql.model.DetailQueryParamBody;
import cn.wkiki.kql.queryUnit.QueryUnit;
import cn.wkiki.kql.tree.LimitResultStatement;
import cn.wkiki.kql.tree.TreeNode;

public class LimitResultTreeNode extends TreeNode implements LimitResultStatement {
    TreeNode searchStatement;

    Token limitToken;

    Token limitValueToken;

    int limitSize = 0;

    @Override
    public void setSearchStatement(TreeNode searchStatement) {
        if (searchStatement != null) {
            this.searchStatement = searchStatement;
        }
    }

    @Override
    public void setLimitToken(Token limitToken) {
        if (limitToken != null && limitToken.getType().equals(Token.Type.limit)) {
            this.limitToken = limitToken;
        } else {
            throw new DSLSemanticsException("limit token 不可为null且仅能为 limit");
        }
    }

    @Override
    public void setLimitValueToken(Token limitValueToken) throws LimitSizeFormatException {
        if (limitValueToken != null) {
            if (limitValueToken.getType().equals(Token.Type.identifier)
                    || limitValueToken.getType().equals(Token.Type.literalValue)) {
                this.limitValueToken = limitValueToken;
                this.limitSize = Integer.parseInt(limitValueToken.getValue());
            } else {
                throw new DSLSemanticsException("limit value token 的token类型仅可为 identifier 或 literalValue");
            }
        } else {
            throw new DSLSemanticsException("limit value token 不可为null");
        }
    }

    @Override
    public String toSourceStr() {
        return searchStatement.toSourceStr() + " " + limitToken.getValue() + " " + limitValueToken.getValue();
    }

    @Override
    public QueryUnit toTranslateUnit(TranslateContext translateContext) {
        return this.searchStatement.toTranslateUnit(translateContext);
    }

    @Override
    public DetailQueryParamBody toQueryParamBody(TranslateContext translateContext) {
        DetailQueryParamBody result = this.searchStatement.toQueryParamBody(translateContext);
        result.setSize(this.limitSize);
        return result;
    }
}
