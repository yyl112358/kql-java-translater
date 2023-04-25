package cn.wkiki.kql.queryUnit;

import lombok.Getter;
import lombok.Setter;

/**
 * 属性查询单元
 */
@Getter
@Setter
public abstract class FieldSearchUnit implements QueryUnit{
    /**
     * 要进行过滤查询的属性名
     */
    String filteredFieldName;
}
