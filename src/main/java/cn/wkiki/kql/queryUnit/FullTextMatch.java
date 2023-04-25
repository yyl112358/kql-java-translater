package cn.wkiki.kql.queryUnit;

import cn.wkiki.kql.util.GsonUtil;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/**
 * ES 查询得全文索引单元
 * @author yanyulong
 */
public class FullTextMatch extends FieldStringSearchUnit{

    public FullTextMatch(String fieldName,String searchValue){
        setFilteredFieldName(fieldName);
        setSearchStr(searchValue);
    }

    @Override
    public String toESQueryJsonEntity() {
        FullTextMatchTemplate template = new FullTextMatchTemplate(this.getFilteredFieldName(),this.getSearchStr());
        return GsonUtil.getInstance().toJson(template);
    }

    @Override
    public String prettyToESQueryJsonEntity() {
        FullTextMatchTemplate template = new FullTextMatchTemplate(this.getFilteredFieldName(),this.getSearchStr());
        return GsonUtil.getInstanceWithPretty().toJson(template);
    }

    /**
     * ES全文查询单元的模板类
     */
    @Getter
    @Setter
    static class FullTextMatchTemplate{

        private Map<String,FullTextMatchFieldQueryConfig> match;

        public FullTextMatchTemplate(String fieldName,String searchValue){
            match = new HashMap<>();
            match.put(fieldName,new FullTextMatchFieldQueryConfig(searchValue));
        }
    }

    @Getter
    @Setter
    static class FullTextMatchFieldQueryConfig{
        public FullTextMatchFieldQueryConfig(){}

        public FullTextMatchFieldQueryConfig(String queryString){
            this.setQuery(queryString);
        }

        private String query;
    }
}
