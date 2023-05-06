package cn.wkiki.kql.exception;


import cn.wkiki.kql.Token;

/**
 * 语法单位期望的指定类型的token不存在异常
 */
public class DSLExpectTokenNotExistException extends DSLSyntaxException{

    public DSLExpectTokenNotExistException(Token nearToken, String source, String noticeUserErrMsg) {
        super(nearToken, source, noticeUserErrMsg);
    }
}
