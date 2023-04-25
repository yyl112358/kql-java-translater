package cn.wkiki.kql.exception;


import cn.wkiki.kql.Token;

public class DSLExpectTokenNotExistException extends DSLSyntaxException{

    public DSLExpectTokenNotExistException(Token nearToken, String source, String noticeUserErrMsg) {
        super(nearToken, source, noticeUserErrMsg);
    }
}
