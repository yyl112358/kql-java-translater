package cn.wkiki.kql.exception;

import cn.wkiki.kql.Token;

/**
 * 限制结果集数量表达式 限制值格式异常<br/>
 * 限制值的token value 必须可以使用Integer.parseInt() 方法转换为Integer值，且值必须大于0
 */
public class LimitSizeFormatException extends DSLSyntaxException {

    public LimitSizeFormatException(Token nearToken, String source, String noticeUserErrMsg) {
        super(nearToken, source, noticeUserErrMsg);
    }
}
