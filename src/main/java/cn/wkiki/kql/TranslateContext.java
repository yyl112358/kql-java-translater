package cn.wkiki.kql;

import cn.wkiki.kql.es.IndexField;
import lombok.Getter;

import java.util.List;

/**
 * 翻译时使用的上下文环境
 */
@Getter
public class TranslateContext {

    public TranslateContext(){}

    public TranslateContext(String indexName, List<IndexField> fields) {
        this.indexName = indexName;
        this.fields = fields;
    }

    /**
     * 执行所在的索引名
     */
    String indexName;

    /**
     * 索引的属性信息
     */
    List<IndexField> fields;
}
