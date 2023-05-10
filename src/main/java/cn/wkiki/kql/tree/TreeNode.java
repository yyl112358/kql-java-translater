package cn.wkiki.kql.tree;


import cn.wkiki.kql.TranslateContext;
import cn.wkiki.kql.es.IndexField;
import cn.wkiki.kql.exception.DSLSemanticsException;
import cn.wkiki.kql.model.DetailQueryParamBody;
import cn.wkiki.kql.queryUnit.QueryUnit;
import cn.wkiki.kql.tree.TreeNodeImpl.FieldSearchTreeNode.FieldNameTreeNode;
import cn.wkiki.kql.util.GsonUtil;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * DSL 语法树
 */
public abstract class TreeNode {

    static final String FuzzyFieldCh ="*";

    /**
     * 获取当前节点可打印的字符串
     * @return
     */
    public abstract String toSourceStr();

    /**
     * 获取当前节点预翻译的字符串
     * @return
     */
    public String toTranslateStr(){
        return toSourceStr();
    }

    public QueryUnit toTranslateUnit(TranslateContext translateContext){
        throw new UnsupportedOperationException("类型"+this.getClass()+"不支持此方法！！！");
    }

    /**
     * 将当前AST转换为明细查询参数消息体
     * @param translateContext 翻译上下文环境
     * @return
     */
    public DetailQueryParamBody toQueryParamBody(TranslateContext translateContext){
        QueryUnit queryUnit = this.toTranslateUnit(translateContext);
        Map<String,Object> queryField = GsonUtil.getInstance().fromJson(queryUnit.toESQueryJsonEntity(),new TypeToken<HashMap<String,Object>>(){}.getType());
        DetailQueryParamBody detailQueryParamBody = new DetailQueryParamBody();
        detailQueryParamBody.setQuery(queryField);
        return detailQueryParamBody;
    }

    /**
     * 判断字段名是否是字段模糊查询,如果是多字段匹配，返回多字段匹配的字段名列表
     * @return 模糊匹配到的字段列表,若上下文中未匹配到对应的字段，则返回空列表
     */
    protected List<String> getMultiMatchField(FieldNameTreeNode fieldNameTreeNode, TranslateContext translateContext){
        String fieldName = fieldNameTreeNode.getFieldNameToken().getValue();
        if(translateContext == null){
            // 无索引上下文时，直接原样返回
            return Collections.singletonList(fieldName);
        }
        String distinctStr = "";
        boolean endFuzzy =true; // * 在字段前方
        if(fieldName.startsWith(FuzzyFieldCh)){
            if(fieldName.length() == 1){
                //只有一个 *
                return translateContext.getFields().stream().map(IndexField::getName).collect(Collectors.toList());
            }else{
                distinctStr = fieldName.substring(1);
            }
        } else if(fieldName.endsWith(FuzzyFieldCh)){
            endFuzzy =false;
            if(fieldName.length() == 1){
                //只有一个 *
                return translateContext.getFields().stream().map(IndexField::getName).collect(Collectors.toList());
            }else{
                distinctStr = fieldName.substring(0,fieldName.length()-1);
            }
        } else if (fieldName.contains(FuzzyFieldCh)){
            String errMsg = String.format("Token[value:%s,startIndex:%d]模糊匹配符[%s]位置非法！！！",
                    fieldName,fieldNameTreeNode.getFieldNameToken().getStartIndex(),FuzzyFieldCh);
            throw new DSLSemanticsException(errMsg);
        }
        if(distinctStr.contains(FuzzyFieldCh)){
            String errMsg = String.format("模糊匹配只能有一个模糊匹配符[*]，token[%s]中，有两个，请检查",fieldName);
            throw new DSLSemanticsException(errMsg);
        }
        final String searchStr = distinctStr;
        List<String> matchedResult =null;
        if(StringUtils.isNoneBlank(searchStr)){
            if(endFuzzy){
                matchedResult = translateContext.getFields().stream().map(IndexField::getName)
                        .filter(t->t.endsWith(searchStr)).collect(Collectors.toList());
            }else{
                matchedResult = translateContext.getFields().stream().map(IndexField::getName)
                        .filter(t->t.startsWith(searchStr)).collect(Collectors.toList());
            }
        }
        if(CollectionUtils.isEmpty(matchedResult)){
            //没有匹配返回空列表
            return Collections.emptyList();
        }
        return matchedResult;
    }

}
