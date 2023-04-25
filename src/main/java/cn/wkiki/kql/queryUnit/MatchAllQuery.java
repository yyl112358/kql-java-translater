package cn.wkiki.kql.queryUnit;


import cn.wkiki.kql.util.GsonUtil;

import java.util.HashMap;
import java.util.Map;

public class MatchAllQuery implements QueryUnit{

    @Override
    public String toESQueryJsonEntity() {
        Map<String,Object> map = new HashMap<>();
        map.put("match_all",new Object());
        return GsonUtil.getInstance().toJson(map);
    }

    @Override
    public String prettyToESQueryJsonEntity() {
        Map<String,Object> map = new HashMap<>();
        map.put("match_all",new Object());
        return GsonUtil.getInstanceWithPretty().toJson(map);
    }
}
