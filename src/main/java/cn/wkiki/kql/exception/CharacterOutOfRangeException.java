package cn.wkiki.kql.exception;

/**
 * 消费源码字符串时索引超出源码合法位置异常
 */
public class CharacterOutOfRangeException extends RuntimeException{

    int expectIndex;

    int sourceLen;

    public CharacterOutOfRangeException(int expectIndex,int sourceLen,String message){
        super(message);
        this.expectIndex = expectIndex;
        this.sourceLen = sourceLen;
    }
}
