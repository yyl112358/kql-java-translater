package cn.wkiki.kql.optimizer;

import cn.wkiki.kql.queryUnit.QueryUnit;
import lombok.Getter;

/**
 * 优化器优化的区别
 */
@Getter
public class OptimizeDiff {

    public OptimizeDiff(QueryUnit rawQueryUnit, QueryUnit optimizedQueryUnit) {
        this.rawQueryUnit = rawQueryUnit;
        this.optimizedQueryUnit = optimizedQueryUnit;
    }

    /**
     * 优化前查询单元
     */
    private QueryUnit rawQueryUnit;

    /**
     * 优化后的查询单元
     */
    private QueryUnit optimizedQueryUnit;

}
