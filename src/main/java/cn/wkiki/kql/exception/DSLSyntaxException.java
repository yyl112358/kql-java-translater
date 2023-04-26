package cn.wkiki.kql.exception;


import cn.wkiki.kql.Token;

/**
 * DSL语法异常
 */
public class DSLSyntaxException extends RuntimeException{

    /**
     * 语法附近的异常Token
     */
    Token nearToken;
    /**
     * 语句源码
     */
    String source;
    /**
     * 通知用户的错误消息
     */
    String noticeUserErrMsg;

    String errMsg;

    public DSLSyntaxException(Token nearToken, String source, String noticeUserErrMsg){
        this.nearToken = nearToken;
        this.source = source;
        this.noticeUserErrMsg = noticeUserErrMsg;
        this.errMsg = getFormatMessage();
    }

    private String getFormatMessage(){
        StringBuilder stringBuilder = new StringBuilder();
        //first line
        stringBuilder.append("在单词[").append(nearToken.getValue()).append("]附近有语法异常，异常消息:[").append(noticeUserErrMsg).append("]").append("\n");
        // source line
        stringBuilder.append(source).append("\n");
        // mark line
        for (int i = 0; i < nearToken.getStartIndex(); i++) {
            stringBuilder.append(" ");
        }
        for (int i = 0; i < nearToken.getValue().length(); i++) {
            stringBuilder.append("^");
        }
        return stringBuilder.toString();
    }

    @Override
    public String getMessage(){
        return this.errMsg;
    }
}
