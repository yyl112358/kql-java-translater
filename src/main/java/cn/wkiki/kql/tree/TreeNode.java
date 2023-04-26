package cn.wkiki.kql.tree;


import cn.wkiki.kql.Token;
import cn.wkiki.kql.TranslateContext;
import cn.wkiki.kql.es.IndexField;
import cn.wkiki.kql.exception.DSLSemanticsException;
import cn.wkiki.kql.exception.DSLSyntaxException;
import cn.wkiki.kql.exception.LimitSizeFormatException;
import cn.wkiki.kql.model.DetailQueryParamBody;
import cn.wkiki.kql.queryUnit.*;
import cn.wkiki.kql.util.GsonUtil;
import com.google.gson.reflect.TypeToken;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
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
    /**
     * 字段名节点
     */
    @Getter
    @Setter
    public static class FieldNameTreeNode extends TreeNode {

        public FieldNameTreeNode(){}

        public FieldNameTreeNode(Token fieldNameToken){
            this.fieldNameToken = fieldNameToken;
        }
        /**
         * 字段名
         */
        Token fieldNameToken;

        @Override
        public String toSourceStr() {
            return fieldNameToken.getValue();
        }
    }

    /**
     * 字面量节点
     */
    @Getter
    public static class LiteralValueTreeNode extends TreeNode {
        /**
         * 字面量
         */
        @Getter
        Token literalValueToken;

        public LiteralValueTreeNode(Token literalValue){
            this.literalValueToken = literalValue;
        }

        @Override
        public String toSourceStr() {
            return literalValueToken.getValue();
        }

        @Override
        public QueryUnit toTranslateUnit(TranslateContext translateContext) {
            return new MultiMatchQuery(literalValueToken.getValue(),false);
        }
    }

    /**
     * 短语查询字面量节点
     */
    public static class PhraseLiteralValueTreeNode extends TreeNode {
        /**
         * 字面量
         */
        @Getter
        String literalValue;

        public PhraseLiteralValueTreeNode(String literalValue){
            this.literalValue =literalValue;
        }

        @Override
        public String toSourceStr() {
            return "\""+literalValue+"\"";
        }

        @Override
        public QueryUnit toTranslateUnit(TranslateContext translateContext) {
            return new MultiMatchQuery(literalValue,true);
        }
    }

    /**
     * 全文查询节点
     */
    public static class TextMatchTreeNode extends TreeNode implements TextMatchStatement {

        @Getter
        private FieldNameTreeNode fieldNameTreeNode;

        @Getter
        private LiteralValueTreeNode literalValueTreeNode;

        public void setFieldNameTreeNode(FieldNameTreeNode fieldNameTreeNode) {
            this.fieldNameTreeNode = fieldNameTreeNode;
        }

        @Override
        public void setLiteralValueTreeNode(LiteralValueTreeNode literalValueTreeNode) {
            this.literalValueTreeNode =literalValueTreeNode;
        }

        @Override
        public String toSourceStr() {
            return fieldNameTreeNode.toSourceStr()+":"+literalValueTreeNode.toSourceStr();
        }

        @Override
        public QueryUnit toTranslateUnit(TranslateContext translateContext) {
            List<String> fuzzySearchFields = getMultiMatchField(fieldNameTreeNode,translateContext);
            if(CollectionUtils.isEmpty(fuzzySearchFields)){
                return new FullTextMatch(fieldNameTreeNode.getFieldNameToken().getValue(),literalValueTreeNode.getLiteralValueToken().getValue());
            }
            if(fuzzySearchFields.size()== 1){
                return new FullTextMatch(fuzzySearchFields.get(0),literalValueTreeNode.getLiteralValueToken().getValue());
            }
            else{
                //return new MultiMatchQuery(literalValueTreeNode.getLiteralValueToken().getValue(),false, fuzzySearchFields);
                BooleanQuery booleanQuery = new BooleanQuery();
                for (String fuzzySearchField : fuzzySearchFields) {
                    booleanQuery.addShould(new FullTextMatch(fuzzySearchField, literalValueTreeNode.getLiteralValueToken().getValue()));
                }
                return booleanQuery;
            }
        }
    }

    /**
     * 查询右侧带有括号"()"包裹的查询节点
     */
    public static class MatchWithBracketTreeNode extends TreeNode {

        @Setter
        @Getter
        private FieldNameTreeNode fieldNameTreeNode;
        /**
         * 右侧括号节点
         */
        @Getter
        @Setter
        public BracketTreeNode bracketTreeNode;

        @Override
        public String toSourceStr() {
            return fieldNameTreeNode.toSourceStr()+":"+bracketTreeNode.toSourceStr();
        }

        @Override
        public String toTranslateStr() {
            return fieldNameTreeNode.toTranslateStr()+":"+bracketTreeNode.toTranslateStr();
        }

        @Override
        public QueryUnit toTranslateUnit(TranslateContext translateContext) {
            TreeNode bracketInnerTreeNode = bracketTreeNode.getInnerNode();
            if(bracketInnerTreeNode.getClass().equals(MultiFieldMatchTreeNode.class)){
                return new FullTextMatch(fieldNameTreeNode.getFieldNameToken().getValue(), ((MultiFieldMatchTreeNode) bracketInnerTreeNode).getLiteralValueTreeNode().getLiteralValueToken().getValue());
            }if(bracketInnerTreeNode.getClass().equals(PhraseLiteralValueTreeNode.class)){
                return new PhraseMatch(fieldNameTreeNode.getFieldNameToken().getValue(), ((PhraseLiteralValueTreeNode) bracketInnerTreeNode).getLiteralValue());
            }else{
                return getBracketLogicQueryUnit(bracketInnerTreeNode);
            }
        }

        /**
         * 获取括号内的逻辑查询子树的查询单元
         * @param bracketInnerTreeNode
         * @return
         */
        private QueryUnit getBracketLogicQueryUnit(TreeNode bracketInnerTreeNode){
            BooleanQuery result = new BooleanQuery();
            if(bracketInnerTreeNode.getClass().equals(BracketTreeNode.class)){
                bracketInnerTreeNode = ((BracketTreeNode) bracketInnerTreeNode).getInnerNode();
            }
            String fieldName = fieldNameTreeNode.getFieldNameToken().getValue();
            if(bracketInnerTreeNode.getClass().equals(LogicCalcTreeNode.class)){
                if(((LogicCalcTreeNode) bracketInnerTreeNode).getLogicCalcToken().getType().equals(Token.Type.not)){
                    result.addMustNot(getBracketLogicQueryUnit(((LogicCalcTreeNode) bracketInnerTreeNode).getRightSubNode().getSubTreeNode()));
                }else if (((LogicCalcTreeNode) bracketInnerTreeNode).getLogicCalcToken().getType().equals(Token.Type.and)){
                    result.addMust(getBracketLogicQueryUnit(((LogicCalcTreeNode) bracketInnerTreeNode).getLeftSubNode().getSubTreeNode()));
                    result.addMust(getBracketLogicQueryUnit(((LogicCalcTreeNode) bracketInnerTreeNode).getRightSubNode().getSubTreeNode()));
                }else if(((LogicCalcTreeNode) bracketInnerTreeNode).getLogicCalcToken().getType().equals(Token.Type.or)){
                    result.addShould(getBracketLogicQueryUnit(((LogicCalcTreeNode) bracketInnerTreeNode).getLeftSubNode().getSubTreeNode()));
                    result.addShould(getBracketLogicQueryUnit(((LogicCalcTreeNode) bracketInnerTreeNode).getRightSubNode().getSubTreeNode()));
                }
            }else if (bracketInnerTreeNode.getClass().equals(MultiFieldMatchTreeNode.class)){
                return new FullTextMatch(fieldName,((MultiFieldMatchTreeNode) bracketInnerTreeNode).getLiteralValueTreeNode().getLiteralValueToken().getValue());
            }else if(bracketInnerTreeNode.getClass().equals(PhraseLiteralValueTreeNode.class)){
                return new PhraseMatch(fieldName,((PhraseLiteralValueTreeNode) bracketInnerTreeNode).getLiteralValue());
            }
            else{
                throw new DSLSemanticsException("带括号的属性查询，括号内只能是字面量与逻辑运算子树！！！");
            }
            return result;
        }
    }

    /**
     * 括号节点
     */
    public static class BracketTreeNode extends TreeNode implements QuotesStatement{

        /**
         * 左括号token
         */
        @Getter
        @Setter
        Token lbracketToken;
        /**
         * 右括号token
         */
        @Getter
        @Setter
        Token rbracketToken;

        @Getter
        TreeNode innerNode;

        @Override
        public void setInnerStatement(TreeNode innerNode) {
            this.innerNode = innerNode;
        }

        @Override
        public String toSourceStr() {
            return lbracketToken.getValue()+innerNode.toSourceStr()+rbracketToken.getValue();
        }

        @Override
        public String toTranslateStr() {
            return lbracketToken.getValue()+innerNode.toTranslateStr()+rbracketToken.getValue();
        }

        @Override
        public QueryUnit toTranslateUnit(TranslateContext translateContext) {
            return innerNode.toTranslateUnit(translateContext);
        }

        @Override
        public DetailQueryParamBody toQueryParamBody(TranslateContext translateContext) {
            return innerNode.toQueryParamBody(translateContext);
        }
    }

    /**
     * 多字段查询节点
     */
    public static class MultiFieldMatchTreeNode extends TreeNode {
        /**
         * 要匹配的字面量
         */
        @Getter
        @Setter
        private LiteralValueTreeNode literalValueTreeNode;

        @Override
        public String toSourceStr() {
            return literalValueTreeNode.toSourceStr();
        }

        @Override
        public QueryUnit toTranslateUnit(TranslateContext translateContext) {
            return new MultiMatchQuery(getLiteralValueTreeNode().getLiteralValueToken().getValue(),false);
        }
    }

    /**
     * 短语查询节点
     */
    public static class MatchPhraseTreeNode extends TreeNode implements MatchPhraseStatement{

        @Getter
        private PhraseLiteralValueTreeNode phraseLiteralValueTreeNode;

        @Getter
        private FieldNameTreeNode fieldNameTreeNode;

        @Override
        public void setPhaseLiteralValueTreeNode(PhraseLiteralValueTreeNode literalValueTreeNode) {
            this.phraseLiteralValueTreeNode = literalValueTreeNode;
        }

        @Override
        public void setFieldNameTreeNode(FieldNameTreeNode fieldNameTreeNode) {
            this.fieldNameTreeNode = fieldNameTreeNode;
        }

        @Override
        public String toSourceStr() {
            return fieldNameTreeNode.toSourceStr()+":"+phraseLiteralValueTreeNode.toSourceStr();
        }

        @Override
        public QueryUnit toTranslateUnit(TranslateContext translateContext) {
            List<String> fuzzySearchFields = getMultiMatchField(fieldNameTreeNode,translateContext);
            if(CollectionUtils.isEmpty(fuzzySearchFields)){
                return new PhraseMatch(getFieldNameTreeNode().getFieldNameToken().getValue(), getPhraseLiteralValueTreeNode().getLiteralValue());
            }
            if(fuzzySearchFields.size()== 1){
                return new PhraseMatch(fuzzySearchFields.get(0), getPhraseLiteralValueTreeNode().getLiteralValue());
            }
            else{
                BooleanQuery booleanQuery = new BooleanQuery();
                for (String fuzzySearchField : fuzzySearchFields) {
                    PhraseMatch phraseMatch = new PhraseMatch(fuzzySearchField, getPhraseLiteralValueTreeNode().getLiteralValue());
                    booleanQuery.addShould(phraseMatch);
                }
                return booleanQuery;
            }
        }
    }

    /**
     * 关系查询节点
     */
    public static class RelationCalcTreeNode extends TreeNode implements RelationCalcStatement{

        @Getter
        private FieldNameTreeNode fieldNameTreeNode;
        @Getter
        private Token relationCalcToken;
        @Getter
        private Class<?> literalValueTreeNodeClazz;
        @Getter
        private LiteralValueTreeNode literalValueTreeNode;
        @Getter
        private PhraseLiteralValueTreeNode phraseLiteralValueTreeNode;

        public void setFieldNameTreeNode(FieldNameTreeNode fieldNameTreeNode) {
            this.fieldNameTreeNode = fieldNameTreeNode;
        }

        @Override
        public void setRelationCalcToken(Token relationCalcToken) {
            switch (relationCalcToken.getType()){
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
            if(this.literalValueTreeNodeClazz ==null){
                this.literalValueTreeNode = literalValueTreeNode;
                literalValueTreeNodeClazz = LiteralValueTreeNode.class;
            }else{
                String errMessage =String.format("当前关系查询节点已被设置了字面量值[type:%s,value:%s]", literalValueTreeNodeClazz.getName(),getLiteralValue());
                throw new RuntimeException(errMessage);
            }
        }

        @Override
        public void setPhraseLiteralValueTreeNode(PhraseLiteralValueTreeNode phraseLiteralValueTreeNode) {
            if(this.literalValueTreeNodeClazz ==null){
                this.phraseLiteralValueTreeNode = phraseLiteralValueTreeNode;
                literalValueTreeNodeClazz = PhraseLiteralValueTreeNode.class;
            }else{
                String errMessage =String.format("当前关系查询节点已被设置了字面量值[type:%s,value:%s]", literalValueTreeNodeClazz.getName(),getLiteralValue());
                throw new RuntimeException(errMessage);
            }
        }

        /**
         * 获取字面量字符串值
         * @return
         */
        private String getLiteralValue(){
             if(literalValueTreeNodeClazz == null){
                 return null;
             }
             if(literalValueTreeNodeClazz == LiteralValueTreeNode.class){
                 return literalValueTreeNode.getLiteralValueToken().getValue();
             }else if(literalValueTreeNodeClazz == PhraseLiteralValueTreeNode.class){
                 return phraseLiteralValueTreeNode.getLiteralValue();
             }else{
                 throw new RuntimeException("未知的的字面量类型！！");
             }
        }

        @Override
        public String toSourceStr() {
            String result = "";
            result+= fieldNameTreeNode.toSourceStr();
            result+= relationCalcToken.getValue();
            result+= literalValueTreeNodeClazz == LiteralValueTreeNode.class? literalValueTreeNode.toSourceStr() : phraseLiteralValueTreeNode.toSourceStr();
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
            if (intValue != null) {
                return new FieldRelationSearch<>(getFieldNameTreeNode().getFieldNameToken().getValue(),
                        getRelationCalcToken().getType(), intValue);
            } else if (doubleValue != null) {
                return new FieldRelationSearch<>(getFieldNameTreeNode().getFieldNameToken().getValue(),
                        getRelationCalcToken().getType(), doubleValue);
            } else {
                return new FieldRelationSearch<>(getFieldNameTreeNode().getFieldNameToken().getValue(),
                        getRelationCalcToken().getType(),
                        literalValue);
            }
        }
    }

    /**
     * 逻辑查询节点
     */
    public static class LogicCalcTreeNode extends TreeNode implements LogicCalcStatement{

        @Getter
        SubTreeNode leftSubNode;
        @Getter
        Token logicCalcToken;
        @Getter
        SubTreeNode rightSubNode;

        @Override
        public void setLeftSubNode(SubTreeNode leftSubNode) {
            this.leftSubNode = leftSubNode;
        }

        @Override
        public void setLogicCalcToken(Token logicCalcToken) {
            switch (logicCalcToken.getType()){
                case and:
                case or:
                case not:
                    this.logicCalcToken = logicCalcToken;
                    break;
                default:
                    String errMsg = String.format("关系运算树节点，只允许使用[and,or,not]运算符，提交的运算符[%s]非法", logicCalcToken.getType().getValue());
                    throw new RuntimeException(errMsg);
            }
        }

        @Override
        public void setRightSubNode(SubTreeNode rightSubNode) {
            this.rightSubNode = rightSubNode;
        }

        /**
         * 逻辑查询节点的子节点
         */
        public static class SubTreeNode extends TreeNode {

            public SubTreeNode(){}

            public SubTreeNode(TreeNode subTreeNode){
                this.setSubTreeNode(subTreeNode);
            }

            final Class[] acceptSubTreeNodeClazz = new Class[]{TextMatchTreeNode.class,
                    MatchPhraseTreeNode.class,
                    MultiFieldMatchTreeNode.class,
                    RelationCalcTreeNode.class,
                    LogicCalcTreeNode.class,
                    BracketTreeNode.class,
                    PhraseLiteralValueTreeNode.class,
                    MatchWithBracketTreeNode.class};

            @Getter
            private TreeNode subTreeNode;

            @Getter
            private Class subTreeNodeClazz;

            public void setSubTreeNode(TreeNode subTreeNode) {
                if(this.subTreeNodeClazz ==null){
                    final Class subTreeNodeClazz = subTreeNode.getClass();
                    if(Arrays.stream(acceptSubTreeNodeClazz).anyMatch(t->t.equals(subTreeNodeClazz))){
                        this.subTreeNode = subTreeNode;
                        this.subTreeNodeClazz = subTreeNodeClazz;
                    }else{
                        String acceptClassStr = Arrays.stream(acceptSubTreeNodeClazz).map(Class::getSimpleName).collect(Collectors.joining(","));
                        String errMsg = String.format("逻辑查询语句子树仅支持接受[%s]类型的节点,设置的结点类型为[%s]",
                                acceptClassStr,
                                subTreeNode.getClass().getSimpleName());
                        throw new RuntimeException(errMsg);
                    }
                }else{
                    String errMessage =String.format("当前逻辑查询子节点已被设置类型为[type:%s]的值", subTreeNodeClazz.getName());
                    throw new RuntimeException(errMessage);
                }
            }

            @Override
            public String toSourceStr() {
                return subTreeNode.toSourceStr();
            }

            @Override
            public String toTranslateStr() {
                return subTreeNode.toTranslateStr();
            }

            @Override
            public QueryUnit toTranslateUnit(TranslateContext translateContext) {
                return getSubTreeNode().toTranslateUnit(translateContext);
            }
        }

        @Override
        public String toSourceStr() {
            String result = "";
            if(leftSubNode != null){
                result+=leftSubNode.toSourceStr();
            }
            result += " "+logicCalcToken.getValue();
            if(rightSubNode != null){
                result+=" "+rightSubNode.toSourceStr();
            }
            return result;
        }

        @Override
        public String toTranslateStr() {
            String result = "";
            if(leftSubNode != null){
                if(leftSubNode.getSubTreeNode().getClass().equals(LogicCalcTreeNode.class)){
                    result+="("+leftSubNode.toTranslateStr()+")";
                }else{
                    result+=leftSubNode.toTranslateStr();
                }
            }
            result += " "+logicCalcToken.getValue()+" ";
            if(rightSubNode != null){
                if(rightSubNode.getSubTreeNode().getClass().equals(LogicCalcTreeNode.class)){
                    result+="("+rightSubNode.toTranslateStr()+")";
                }else{
                    result+=rightSubNode.toTranslateStr();
                }
            }
            return result;
        }

        @Override
        public QueryUnit toTranslateUnit(TranslateContext translateContext) {
            BooleanQuery result =new BooleanQuery();
            if(getLogicCalcToken().getType().equals(Token.Type.and)){
                result.addMust(getLeftSubNode().toTranslateUnit(translateContext));
                result.addMust(getRightSubNode().toTranslateUnit(translateContext));
            }
            if(getLogicCalcToken().getType().equals(Token.Type.or)){
                result.addShould(getLeftSubNode().toTranslateUnit(translateContext));
                result.addShould(getRightSubNode().toTranslateUnit(translateContext));
            }
            if(getLogicCalcToken().getType().equals(Token.Type.not)){
                result.addMustNot(getRightSubNode().toTranslateUnit(translateContext));
            }
            return result;
        }
    }

    /**
     * 聚合查询节点
     */
    public static class AggregationTreeNode extends TreeNode implements AggregationStatement{

        TreeNode filterTree;

        /**
         * 聚合选择使用的字段 字符串常量节点
         */
        @Getter
        LiteralValueTreeNode literalValueTreeNode;

        /**
         * 聚合函数token
         */
        @Getter
        Token aggregationFunctionToken;

        @Override
        public void setFilterTree(TreeNode filterTree) {
            if(filterTree == null){
                throw new DSLSemanticsException("聚合语句必须要有过滤子树");
            }
            if(filterTree.getClass().equals(BracketTreeNode.class)){
                this.filterTree = filterTree;
            }else{
                throw new DSLSemanticsException("聚合语句的过滤子句必须为括号子树");
            }
        }

        @Override
        public void setAggregationFiledToken(LiteralValueTreeNode literalValueTreeNode) {
            this.literalValueTreeNode = literalValueTreeNode;
        }

        @Override
        public void setAggregationFunctionToken(Token aggregationFunctionToken) {
            switch (aggregationFunctionToken.getType()){
                case avg:
                case max:
                case min:
                case sum:
                case count:
                case terms:
                    this.aggregationFunctionToken = aggregationFunctionToken;
                    break;
                default:
                    String errMsg =String.format("未知的聚合类型[type:%s,value:%s]",aggregationFunctionToken.getType(),aggregationFunctionToken.getValue());
                    throw new DSLSyntaxException(aggregationFunctionToken,aggregationFunctionToken.getValue(),errMsg);
            }
        }

        @Override
        public String toSourceStr() {
            return "group by("+ literalValueTreeNode.getLiteralValueToken().getValue()+") "+ aggregationFunctionToken.getValue();
        }

        @Override
        public QueryUnit toTranslateUnit(TranslateContext translateContext) {
            return this.filterTree.toTranslateUnit(translateContext);
        }

        @Override
        public DetailQueryParamBody toQueryParamBody(TranslateContext translateContext) {
            DetailQueryParamBody result = filterTree.toQueryParamBody(translateContext);
            Token aggFunctionToken = this.getAggregationFunctionToken();
            String aggFieldName  = this.getLiteralValueTreeNode().getLiteralValueToken().getValue();
            if(translateContext!=null && translateContext.getFields().stream().noneMatch(t->t.getName().equals(aggFieldName))){
                String errMsg =String.format("要聚合的字段[%s]不在索引的属性列表中", aggFieldName);
                throw new DSLSemanticsException(errMsg);
            }
            if(result.getAggs() == null){
                result.setAggs(new HashMap<>());
            }
            String aggName ="";
            Map<String,Map<String,String>> aggBody = new HashMap();
            switch (aggFunctionToken.getType()){
                case avg:
                    aggName ="avg";
                    break;
                case stat:
                    aggName ="stats";
                case max:
                    aggName ="max";
                    break;
                case min:
                    aggName ="min";
                    break;
                case sum:
                    aggName ="sum";
                    break;
                case count:
                    aggName ="value_count";
                    break;
                case terms:
                    aggName="terms";
                    break;
                default:
                    throw new DSLSemanticsException("未知的聚合类型:"+aggFunctionToken.getValue());

            }
            HashMap<String,String> aggsConfigMap = new HashMap<>();
            aggsConfigMap.put("field",aggFieldName);
            aggBody.put(aggName, aggsConfigMap);
            result.getAggs().put(aggName, aggBody);
            return result;
        }
    }

    /**
     * 空节点，用于处理 “()” 空括号内需要有子节点的逻辑
     */
    public static class EmptyTreeNode extends TreeNode {
        final String EMPTY_STR="";
        @Override
        public String toSourceStr() {
            return EMPTY_STR;
        }

        @Override
        public String toTranslateStr() {
            return EMPTY_STR;
        }

        @Override
        public QueryUnit toTranslateUnit(TranslateContext translateContext) {
            return new EmptyUnit();
        }
    }

    public static class LimitResultTreeNode extends TreeNode implements LimitResultStatement{
        TreeNode searchStatement;

        Token limitToken;

        Token limitValueToken;

        int limitSize = 0;
        @Override
        public void setSearchStatement(TreeNode searchStatement) {
            if(searchStatement !=null){
                this.searchStatement = searchStatement;
            }
        }

        @Override
        public void setLimitToken(Token limitToken) {
            if(limitToken != null&& limitToken.getType().equals(Token.Type.limit)){
                this.limitToken = limitToken;
            }else{
                throw new DSLSemanticsException("limit token 不可为null且仅能为 limit");
            }
        }

        @Override
        public void setLimitValueToken(Token limitValueToken) throws LimitSizeFormatException {
            if(limitValueToken !=null){
                if(limitValueToken.getType().equals(Token.Type.identifier)
                    ||limitValueToken.getType().equals(Token.Type.literalValue)){
                    this.limitValueToken = limitValueToken;
                    this.limitSize = Integer.parseInt(limitValueToken.getValue());
                }else{
                    throw new DSLSemanticsException("limit value token 的token类型仅可为 identifier 或 literalValue");
                }
            }else {
                throw new DSLSemanticsException("limit value token 不可为null");
            }
        }

        @Override
        public String toSourceStr() {
            return searchStatement.toSourceStr()+" "+limitToken.getValue()+" "+limitValueToken.getValue();
        }

        @Override
        public QueryUnit toTranslateUnit(TranslateContext translateContext) {
            return this.searchStatement.toTranslateUnit(translateContext);
        }

        @Override
        public DetailQueryParamBody toQueryParamBody(TranslateContext translateContext) {
            DetailQueryParamBody result = super.toQueryParamBody(translateContext);
            result.setSize(this.limitSize);
            return result;
        }
    }
}
