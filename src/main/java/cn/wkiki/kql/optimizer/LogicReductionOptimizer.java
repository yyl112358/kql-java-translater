package cn.wkiki.kql.optimizer;


import cn.wkiki.kql.queryUnit.BooleanQuery;
import cn.wkiki.kql.queryUnit.QueryUnit;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 逻辑归约优化器。
 * 此优化器对bool 查询中(filter、must、must_not)中相同类型逻辑查询嵌套的优化
 * 例如以下 dsl
 * a:a and b:b and c:c parser
 * 直接生成的查询单元情况为
 * bool:
 *   must:
 *      a:a
 *      bool:
 *         must:
 *            b:b
 *            bool:
 *              c:c
 * 优化后的查询单元为
 * bool:
 *    must:
 *      a:a
 *      b:b
 *      c:c
 * 优化掉无用的逻辑嵌套关系
 */
public class LogicReductionOptimizer implements Optimize{
    @Override
    public QueryUnit optimizeQuery(QueryUnit queryTreeRoot) {
        if (queryTreeRoot.getClass().equals(BooleanQuery.class)){
            processOptimize((BooleanQuery) queryTreeRoot,null);
        }
        return queryTreeRoot;
    }

    /**
     * 处理优化 若当前层的某个逻辑存在且上层
     * @param booleanQuery
     * @return
     */
    private void processOptimize(BooleanQuery booleanQuery, BooleanQuery parentNode){
        processMust(booleanQuery,parentNode);
        processShould(booleanQuery,parentNode);
        processMustNot(booleanQuery,parentNode);
        processFilter(booleanQuery,parentNode);
    }

    private void processMust(BooleanQuery booleanQuery,BooleanQuery parentNode){
        if(CollectionUtils.isNotEmpty(booleanQuery.getMust())){
            // 先加工复杂子节点，可能在收集的过程 复杂子节点会被归约为一批简单查询节点
            List<QueryUnit> reductionQueryUnits = new ArrayList<>();
            for (int i = 0; i < booleanQuery.getMust().size(); i++) {
                QueryUnit queryUnit = booleanQuery.getMust().get(i);
                if(queryUnit.getClass().equals(BooleanQuery.class)){
                    processOptimize((BooleanQuery) queryUnit, booleanQuery);
                    if(ifBlankLogicQueryUnit((BooleanQuery)queryUnit)){
                        reductionQueryUnits.add(queryUnit);
                    }
                }
            }
            // 加工本层简单查询，此时本层简单查询已包含自己节点归约上来的简单查询
            for (int i = 0; i < booleanQuery.getMust().size(); i++) {
                QueryUnit queryUnit = booleanQuery.getMust().get(i);
                if(!queryUnit.getClass().equals(BooleanQuery.class)){
                    if(parentNode != null&&CollectionUtils.isNotEmpty(parentNode.getMust())) {
                        parentNode.addMust(queryUnit);  //上涌此条件
                        reductionQueryUnits.add(queryUnit); //记录需要移除的条件
                    }
                }
            }
            if(!reductionQueryUnits.isEmpty()){
                booleanQuery.getMust().removeAll(reductionQueryUnits);
            }
            if(CollectionUtils.isNotEmpty(booleanQuery.getMust()) &&booleanQuery.getMust().size() == 1){
                if(parentNode!=null && CollectionUtils.isNotEmpty(parentNode.getMust())){
                    // 当前条件仅剩一个且，父级有相同类型的条件存在，将本层剩余的条件上涌到父级相同逻辑关系的集合中
                    QueryUnit latestUnit = booleanQuery.getMust().get(0);
                    parentNode.getMust().add(latestUnit);
                    booleanQuery.getMust().remove(latestUnit);
                }
            }
        }
    }

