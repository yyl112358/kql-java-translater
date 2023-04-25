package cn.wkiki.kql.queryUnit;

import cn.wkiki.kql.Token;
import cn.wkiki.kql.exception.DSLSemanticsException;
import cn.wkiki.kql.util.GsonUtil;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/**
 * 字段关系查询基础类
 */
public class FieldRelationSearch<T> extends FieldSearchUnit{

    public FieldRelationSearch(String fieldName, Token.Type relationType, T relationCalcValue) {
        setFilteredFieldName(fieldName);
        switch (relationType){
            case lt:
            case lte:
            case gt:
            case gte:
                this.relationType = relationType;
                break;
            default:
                throw new DSLSemanticsException("字段关系匹配只能是[<,<=,>,>=]");
        }
        this.relationCalcValue = relationCalcValue;
    }

    /**
     * 属性关系查询计算类型
     */
    @Getter
    @Setter
    Token.Type relationType;

    /**
     * 第二个属性关系查询计算类型
     */
    @Getter
    Token.Type secondRelationType;

    public void setSecondRelationType(Token.Type secondRelationType){
        if(secondRelationType.equals(relationType)){
            throw new DSLSemanticsException("关系运算查询两个关系运算符不可相同！！！！");
        }
        this.secondRelationType = secondRelationType;
    }

    /**
     * 进行关系查询的值
     */
    @Getter
    @Setter
    T relationCalcValue;

    /**
     * 第二个关系查询的值
     */
    @Getter
    @Setter
    T secondRelationCalcValue;

    @Override
    public String toESQueryJsonEntity() {
        RelationSearchTemplate<T> template =
                new RelationSearchTemplate<>(getFilteredFieldName(), getRelationType(), getSecondRelationType(), getRelationCalcValue(), secondRelationCalcValue);
        return GsonUtil.getInstance().toJson(template);
    }

    @Override
    public String prettyToESQueryJsonEntity() {
        RelationSearchTemplate<T> template =
                new RelationSearchTemplate<>(getFilteredFieldName(), getRelationType(), getSecondRelationType(), getRelationCalcValue(), secondRelationCalcValue);
        return GsonUtil.getInstanceWithPretty().toJson(template);
    }

    /**
     * ES关系查询单元的模板类
     */
    @Getter
    @Setter
    static class RelationSearchTemplate<T>{

        RelationSearchTemplate(String fieldName,Token.Type relationType,Token.Type secondRelationType,T relationCalcValue,T secondCalcValue){
            range = new HashMap<>();
            RelationSearchConfig relationSearchConfig = new RelationSearchConfig();
            switch (relationType){
                case lt:
                    relationSearchConfig.setLt(relationCalcValue);
                    break;
                case lte:
                    relationSearchConfig.setLte(relationCalcValue);
                    break;
                case gt:
                    relationSearchConfig.setGt(relationCalcValue);
                    break;
                case gte:
                    relationSearchConfig.setGte(relationCalcValue);
                    break;
            }
            if(secondRelationType!=null && secondCalcValue!=null){
                switch (secondRelationType){
                    case lt:
                        relationSearchConfig.setLt(secondCalcValue);
                        break;
                    case lte:
                        relationSearchConfig.setLte(secondCalcValue);
                        break;
                    case gt:
                        relationSearchConfig.setGt(secondCalcValue);
                        break;
                    case gte:
                        relationSearchConfig.setGte(secondCalcValue);
                        break;
                }
            }
            range.put(fieldName,relationSearchConfig);
        }

        private Map<String, RelationSearchConfig> range;


    }

    @Getter
    @Setter
    static class RelationSearchConfig{

        Object gt;

        Object gte;

        Object lt;

        Object lte;
    }
}
