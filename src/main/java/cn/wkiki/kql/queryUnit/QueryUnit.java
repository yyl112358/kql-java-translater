package cn.wkiki.kql.queryUnit;

/**
 *
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
