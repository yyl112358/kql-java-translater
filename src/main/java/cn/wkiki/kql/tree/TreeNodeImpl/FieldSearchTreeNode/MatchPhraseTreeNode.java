package cn.wkiki.kql.tree.TreeNodeImpl.FieldSearchTreeNode;

import cn.wkiki.kql.TranslateContext;
import cn.wkiki.kql.queryUnit.BooleanQuery;
import cn.wkiki.kql.queryUnit.PhraseMatch;
import cn.wkiki.kql.queryUnit.QueryUnit;
import cn.wkiki.kql.tree.MatchPhraseStatement;
import cn.wkiki.kql.tree.TreeNodeImpl.PhraseLiteralValueTreeNode;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;

/**
 * 短语查询节点
 */
public class MatchPhraseTreeNode extends FieldSearchTreeNode implements MatchPhraseStatement {

    @Getter
    private PhraseLiteralValueTreeNode phraseLiteralValueTreeNode;

    @Override
    public void setPhaseLiteralValueTreeNode(PhraseLiteralValueTreeNode literalValueTreeNode) {
        this.phraseLiteralValueTreeNode = literalValueTreeNode;
    }

    @Override
    public String toSourceStr() {
        return fieldNameTreeNode.toSourceStr() + ":" + phraseLiteralValueTreeNode.toSourceStr();
    }

    @Override
    public QueryUnit toTranslateUnit(TranslateContext translateContext) {
        if(isNestedProp()){
            return new PhraseMatch(getNestedPropName(), getPhraseLiteralValueTreeNode().getLiteralValue());
        }else{
            List<String> fuzzySearchFields = getMultiMatchField(fieldNameTreeNode, translateContext);
            if (CollectionUtils.isEmpty(fuzzySearchFields)) {
                return new PhraseMatch(getFieldNameTreeNode().getFieldNameToken().getValue(), getPhraseLiteralValueTreeNode().getLiteralValue());
            }
            if (fuzzySearchFields.size() == 1) {
                return new PhraseMatch(fuzzySearchFields.get(0), getPhraseLiteralValueTreeNode().getLiteralValue());
            } else {
                BooleanQuery booleanQuery = new BooleanQuery();
                for (String fuzzySearchField : fuzzySearchFields) {
                    PhraseMatch phraseMatch = new PhraseMatch(fuzzySearchField, getPhraseLiteralValueTreeNode().getLiteralValue());
                    booleanQuery.addShould(phraseMatch);
                }
                return booleanQuery;
            }
        }
    }
}
