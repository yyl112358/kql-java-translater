package cn.wkiki.kql;

import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;
import java.util.Optional;

/**
 * dsl token对象
 */
@Setter
@Getter
public class Token {

    public Token(){}

    public Token(String value,int startIndex,Type type){
        this.value = value;
        this.startIndex = startIndex;
        this.type = type;
    }

    /**
     * token 字符值
     */
    private String value;
    /**
     * token 在查询语句的起始位置
     */
    private int startIndex;
    /**
     * token的类型
     */
    private Type type;

    @Override
    public String toString() {
        return "[value:"+getValue()+",startIndex:"+startIndex+",tokenType:"+type.name()+"]";
    }

    /**
     * token 类型
     */
    public enum Type{
        // 标示符
        identifier(null),
        colon(":"),
        quotes("\""),
        strEscape("\\"),
        lt("<"),
        gt(">"),
        lte("<="),
        gte(">="),
        and("and"),
        not("not"),
        or("or"),
        lbracket("("),
        rbracket(")"),
        lbrace("{"),
        rbrace("}"),
        // aggregation
        group("group"),
        by("by"),
        limit("limit"),
        order("order"),
        // aggregation function
        avg("avg()"),
        stat("stat()"),
        min("min()"),
        max("max()"),
        sum("sum()"),
        terms("terms()"),
        count("count()"),
        // 常量
        literalValue(null);

        /**
         * 标示符为语言保留字时的值
         */
        final String value;

        /**
         * 关键字的最大文本长度
         */
        public static final int KEY_WORD_MAX_LEN;

        Type(String value){
            this.value = value;
        }

        public String getValue(){
            return this.value;
        }

        /**
         * 判断给定的字符是否匹配到关键字
         * @param ch 要判断的字符
         * @return 字符对应的关键字类型 或 null(字符不是关键字)
         */
        public static Type isLangKeyWord(char ch){
            Type result = null;
            Optional<Type> matchedTypeOpt = Arrays.stream(Type.values()).filter(t -> t.getValue()!= null
                    && t.getValue().length() == 1
                    && t.getValue().charAt(0) == ch).findAny();
            if(matchedTypeOpt.isPresent()){
                result = matchedTypeOpt.get();
            }
            return result;
        }

        /**
         * 判断规定的字符串是否匹配到关键字
         * @param str 要判断的字符串
         * @return 字符串对应的关键字类型 或 null(字符串不是关键字)
         */
        public static Type isLangKeyWord(String str){
            if(str.length() == 1){
                return isLangKeyWord(str.charAt(0));
            }else{
                Optional<Type> matchedTypeOpt = Arrays.stream(Type.values()).filter(t -> t.getValue()!=null
                        && t.getValue().equals(str)).findAny();
                return matchedTypeOpt.isPresent()?matchedTypeOpt.get():null;
            }
        }

        static {
            KEY_WORD_MAX_LEN = Arrays.stream(Type.values()).map(t->t.getValue() == null?0:t.getValue().length()).max(Integer::compareTo).get();
        }
    }

    /**
     * 创建一个自身的拷贝
     */
    public Token copySelf(){
        Token result = new Token();
        result.setValue(value);
        result.setType(type);
        result.setStartIndex(startIndex);
        return result;
    }
}
