package cn.wkiki.kql.queryUnit;


import cn.wkiki.kql.util.GsonUtil;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

public class PhraseMatch extends FieldStringSearchUnit{

    public PhraseMatch(String fieldName,String searchStr) {
        setFilteredFieldName(fieldName);
        setSearchStr(searchStr);
    }

    @Override
    public String toESQueryJsonEntity() {
        PhraseMatchTemplate template = new PhraseMatchTemplate(getFilteredFieldName(),getSearchStr());
        return GsonUtil.getInstance().toJson(template);
    }

    @Override
    public String prettyToESQueryJsonEntity() {
        PhraseMatchTemplate template = new PhraseMatchTemplate(getFilteredFieldName(),getSearchStr());
        return GsonUtil.getInstanceWithPretty().toJson(template);
    }

    /**
     * ES全文查询单元的模板类
     */
    @Getter
    @Setter
    static class PhraseMatchTemplate{

        private Map<String, PhraseMatchFieldQueryConfig> match_phrase;

        public PhraseMatchTemplate(String fieldName,String searchValue){
            match_phrase = new HashMap<>();
            match_phrase.put(fieldName,new PhraseMatchFieldQueryConfig(searchValue));
        }
    }

    @Getter
    @Setter
    static class PhraseMatchFieldQueryConfig{
        public PhraseMatchFieldQueryConfig(){}

        public PhraseMatchFieldQueryConfig(String queryString){
            this.setQuery(queryString);
        }

        private String query;
    }
}
