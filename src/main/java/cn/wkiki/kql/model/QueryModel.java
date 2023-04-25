package cn.wkiki.kql.model;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class QueryModel {
    /**
     * query结构
     */
    private Map<String,Object> query;

    /**
     * 聚合配置
     */
    private Map<String,Object> aggs;
    /**
     * 查询size
     */
    private int size = 10;
}
