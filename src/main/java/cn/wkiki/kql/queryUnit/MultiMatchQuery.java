package cn.wkiki.kql.queryUnit;

import cn.wkiki.kql.util.GsonUtil;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;

/**
 * multiMatch 泛查询
 */
public class MultiMatchQuery implements QueryUnit {

    public MultiMatchQuery(String queryStr,boolean isPhraseMatch){
        this.queryStr = queryStr;
        this.ifPhraseMatch = isPhraseMatch;
    }

    public MultiMatchQuery(String queryStr,boolean isPhraseMatch,List<String> limitFields){
        this(queryStr,isPhraseMatch);
        this.limitFields = limitFields;
    }

    /**
     * 泛查询字符串
     */
    String queryStr;

    /**
     * 查询约束的字段列表
     */
    List<String> limitFields;

    /**
     * 是否是短语匹配
     */
    boolean ifPhraseMatch;

    @Override
    public String toESQueryJsonEntity() {
        MultiMatchTemplate template = null;
        if(CollectionUtils.isEmpty(limitFields)){
            template = new MultiMatchTemplate(this.queryStr,this.ifPhraseMatch);
        }else{
            template = new MultiMatchTemplate(queryStr,limitFields,this.ifPhraseMatch);
        }
        return GsonUtil.getInstance().toJson(template);
    }

    @Override
    public String prettyToESQueryJsonEntity() {
        MultiMatchTemplate template = null;
        if(CollectionUtils.isEmpty(limitFields)){
            template = new MultiMatchTemplate(this.queryStr,this.ifPhraseMatch);
        }else{
            template = new MultiMatchTemplate(queryStr,limitFields,this.ifPhraseMatch);
        }
        return GsonUtil.getInstanceWithPretty().toJson(template);
    }

    /**
     * ES 多字段泛查询模板类
     */
    static class MultiMatchTemplate{

        public MultiMatchTemplate(String queryStr,boolean isPhrase){
            multi_match = new MultiMatchConfig(queryStr,null,isPhrase);
        }

        public MultiMatchTemplate(String queryStr,List<String> fields,boolean isPhrase){
            multi_match = new MultiMatchConfig(queryStr,fields,isPhrase);
        }

        MultiMatchConfig multi_match;
    }

    /**
     * 泛查询配置类
     */
    static class MultiMatchConfig{

        MultiMatchConfig(String queryStr,List<String> fields,boolean phrase){
            this.query = queryStr;
            this.fields =fields;
            if(phrase){
                this.type="phrase";
            }
        }

        String query;

        List<String> fields;

        String type;
    }
}
