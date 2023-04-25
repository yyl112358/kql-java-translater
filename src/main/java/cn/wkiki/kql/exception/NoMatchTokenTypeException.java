package cn.wkiki.kql.exception;

/**
 * 未匹配到Token类型异常
 */
public class NoMatchTokenTypeException extends RuntimeException{

    String tokenStr;

    public NoMatchTokenTypeException(String tokenStr){
        this.tokenStr = tokenStr;
    }
}
