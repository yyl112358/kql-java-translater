package cn.wkiki.kql.es;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IndexField {
    /**
     * 字段名
     */
    private String name;
    /**
     * 字段类型
     */
    private String type;
    /**
     * 字段格式
     */
    private String format;

}