    private void processShould(BooleanQuery booleanQuery,BooleanQuery parentNode){
        if(CollectionUtils.isNotEmpty(booleanQuery.getShould())){
            // 先加工复杂子节点，可能在收集的过程 复杂子节点会被归约为一批简单查询节点
            List<QueryUnit> reductionQueryUnits = new ArrayList<>();
            for (int i = 0; i < booleanQuery.getShould().size(); i++) {
                QueryUnit queryUnit = booleanQuery.getShould().get(i);
                if(queryUnit.getClass().equals(BooleanQuery.class)){
                    processOptimize((BooleanQuery) queryUnit, booleanQuery);
                    if(ifBlankLogicQueryUnit((BooleanQuery)queryUnit)){
                        reductionQueryUnits.add(queryUnit);
                    }
                }
            }
            // 加工本层简单查询，此时本层简单查询已包含自己节点归约上来的简单查询
            for (int i = 0; i < booleanQuery.getShould().size(); i++) {
                QueryUnit queryUnit = booleanQuery.getShould().get(i);
                if(!queryUnit.getClass().equals(BooleanQuery.class)){
                    if(parentNode != null&&CollectionUtils.isNotEmpty(parentNode.getShould())) {
                        parentNode.addShould(queryUnit);  //上涌此条件
                        reductionQueryUnits.add(queryUnit); //记录需要移除的条件
                    }
                }
            }
            if(!reductionQueryUnits.isEmpty()){
                booleanQuery.getShould().removeAll(reductionQueryUnits);
            }
            if(CollectionUtils.isNotEmpty(booleanQuery.getShould()) &&booleanQuery.getShould().size() == 1){
                if(parentNode!=null && CollectionUtils.isNotEmpty(parentNode.getShould())){
                    // 当前条件仅剩一个且，父级有相同类型的条件存在，将本层剩余的条件上涌到父级相同逻辑关系的集合中
                    QueryUnit latestUnit = booleanQuery.getShould().get(0);
                    parentNode.getShould().add(latestUnit);
                    booleanQuery.getShould().remove(latestUnit);
                }
            }
        }
    }
    private void processMustNot(BooleanQuery booleanQuery,BooleanQuery parentNode){
        if(CollectionUtils.isNotEmpty(booleanQuery.getMust_not())){
            // 先加工复杂子节点，可能在收集的过程 复杂子节点会被归约为一批简单查询节点
            List<QueryUnit> reductionQueryUnits = new ArrayList<>();
            for (int i = 0; i < booleanQuery.getMust_not().size(); i++) {
                QueryUnit queryUnit = booleanQuery.getMust_not().get(i);
                if(queryUnit.getClass().equals(BooleanQuery.class)){
                    processOptimize((BooleanQuery) queryUnit, booleanQuery);
                    if(ifBlankLogicQueryUnit((BooleanQuery)queryUnit)){
                        reductionQueryUnits.add(queryUnit);
                    }
                }
            }
            // 加工本层简单查询，此时本层简单查询已包含自己节点归约上来的简单查询
            for (int i = 0; i < booleanQuery.getMust_not().size(); i++) {
                QueryUnit queryUnit = booleanQuery.getMust_not().get(i);
                if(!queryUnit.getClass().equals(BooleanQuery.class)){
                    if(parentNode != null&&CollectionUtils.isNotEmpty(parentNode.getMust_not())) {
                        parentNode.addMustNot(queryUnit);
                        reductionQueryUnits.add(queryUnit);
                    }
                }
            }
            if(!reductionQueryUnits.isEmpty()){
                booleanQuery.getMust_not().removeAll(reductionQueryUnits);
            }
            if(CollectionUtils.isNotEmpty(booleanQuery.getMust_not()) &&booleanQuery.getMust_not().size() == 1){
                if(parentNode!=null && CollectionUtils.isNotEmpty(parentNode.getMust_not())){
                    // 当前条件仅剩一个且，父级有相同类型的条件存在，将本层剩余的条件上涌到父级相同逻辑关系的集合中
                    QueryUnit latestUnit = booleanQuery.getMust_not().get(0);
                    parentNode.getMust_not().add(latestUnit);
                    booleanQuery.getMust_not().remove(latestUnit);
                }
            }
        }
    }

    private void processFilter(BooleanQuery booleanQuery,BooleanQuery parentNode){
        if(CollectionUtils.isNotEmpty(booleanQuery.getFilter())){
            // 先加工复杂子节点，可能在收集的过程 复杂子节点会被归约为一批简单查询节点
            List<QueryUnit> reductionQueryUnits = new ArrayList<>();
            for (int i = 0; i < booleanQuery.getFilter().size(); i++) {
                QueryUnit queryUnit = booleanQuery.getFilter().get(i);
                if(queryUnit.getClass().equals(BooleanQuery.class)){
                    processOptimize((BooleanQuery) queryUnit, booleanQuery);
                    if(ifBlankLogicQueryUnit((BooleanQuery)queryUnit)){
                        reductionQueryUnits.add(queryUnit);
                    }
                }
            }
            // 加工本层简单查询，此时本层简单查询已包含自己节点归约上来的简单查询
            for (int i = 0; i < booleanQuery.getFilter().size(); i++) {
                QueryUnit queryUnit = booleanQuery.getFilter().get(i);
                if(!queryUnit.getClass().equals(BooleanQuery.class)){
                    if(parentNode != null&&CollectionUtils.isNotEmpty(parentNode.getFilter())) {
                        parentNode.addFilter(queryUnit);
                        reductionQueryUnits.add(queryUnit);
                    }
                }
            }
            if(!reductionQueryUnits.isEmpty()){
                booleanQuery.getFilter().removeAll(reductionQueryUnits);
            }
            if(CollectionUtils.isNotEmpty(booleanQuery.getFilter()) &&booleanQuery.getFilter().size() == 1){
                if(parentNode!=null && CollectionUtils.isNotEmpty(parentNode.getFilter())){
                    // 当前条件仅剩一个且，父级有相同类型的条件存在，将本层剩余的条件上涌到父级相同逻辑关系的集合中
                    QueryUnit latestUnit = booleanQuery.getFilter().get(0);
                    parentNode.getFilter().add(latestUnit);
                    booleanQuery.getFilter().remove(latestUnit);
                }
            }
        }
    }


    /**
     * 判断一个BooleanQuery是否是一个空白的 查询单元，
     * 空白单元意味着，这个查询单元的must filter must_not should 集合均为空
     * @param booleanQuery
     * @return
     */
    private boolean ifBlankLogicQueryUnit(BooleanQuery booleanQuery){
        if(booleanQuery == null){
            return true;
        }
        return CollectionUtils.isEmpty(booleanQuery.getMust())&&
                CollectionUtils.isEmpty(booleanQuery.getFilter())&&
                CollectionUtils.isEmpty(booleanQuery.getMust_not())&&
                CollectionUtils.isEmpty(booleanQuery.getShould());
    }
}
