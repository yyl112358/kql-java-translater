package cn.wkiki.kql.optimizer;


import cn.wkiki.kql.queryUnit.QueryUnit;

public interface Optimize {

    /**
     * 优化目标生成树,优化过程为深度拷贝对象。返回的QueryUnit为全新的对象
     * @param queryTreeRoot 翻译生成树的树根
     * @return 优化后新的生成树
     */
    QueryUnit optimizeQuery(QueryUnit queryTreeRoot);

}
