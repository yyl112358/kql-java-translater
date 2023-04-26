package cn.wkiki.kql.exception;


/**
 * 语义异常
 */
public class DSLSemanticsException extends RuntimeException {
    public DSLSemanticsException(String errMsg){
        super(errMsg);
    }
}
