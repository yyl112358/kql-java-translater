package cn.wkiki.kql;


import cn.wkiki.kql.optimizer.LogicReductionOptimizer;
import cn.wkiki.kql.optimizer.MergeRelationOptimizer;
import cn.wkiki.kql.optimizer.Optimize;
import cn.wkiki.kql.queryUnit.QueryUnit;

/**
 * 生成树优化器
 */
public class Optimizer {

    /**
     * 使用合并关系运算优化器开关
     */
    boolean mergeRelationSwitch;

    boolean logicReductionSwitch;

    public Optimizer(){
        this.mergeRelationSwitch=true;
        this.logicReductionSwitch = true;
    }

    public Optimizer(boolean mergeRelationSwitch,boolean logicReductionSwitch){
        this.mergeRelationSwitch = mergeRelationSwitch;
        this.logicReductionSwitch = logicReductionSwitch;
    }

    public QueryUnit optimize(QueryUnit rawQueryUnit){
        QueryUnit result = rawQueryUnit;
        if(logicReductionSwitch){
            Optimize optimizer = new LogicReductionOptimizer();
            result = optimizer.optimizeQuery(result);
        }
        if(mergeRelationSwitch){
            Optimize optimizer =new MergeRelationOptimizer();
            result = optimizer.optimizeQuery(result);
        }
        return result;
    }
}
