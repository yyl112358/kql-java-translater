package cn.wkiki.kql.tree.TreeNodeImpl.FieldSearchTreeNode;

import cn.wkiki.kql.Token;
import cn.wkiki.kql.TranslateContext;
import cn.wkiki.kql.exception.DSLSemanticsException;
import cn.wkiki.kql.queryUnit.BooleanQuery;
import cn.wkiki.kql.queryUnit.FullTextMatch;
import cn.wkiki.kql.queryUnit.PhraseMatch;
import cn.wkiki.kql.queryUnit.QueryUnit;
import cn.wkiki.kql.tree.TreeNode;
import cn.wkiki.kql.tree.TreeNodeImpl.BracketTreeNode;
import cn.wkiki.kql.tree.TreeNodeImpl.LogicCalcTreeNode;
import cn.wkiki.kql.tree.TreeNodeImpl.MultiFieldMatchTreeNode;
import cn.wkiki.kql.tree.TreeNodeImpl.PhraseLiteralValueTreeNode;
import lombok.Getter;
import lombok.Setter;

/**
 * 查询右侧带有括号"()"包裹的查询节点
 */
public class MatchWithBracketTreeNode extends FieldSearchTreeNode {

    /**
     * 右侧括号节点
     */
    @Getter
    @Setter
    public BracketTreeNode bracketTreeNode;

    @Override
    public String toSourceStr() {
        return fieldNameTreeNode.toSourceStr() + ":" + bracketTreeNode.toSourceStr();
    }

    @Override
    public String toTranslateStr() {
        return fieldNameTreeNode.toTranslateStr() + ":" + bracketTreeNode.toTranslateStr();
    }

    @Override
    public QueryUnit toTranslateUnit(TranslateContext translateContext) {
        TreeNode bracketInnerTreeNode = bracketTreeNode.getInnerNode();
        if (bracketInnerTreeNode.getClass().equals(MultiFieldMatchTreeNode.class)) {
            return new FullTextMatch(fieldNameTreeNode.getFieldNameToken().getValue(), ((MultiFieldMatchTreeNode) bracketInnerTreeNode).getLiteralValueTreeNode().getLiteralValueToken().getValue());
        }
        if (bracketInnerTreeNode.getClass().equals(PhraseLiteralValueTreeNode.class)) {
            return new PhraseMatch(fieldNameTreeNode.getFieldNameToken().getValue(), ((PhraseLiteralValueTreeNode) bracketInnerTreeNode).getLiteralValue());
        } else {
            return getBracketLogicQueryUnit(bracketInnerTreeNode);
        }
    }

    /**
     * 获取括号内的逻辑查询子树的查询单元
     *
     * @param bracketInnerTreeNode
     * @return
     */
    private QueryUnit getBracketLogicQueryUnit(TreeNode bracketInnerTreeNode) {
        BooleanQuery result = new BooleanQuery();
        if (bracketInnerTreeNode.getClass().equals(BracketTreeNode.class)) {
            bracketInnerTreeNode = ((BracketTreeNode) bracketInnerTreeNode).getInnerNode();
        }
        String fieldName = fieldNameTreeNode.getFieldNameToken().getValue();
        if (bracketInnerTreeNode.getClass().equals(LogicCalcTreeNode.class)) {
            if (((LogicCalcTreeNode) bracketInnerTreeNode).getLogicCalcToken().getType().equals(Token.Type.not)) {
                result.addMustNot(getBracketLogicQueryUnit(((LogicCalcTreeNode) bracketInnerTreeNode).getRightSubNode().getSubTreeNode()));
            } else if (((LogicCalcTreeNode) bracketInnerTreeNode).getLogicCalcToken().getType().equals(Token.Type.and)) {
                result.addMust(getBracketLogicQueryUnit(((LogicCalcTreeNode) bracketInnerTreeNode).getLeftSubNode().getSubTreeNode()));
                result.addMust(getBracketLogicQueryUnit(((LogicCalcTreeNode) bracketInnerTreeNode).getRightSubNode().getSubTreeNode()));
            } else if (((LogicCalcTreeNode) bracketInnerTreeNode).getLogicCalcToken().getType().equals(Token.Type.or)) {
                result.addShould(getBracketLogicQueryUnit(((LogicCalcTreeNode) bracketInnerTreeNode).getLeftSubNode().getSubTreeNode()));
                result.addShould(getBracketLogicQueryUnit(((LogicCalcTreeNode) bracketInnerTreeNode).getRightSubNode().getSubTreeNode()));
            }
        } else if (bracketInnerTreeNode.getClass().equals(MultiFieldMatchTreeNode.class)) {
            return new FullTextMatch(fieldName, ((MultiFieldMatchTreeNode) bracketInnerTreeNode).getLiteralValueTreeNode().getLiteralValueToken().getValue());
        } else if (bracketInnerTreeNode.getClass().equals(PhraseLiteralValueTreeNode.class)) {
            return new PhraseMatch(fieldName, ((PhraseLiteralValueTreeNode) bracketInnerTreeNode).getLiteralValue());
        } else {
            throw new DSLSemanticsException("带括号的属性查询，括号内只能是字面量与逻辑运算子树！！！");
        }
        return result;
    }
}
