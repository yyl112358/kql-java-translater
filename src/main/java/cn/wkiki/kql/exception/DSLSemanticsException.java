package cn.wkiki.kql.exception;


/**
 * 语义异常s
 */
public class DSLSemanticsException extends RuntimeException {
    public DSLSemanticsException(String errMsg){
        super(errMsg);
    }
}
