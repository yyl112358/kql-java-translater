package cn.wkiki.kql.queryUnit;

/**
 * es查询的一个查询单元<br/>
 * 对应查询RESTApi中query body 部分<br/>
 * dsl语法树种的多数语句都会被翻译到这个query 单元中<br/>
 */
public interface QueryUnit {

    /**
     * 获取ES可用的查询Json实体
     * @return
     */
    String toESQueryJsonEntity();

    /**
     * 获取美化后的ES可用的查询Json实体
     * @return
     */
    String prettyToESQueryJsonEntity();
}
