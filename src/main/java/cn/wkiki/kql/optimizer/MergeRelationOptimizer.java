package cn.wkiki.kql.optimizer;


import cn.wkiki.kql.queryUnit.BooleanQuery;
import cn.wkiki.kql.queryUnit.DateFieldRelationSearch;
import cn.wkiki.kql.queryUnit.FieldRelationSearch;
import cn.wkiki.kql.queryUnit.QueryUnit;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 合并关系运算优化器。
 * 此优化器对bool 查询中(filter、must、must_not)中对相同属性的关系运算尝试进行合并优化
 */
public class MergeRelationOptimizer implements Optimize {

    @Override
    public QueryUnit optimizeQuery(QueryUnit queryTreeRoot) {
        if(queryTreeRoot.getClass().equals(BooleanQuery.class)){
            return optimize((BooleanQuery) queryTreeRoot);
        }else{
            return queryTreeRoot;
        }
    }

    /**
     * 优化过程，递归向下扫描，同级出现属性名相同的fieldRelationSearch 类型的节点，则判断合并
     * @param booleanQuery
     * @return
     */
    private QueryUnit optimize(BooleanQuery booleanQuery){
        BooleanQuery result = new BooleanQuery();
        //filter
        if(booleanQuery.getFilter()!=null){
            List<QueryUnit> processResult = processAndLogic(booleanQuery.getFilter());
            for (QueryUnit queryUnit : processResult) {
                result.addFilter(queryUnit);
            }
        }
        // must
        if(booleanQuery.getMust()!=null){
            List<QueryUnit> processResult = processAndLogic(booleanQuery.getMust());
            for (QueryUnit queryUnit : processResult) {
                result.addMust(queryUnit);
            }
        }
        // must_not
        if(booleanQuery.getMust_not()!=null){
            List<QueryUnit> processResult = processAndLogic(booleanQuery.getMust_not());
            for (QueryUnit queryUnit : processResult) {
                result.addMustNot(queryUnit);
            }
        }
        //should
        if(booleanQuery.getShould()!=null){
            for (QueryUnit shouldUnit : booleanQuery.getShould()) {
                if(shouldUnit.getClass().equals(BooleanQuery.class)){
                    QueryUnit optimizedSubTree = optimize((BooleanQuery) shouldUnit);
                    result.addShould(optimizedSubTree);
                }else{
                    result.addShould(shouldUnit);
                }
            }
        }
        return result;
    }

    /**
     * 处理 boolean 中的一个and逻辑条件集合（must filter must_not）
     * 并返回一个与新的处理过的集合, 返回的集合传入的集合在内存中是两个集合
     * @param andLogicQueue
     * @return
     */
    private List<QueryUnit> processAndLogic(List<QueryUnit> andLogicQueue){
        List<QueryUnit> result = new LinkedList<>();
        List<FieldRelationSearch> filterRelations = andLogicQueue.stream()
                .filter(t->t.getClass().equals(FieldRelationSearch.class)||t.getClass().equals(DateFieldRelationSearch.class))
                .map(t->(FieldRelationSearch)t).collect(Collectors.toList());
        List<FieldRelationSearch> optimizeResult = optimizeQueryUnitList(filterRelations);
        if((!optimizeResult.equals(filterRelations)) && optimizeResult.size() != filterRelations.size()){
            for (FieldRelationSearch fieldRelationSearch : optimizeResult) {
                result.add(fieldRelationSearch);
            }
            for (QueryUnit queryUnit : andLogicQueue) {
                if(queryUnit.getClass().equals(FieldRelationSearch.class)||queryUnit.getClass().equals(DateFieldRelationSearch.class)){
                    continue;
                }else if(queryUnit.getClass().equals(BooleanQuery.class)){
                    result.add(optimize((BooleanQuery)queryUnit));
                }else{
                    result.add(queryUnit);
                }
            }
            return result;
        }else{
            for (QueryUnit queryUnit : andLogicQueue) {
                if(queryUnit.getClass().equals(BooleanQuery.class)){
                    result.add(optimize((BooleanQuery)queryUnit));
                }else{
                    result.add(queryUnit);
                }
            }
        }
        return result;
    }

    /**
     * 优化一个只有条件查询的结合，合并其中对相同属性的的条件关系
     * 例如
     * <pre>
     * [
     *     {
     *         prop1 > 9
     *     },
     *     {
     *         prop1 <25
     *     },
     *     {
     *         prop2 > 35
     *     }
     * ]
     * 会被优化为
     * [
     *     {
     *         prop1 > 9,
     *         prop1 <25
     *     },
     *     {
     *         prop2 > 35
     *     }
     * ]
     * </pre>
     * 若无优化的部分，则将列表拷贝一份后原样将拷贝返回
     * @param fieldRelationSearches
     * @return
     */
    private List<FieldRelationSearch> optimizeQueryUnitList(List<FieldRelationSearch> fieldRelationSearches){
        List<FieldRelationSearch> result = new LinkedList<>();
        for (FieldRelationSearch fieldRelationSearch : fieldRelationSearches) {
            //不在原集合上直接操作,拷贝一份原始集合
            if(fieldRelationSearch.getClass().equals(DateFieldRelationSearch.class)){
                result.add(new DateFieldRelationSearch(fieldRelationSearch.getFilteredFieldName(),fieldRelationSearch.getRelationType(),((DateFieldRelationSearch) fieldRelationSearch).getRelationCalcValue()));
            }else{
                result.add(new FieldRelationSearch<>(fieldRelationSearch.getFilteredFieldName(),fieldRelationSearch.getRelationType(),fieldRelationSearch.getRelationCalcValue()));
            }
        }
        boolean optimizeHappen = false;
        for (FieldRelationSearch fieldRelationSearch : result) {
            if(fieldRelationSearch.getSecondRelationType() != null ||fieldRelationSearch.getSecondRelationCalcValue() !=null){
                // 合并过一次的条件，直接跳过
                continue;
            }
            //找到可以合并的关系运算节点,条件([1]属性名字相同。[2]第一个条件不为空且无第二个关系条件。[3]同时第一个关系条件与当前节点的条件不一样)
            Optional<FieldRelationSearch> canMergeNodeOpt = result.stream().filter(t->t.getFilteredFieldName().equals(fieldRelationSearch.getFilteredFieldName()))
                        .filter(t->t.getRelationType()!=null && t.getSecondRelationType() == null)
                        .filter(t->!t.getRelationType().equals(fieldRelationSearch.getRelationType()))
                        .filter(t->t.getSecondRelationCalcValue() == null)
                        .findFirst();
            if(canMergeNodeOpt.isPresent()){
                canMergeNodeOpt.get().setSecondRelationType(fieldRelationSearch.getRelationType());
                canMergeNodeOpt.get().setSecondRelationCalcValue(fieldRelationSearch.getRelationCalcValue());
                fieldRelationSearch.setRelationType(null);
                fieldRelationSearch.setRelationCalcValue(null);
                optimizeHappen = true;
            }
        }
        if(optimizeHappen){
            return result.stream().filter(t->t.getRelationType()!=null).collect(Collectors.toList());
        }else{
            return result;
        }
    }
}
