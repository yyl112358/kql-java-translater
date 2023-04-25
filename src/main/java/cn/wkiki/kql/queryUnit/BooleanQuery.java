package cn.wkiki.kql.queryUnit;

import cn.wkiki.kql.util.GsonUtil;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * boolean查询
 * @author yanyulong
 */
@Getter
public class BooleanQuery implements QueryUnit {


    List<QueryUnit> must;

    List<QueryUnit> should;

    List<QueryUnit> filter;

    List<QueryUnit> must_not;

    public void addMust(QueryUnit queryUnit){
        must = CollectionUtils.isNotEmpty(must)?must:new ArrayList<>();
        must.add(queryUnit);
    }

    public void addShould(QueryUnit queryUnit){
        should = CollectionUtils.isNotEmpty(should)?should:new ArrayList<>();
        should.add(queryUnit);
    }

    public void addFilter(QueryUnit queryUnit){
        filter = CollectionUtils.isNotEmpty(filter)?filter:new ArrayList<>();
        filter.add(queryUnit);
    }

    public void addMustNot(QueryUnit queryUnit){
        must_not = CollectionUtils.isNotEmpty(must_not)?must_not:new ArrayList<>();
        must_not.add(queryUnit);
    }


    @Override
    public String toESQueryJsonEntity() {
        return GsonUtil.getInstance().toJson(collectMap());
    }

    @Override
    public String prettyToESQueryJsonEntity() {
        return GsonUtil.getInstanceWithPretty().toJson(collectMap());
    }

    private Map collectMap(){
        Map<String,List<Map>> map = new HashMap<>();
        if(CollectionUtils.isNotEmpty(must)){
            map.put("must",must.stream().filter(t->!t.getClass().equals(EmptyUnit.class)).map(t-> GsonUtil.getInstance().fromJson(t.toESQueryJsonEntity(),Map.class)).collect(Collectors.toList()));
        }
        if(CollectionUtils.isNotEmpty(should)){
            map.put("should",should.stream().filter(t->!t.getClass().equals(EmptyUnit.class)).map(t-> GsonUtil.getInstance().fromJson(t.toESQueryJsonEntity(),Map.class)).collect(Collectors.toList()));
        }
        if(CollectionUtils.isNotEmpty(filter)){
            map.put("filter",filter.stream().filter(t->!t.getClass().equals(EmptyUnit.class)).map(t-> GsonUtil.getInstance().fromJson(t.toESQueryJsonEntity(),Map.class)).collect(Collectors.toList()));
        }
        if(CollectionUtils.isNotEmpty(must_not)){
            map.put("must_not",must_not.stream().filter(t->!t.getClass().equals(EmptyUnit.class)).map(t-> GsonUtil.getInstance().fromJson(t.toESQueryJsonEntity(),Map.class)).collect(Collectors.toList()));
        }
        Map<String,Map<String,List<Map>>> result = new HashMap<>();
        result.put("bool",map);
        return result;
    }
}
