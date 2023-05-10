package cn.wkiki.kql.queryUnit;

import cn.wkiki.kql.util.GsonUtil;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;


public class NestedFieldQuery extends FieldSearchUnit{

    QueryUnit nestedFilterQueryUnit;

    public NestedFieldQuery(String outerFieldName, QueryUnit nestedFilterQueryUnit){
        setFilteredFieldName(outerFieldName);
        this.nestedFilterQueryUnit = nestedFilterQueryUnit;
    }
    @Override
    public String toESQueryJsonEntity() {
        NestedFieldQueryTemplate nestedFieldQueryTemplate = new NestedFieldQueryTemplate(getFilteredFieldName(),nestedFilterQueryUnit);
        return GsonUtil.getInstance().toJson(nestedFieldQueryTemplate);
    }

    @Override
    public String prettyToESQueryJsonEntity() {
        NestedFieldQueryTemplate nestedFieldQueryTemplate = new NestedFieldQueryTemplate(getFilteredFieldName(),nestedFilterQueryUnit);
        return GsonUtil.getInstanceWithPretty().toJson(nestedFieldQueryTemplate);
    }

    @Getter
    @Setter
    static class  NestedFieldQueryTemplate{

        NestedFieldQueryTemplate(String path,QueryUnit nestedQuery){
            this.nested = new NestedFieldQueryConfig();
            nested.path = path;
            nested.query = nestedQuery;
        }

        NestedFieldQueryConfig nested;
    }

    static class NestedFieldQueryConfig{

        /**
         * outer field path config
         */
        private String path;

        /**
         * nested query json
         */
        @JsonAdapter(NestedQueryAdapter.class)
        private QueryUnit query;
    }

    static class NestedQueryAdapter extends TypeAdapter<QueryUnit>{
        @Override
        public void write(JsonWriter out, QueryUnit value) throws IOException {
            String json = value.prettyToESQueryJsonEntity();
            out.jsonValue(json);
        }

        @Override
        public QueryUnit read(JsonReader in) throws IOException {
            throw new UnsupportedOperationException();
        }
    }
}
