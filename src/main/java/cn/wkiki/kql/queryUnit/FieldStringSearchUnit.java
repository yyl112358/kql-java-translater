package cn.wkiki.kql.queryUnit;

import lombok.Getter;
import lombok.Setter;

/**
 * 属性文本查询基础类
 */
@Getter
@Setter
public abstract class FieldStringSearchUnit extends FieldSearchUnit{
    /**
     * 要查询的字符串
     */
    String searchStr;
}
