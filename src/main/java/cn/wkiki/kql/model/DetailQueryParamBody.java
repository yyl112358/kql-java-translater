package cn.wkiki.kql.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class DetailQueryParamBody {
    /**
     * query结构
     */
    private Map<String,Object> query;

    /**
     * 聚合配置
     */
    private Map<String,Object> aggs;

    /**
     * 排序配置
     */
    private List<Map<String,String>> sort;
    /**
     * 查询size default 10
     */
    private int size = 10;
}
