package cn.wkiki.kql.tree;

import cn.wkiki.kql.Token;
import cn.wkiki.kql.exception.LimitSizeFormatException;

/**
 * 限制结果集数量表达式
 */
public interface LimitResultStatement {

    /**
     * 设置前置的查询表达式
     * @param searchStatement
     */
    void setSearchStatement(TreeNode searchStatement);

    /**
     * limit token
     * @param limitToken
     */
    void setLimitToken(Token limitToken);

    /**
     * 设置配置的限制值 token 若 token格式或值非法会则会抛出 <br/>
     * {@link LimitSizeFormatException}
     * @param limitValueToken
     * @throws LimitSizeFormatException
     */
    void setLimitValueToken(Token limitValueToken) throws LimitSizeFormatException;
}
