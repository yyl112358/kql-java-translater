package cn.wkiki.kql.queryUnit;

import cn.wkiki.kql.Token;
import cn.wkiki.kql.util.GsonUtil;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.SignStyle;
import java.util.HashMap;
import java.util.Map;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.time.temporal.ChronoField.*;

/**
 * 时间类型字段的关系查询单元，可以指定format与zone
 */
public class DateFieldRelationSearch extends FieldRelationSearch<LocalDateTime>{

    public static final DateTimeFormatter defaultFormatter = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .append(ISO_LOCAL_DATE)
            .appendLiteral('T')
            .appendValue(HOUR_OF_DAY, 2)
            .appendLiteral(':')
            .appendValue(MINUTE_OF_HOUR, 2)
            .appendLiteral(':')
            .appendValue(SECOND_OF_MINUTE, 2)
            .appendLiteral('.')
            .appendValue(MILLI_OF_SECOND, 3,3, SignStyle.NEVER)
            .appendLiteral("+0800")
            .toFormatter();

    //！！！！！！！！！！！需要与上方同步！！！！！！！！！！！
    public static String formatStr = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

    public DateFieldRelationSearch(String fieldName, Token.Type relationType, LocalDateTime relationCalcValue) {
        super(fieldName, relationType, relationCalcValue);
    }

    @Override
    public String toESQueryJsonEntity() {
        RelationSearchTemplate template =
                new RelationSearchTemplate(getFilteredFieldName(),getRelationType(),getSecondRelationType(),getRelationCalcValue(),getSecondRelationCalcValue(),formatStr);
        return GsonUtil.getInstance().toJson(template);
    }

    @Override
    public String prettyToESQueryJsonEntity() {
        RelationSearchTemplate template =
                new RelationSearchTemplate(getFilteredFieldName(),getRelationType(),getSecondRelationType(),getRelationCalcValue(),getSecondRelationCalcValue(),formatStr);
        return GsonUtil.getInstanceWithPretty().toJson(template);
    }

    /**
     * ES关系查询单元的模板类
     */
    @Getter
    @Setter
    static class RelationSearchTemplate{

        RelationSearchTemplate(String fieldName,Token.Type relationType,Token.Type secondType,LocalDateTime relationCalcValue,LocalDateTime secondCalcValue,String formatStr){
            range = new HashMap<>();
            RelationSearchConfig relationSearchConfig = new RelationSearchConfig();
            switch (relationType){
                case lt:
                    relationSearchConfig.setLt(relationCalcValue.format(defaultFormatter));
                    break;
                case lte:
                    relationSearchConfig.setLte(relationCalcValue.format(defaultFormatter));
                    break;
                case gt:
                    relationSearchConfig.setGt(relationCalcValue.format(defaultFormatter));
                    break;
                case gte:
                    relationSearchConfig.setGte(relationCalcValue.format(defaultFormatter));
                    break;
            }
            if(secondType!=null && secondCalcValue!=null){
                switch (secondType){
                    case lt:
                        relationSearchConfig.setLt(secondCalcValue.format(defaultFormatter));
                        break;
                    case lte:
                        relationSearchConfig.setLte(secondCalcValue.format(defaultFormatter));
                        break;
                    case gt:
                        relationSearchConfig.setGt(secondCalcValue.format(defaultFormatter));
                        break;
                    case gte:
                        relationSearchConfig.setGte(secondCalcValue.format(defaultFormatter));
                        break;
                }
            }
            relationSearchConfig.setFormat(formatStr);
            range.put(fieldName,relationSearchConfig);
        }

        private Map<String, RelationSearchConfig> range;

    }

    @Getter
    @Setter
    static class RelationSearchConfig extends FieldRelationSearch.RelationSearchConfig<String>{

        String format;

    }
}
