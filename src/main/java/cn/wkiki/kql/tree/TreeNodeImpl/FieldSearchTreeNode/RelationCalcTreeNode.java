package cn.wkiki.kql.tree.TreeNodeImpl.FieldSearchTreeNode;

import cn.wkiki.kql.Token;
import cn.wkiki.kql.TranslateContext;
import cn.wkiki.kql.exception.DSLSemanticsException;
import cn.wkiki.kql.queryUnit.FieldRelationSearch;
import cn.wkiki.kql.queryUnit.QueryUnit;
import cn.wkiki.kql.tree.RelationCalcStatement;
import cn.wkiki.kql.tree.TreeNodeImpl.LiteralValueTreeNode;
import cn.wkiki.kql.tree.TreeNodeImpl.PhraseLiteralValueTreeNode;
import lombok.Getter;

/**
 * 关系查询节点
 */
public class RelationCalcTreeNode extends FieldSearchTreeNode implements RelationCalcStatement {

    @Getter
    private Token relationCalcToken;
    @Getter
    private Class<?> literalValueTreeNodeClazz;
    @Getter
    private LiteralValueTreeNode literalValueTreeNode;
    @Getter
    private PhraseLiteralValueTreeNode phraseLiteralValueTreeNode;

    @Override
    public void setRelationCalcToken(Token relationCalcToken) {
        switch (relationCalcToken.getType()) {
            case gt:
            case lt:
            case gte:
            case lte:
                this.relationCalcToken = relationCalcToken;
                break;
            default:
                String errMsg = String.format("关系运算树节点，只允许使用[>,<,>=,<=]运算符，提交的运算符[%s]非法", relationCalcToken.getType().getValue());
                throw new RuntimeException(errMsg);
        }
    }

    @Override
    public void setLiteralValueTreeNode(LiteralValueTreeNode literalValueTreeNode) {
        if (this.literalValueTreeNodeClazz == null) {
            this.literalValueTreeNode = literalValueTreeNode;
            literalValueTreeNodeClazz = LiteralValueTreeNode.class;
        } else {
            String errMessage = String.format("当前关系查询节点已被设置了字面量值[type:%s,value:%s]", literalValueTreeNodeClazz.getName(), getLiteralValue());
            throw new RuntimeException(errMessage);
        }
    }

    @Override
    public void setPhraseLiteralValueTreeNode(PhraseLiteralValueTreeNode phraseLiteralValueTreeNode) {
        if (this.literalValueTreeNodeClazz == null) {
            this.phraseLiteralValueTreeNode = phraseLiteralValueTreeNode;
            literalValueTreeNodeClazz = PhraseLiteralValueTreeNode.class;
        } else {
            String errMessage = String.format("当前关系查询节点已被设置了字面量值[type:%s,value:%s]", literalValueTreeNodeClazz.getName(), getLiteralValue());
            throw new RuntimeException(errMessage);
        }
    }

    /**
     * 获取字面量字符串值
     *
     * @return
     */
    private String getLiteralValue() {
        if (literalValueTreeNodeClazz == null) {
            return null;
        }
        if (literalValueTreeNodeClazz == LiteralValueTreeNode.class) {
            return literalValueTreeNode.getLiteralValueToken().getValue();
        } else if (literalValueTreeNodeClazz == PhraseLiteralValueTreeNode.class) {
            return phraseLiteralValueTreeNode.getLiteralValue();
        } else {
            throw new RuntimeException("未知的的字面量类型！！");
        }
    }

    @Override
    public String toSourceStr() {
        String result = "";
        result += fieldNameTreeNode.toSourceStr();
        result += relationCalcToken.getValue();
        result += literalValueTreeNodeClazz == LiteralValueTreeNode.class ? literalValueTreeNode.toSourceStr() : phraseLiteralValueTreeNode.toSourceStr();
        return result;
    }

    @Override
    public QueryUnit toTranslateUnit(TranslateContext translateContext) {
        String literalValue = getLiteralValue();
        if (literalValue == null) {
            String errMsg = String.format("关系查询无字面量,token信息[%s]", getFieldNameTreeNode().getFieldNameToken());
            throw new DSLSemanticsException(errMsg);
        }
        Integer intValue = null;
        Double doubleValue = null;
        try {
            intValue = Integer.parseInt(literalValue);
        } catch (NumberFormatException exception) {
            try {
                doubleValue = Double.parseDouble(literalValue);
            } catch (NumberFormatException e) {
                // do nothing
            }
        }
        String fieldName = isNestedProp()?getNestedPropName():fieldNameTreeNode.getFieldNameToken().getValue();
        if (intValue != null) {
            return new FieldRelationSearch<>(fieldName, getRelationCalcToken().getType(), intValue);
        } else if (doubleValue != null) {
            return new FieldRelationSearch<>(fieldName, getRelationCalcToken().getType(), doubleValue);
        } else {
            return new FieldRelationSearch<>(fieldName, getRelationCalcToken().getType(), literalValue);
        }
    }
}
