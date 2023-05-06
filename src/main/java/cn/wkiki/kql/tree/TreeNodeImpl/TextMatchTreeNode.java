package cn.wkiki.kql.tree.TreeNodeImpl;

import cn.wkiki.kql.TranslateContext;
import cn.wkiki.kql.queryUnit.BooleanQuery;
import cn.wkiki.kql.queryUnit.FullTextMatch;
import cn.wkiki.kql.queryUnit.QueryUnit;
import cn.wkiki.kql.tree.TextMatchStatement;
import cn.wkiki.kql.tree.TreeNode;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;

/**
 * 全文查询节点
 */
public class TextMatchTreeNode extends TreeNode implements TextMatchStatement {

    @Getter
    private FieldNameTreeNode fieldNameTreeNode;

    @Getter
    private LiteralValueTreeNode literalValueTreeNode;

    public void setFieldNameTreeNode(FieldNameTreeNode fieldNameTreeNode) {
        this.fieldNameTreeNode = fieldNameTreeNode;
    }

    @Override
    public void setLiteralValueTreeNode(LiteralValueTreeNode literalValueTreeNode) {
        this.literalValueTreeNode = literalValueTreeNode;
    }

    @Override
    public String toSourceStr() {
        return fieldNameTreeNode.toSourceStr() + ":" + literalValueTreeNode.toSourceStr();
    }

    @Override
    public QueryUnit toTranslateUnit(TranslateContext translateContext) {
        List<String> fuzzySearchFields = getMultiMatchField(fieldNameTreeNode, translateContext);
        if (CollectionUtils.isEmpty(fuzzySearchFields)) {
            return new FullTextMatch(fieldNameTreeNode.getFieldNameToken().getValue(), literalValueTreeNode.getLiteralValueToken().getValue());
        }
        if (fuzzySearchFields.size() == 1) {
            return new FullTextMatch(fuzzySearchFields.get(0), literalValueTreeNode.getLiteralValueToken().getValue());
        } else {
            //return new MultiMatchQuery(literalValueTreeNode.getLiteralValueToken().getValue(),false, fuzzySearchFields);
            BooleanQuery booleanQuery = new BooleanQuery();
            for (String fuzzySearchField : fuzzySearchFields) {
                booleanQuery.addShould(new FullTextMatch(fuzzySearchField, literalValueTreeNode.getLiteralValueToken().getValue()));
            }
            return booleanQuery;
        }
    }
}
