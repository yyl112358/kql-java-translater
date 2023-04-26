package cn.wkiki.kql;

import cn.wkiki.kql.exception.DSLExpectTokenNotExistException;
import cn.wkiki.kql.exception.DSLSemanticsException;
import cn.wkiki.kql.exception.DSLSyntaxException;
import cn.wkiki.kql.tree.TreeNode;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class Parser {

    PerReadAbleLexicalAnalysis lexicalAnalysis;

    String source;

    public Parser(String source){
        this.source = source;
        lexicalAnalysis = new PerReadAbleLexicalAnalysis(source);
        lexicalAnalysis.nextToken();
    }

    /**
     * 获取语法树
     * @return
     */
    public TreeNode getAST(){
        TreeNode perTreeNode = null;
        boolean greedMatchLogicToken = lexicalAnalysis.token().getType().equals(Token.Type.not);
        do {
            perTreeNode = getSubTreeNode(greedMatchLogicToken,perTreeNode,lexicalAnalysis.token());
            lexicalAnalysis.nextToken();
        }while (lexicalAnalysis.token()!=null);
        return perTreeNode;
    }

    /**
     * 获取一个子树
     * @param startToken 开始token
     * @param bracketGreedMatchLogicToken 当子树是BracketTreeNode时，控制是否向后贪心匹配一个逻辑运算符
     * @return
     */
    private TreeNode getSubTreeNode(boolean bracketGreedMatchLogicToken, TreeNode perTreeNode, Token startToken){
        switch (startToken.getType()){
            case identifier:
                return processTextMatchOrPhraseMatchOrRelation(startToken);
            case literalValue:
                if(perTreeNode == null || perTreeNode.getClass().equals(TreeNode.LogicCalcTreeNode.class)
                || perTreeNode.getClass().equals(TreeNode.BracketTreeNode.class)){
                    return processMultiField(startToken);
                }
                throw new DSLSyntaxException(startToken, source, "多字段匹配查询前只能是 空、逻辑运算子树或括号运算树");
            case and:
            case or:
            case not:
                return processLogicCalcTree(perTreeNode,startToken);
            case lbracket:
                if(perTreeNode == null || perTreeNode.getClass().equals(TreeNode.LogicCalcTreeNode.class)
                ||perTreeNode.getClass().equals(TreeNode.BracketTreeNode.class)){
                    return processBracketTree(bracketGreedMatchLogicToken,startToken);
                }
                throw new DSLSyntaxException(startToken, source, "括号前只能是 空、逻辑运算子树或括号运算符");
            case quotes:
                if(perTreeNode == null || perTreeNode.getClass().equals(TreeNode.LogicCalcTreeNode.class)){
                    return collectPhraseLiteralValue(startToken);
                }
                throw new DSLSyntaxException(startToken, source, "引号(\")前只能是null或逻辑运算子树");
            case group:
                return processAggregationTree(perTreeNode, startToken);
            case limit:
                return processLimitResultTree(perTreeNode,startToken);
            default:
                String syntaxErrMsg="";
                if(perTreeNode== null){
                    syntaxErrMsg = String.format("第一个token只能以[%s,%s,%s,%s,%s]开头,当前token[%s]"
                            ,Token.Type.identifier
                            ,Token.Type.literalValue
                            ,Token.Type.not
                            ,Token.Type.lbracket
                            ,Token.Type.quotes
                            ,startToken.getValue());
                }else{
                    syntaxErrMsg = String.format("语义无法理解当前关系,上一个token为[%s],当前token[%s]", lexicalAnalysis.pervToken,startToken);
                }
                throw new DSLSyntaxException(startToken, source, syntaxErrMsg);
        }
    }

    /**
     * 从一个 “ 开始收集 短语匹配的字面量
     * @param quoteToken
     * @return
     */
    private TreeNode.PhraseLiteralValueTreeNode collectPhraseLiteralValue(Token quoteToken){
        lexicalAnalysis.nextToken();
        Token nextToken =null;
        Token perToken  = null;
        LinkedList<Token> midTokens = new LinkedList<>();
        while ((nextToken = lexicalAnalysis.token())!=null){
            if(nextToken.getType()== Token.Type.quotes){
                if(perToken!=null && perToken.getType().equals(Token.Type.strEscape)){
                    //当前引号为转义引号，是字符串里的一部分，不是结束符
                    midTokens.removeLast();
                    midTokens.add(lexicalAnalysis.token());
                    lexicalAnalysis.nextToken();
                    perToken = nextToken;
                    continue;
                }
                break;
            }else{
                midTokens.add(lexicalAnalysis.token());
                lexicalAnalysis.nextToken();
            }
            perToken = nextToken;
        }
        if(nextToken == null){
            throw new DSLSyntaxException(quoteToken, source, "未找到闭合的[\"]符号");
        }else{
            String phraseLiteralValue = "";
            if(CollectionUtils.isNotEmpty(midTokens)){
                StringBuilder stringBuilder = new StringBuilder();
                for (Token midToken : midTokens) {
                    stringBuilder.append(midToken.getValue());
                }
                phraseLiteralValue = stringBuilder.toString();
            }
            TreeNode.PhraseLiteralValueTreeNode result = new TreeNode.PhraseLiteralValueTreeNode(phraseLiteralValue);
            return result;
        }
    }


    /**
     * 处理全文匹配，短语匹配和关系匹配 AST
     * @param startToken 开始树的token
     * @return 一个全文匹配或短语匹配或关系匹配的子树 或 null(未匹配这三种类型)
     */
    private TreeNode processTextMatchOrPhraseMatchOrRelation(Token startToken) throws DSLSyntaxException{
        List<Token.Type> acceptTypes = Arrays.asList(Token.Type.colon,Token.Type.gt,Token.Type.gte,Token.Type.lt,Token.Type.lte);
        String acceptTypeNames = acceptTypes.stream().map(Token.Type::name).collect(Collectors.joining(","));
        Token nextToken = lexicalAnalysis.perReadToken();
        String exceptionMsg = String.format("期望的下一个token类型[%s]不匹配,下一个token[%s]",acceptTypeNames,nextToken);
        acceptNextToken(startToken,acceptTypes,exceptionMsg);
        switch (nextToken.getType()){
            case colon:
                lexicalAnalysis.nextToken();
                Token perReadToken = lexicalAnalysis.perReadToken();
                if(perReadToken == null){
                    throw new DSLSyntaxException(lexicalAnalysis.token(), source, "符号':'后不可为空！！");
                }
                if(perReadToken.getType().equals(Token.Type.literalValue)){
                    return processTextMatchTree(startToken, lexicalAnalysis.token());
                }else if(perReadToken.getType().equals(Token.Type.quotes)){
                    return processMatchPhraseTree(startToken, lexicalAnalysis.token());
                }else if (perReadToken.getType().equals(Token.Type.lbracket)){
                    return processMatchWithBracketTree(startToken, lexicalAnalysis.token());
                }else{
                    throw new DSLSyntaxException(lexicalAnalysis.token(),source,"查询符号':'后只允许是[字面量,\",(]");
                }
            case lt:
            case lte:
            case gt:
            case gte:
                lexicalAnalysis.nextToken();
                return processRelationCalcTree(startToken,nextToken);
        }
        return null;
    }

    /**
     * 处理一个全文查询的子树
     * @param identifierToken 属性标记符token
     * @param colonToken 查询冒号token
     * @return
     */
    private TreeNode.TextMatchTreeNode processTextMatchTree(Token identifierToken, Token colonToken){
        TreeNode.TextMatchTreeNode result = new TreeNode.TextMatchTreeNode();
        TreeNode.FieldNameTreeNode fieldNameTreeNode = new TreeNode.FieldNameTreeNode(identifierToken);
        result.setFieldNameTreeNode(fieldNameTreeNode);
        lexicalAnalysis.nextToken();
        Token literalToken = lexicalAnalysis.token();
        TreeNode.LiteralValueTreeNode literalValueTreeNode = new TreeNode.LiteralValueTreeNode(literalToken);
        result.setLiteralValueTreeNode(literalValueTreeNode);
        return result;
    }

    /**
     * 处理一个短语查询的子树
     * @param identifierToken 属性标记符token
     * @param colonToken 查询冒号token
     * @return
     */
    private TreeNode.MatchPhraseTreeNode processMatchPhraseTree(Token identifierToken, Token colonToken){
        TreeNode.MatchPhraseTreeNode result = new TreeNode.MatchPhraseTreeNode();
        TreeNode.FieldNameTreeNode fieldNameTreeNode = new TreeNode.FieldNameTreeNode(identifierToken);
        result.setFieldNameTreeNode(fieldNameTreeNode);
        lexicalAnalysis.nextToken();
        Token quoteToken = lexicalAnalysis.token();
        TreeNode.PhraseLiteralValueTreeNode phraseLiteralValueTreeNode = collectPhraseLiteralValue(quoteToken);
        result.setPhaseLiteralValueTreeNode(phraseLiteralValueTreeNode);
        return result;
    }

    /**
     * 处理一个带括号语句的属性查询
     * @param identifierToken
     * @param colonToken
     * @return
     */
    private TreeNode processMatchWithBracketTree(Token identifierToken, Token colonToken){
        TreeNode.MatchWithBracketTreeNode result = new TreeNode.MatchWithBracketTreeNode();
        TreeNode.FieldNameTreeNode fieldNameTreeNode = new TreeNode.FieldNameTreeNode(identifierToken);
        result.setFieldNameTreeNode(fieldNameTreeNode);
        lexicalAnalysis.nextToken();
        TreeNode bracketTreeNode = processBracketTree(false,lexicalAnalysis.token());
        if(bracketTreeNode.getClass().equals(TreeNode.BracketTreeNode.class)){
            //validate 借一个queue校验括号内是否是只有字面量与逻辑运算子树
            LinkedList<TreeNode> linkedList = new LinkedList();
            linkedList.addLast(((TreeNode.BracketTreeNode) bracketTreeNode).getInnerNode());
            TreeNode treeNode = null;
            while ((treeNode = linkedList.pollFirst())!=null){
                if (treeNode.getClass().equals(TreeNode.BracketTreeNode.class)){
                    treeNode = ((TreeNode.BracketTreeNode) treeNode).getInnerNode();
                }
                if(treeNode.getClass().equals(TreeNode.LogicCalcTreeNode.class)){
                    if(!((TreeNode.LogicCalcTreeNode) treeNode).getLogicCalcToken().getType().equals(Token.Type.not)){
                        linkedList.addLast(((TreeNode.LogicCalcTreeNode) treeNode).getLeftSubNode().getSubTreeNode());
                    }
                    linkedList.addLast(((TreeNode.LogicCalcTreeNode) treeNode).getRightSubNode().getSubTreeNode());
                }
                else if(!treeNode.getClass().equals(TreeNode.LiteralValueTreeNode.class)&&
                !treeNode.getClass().equals(TreeNode.PhraseLiteralValueTreeNode.class)&&
                        !treeNode.getClass().equals(TreeNode.MultiFieldMatchTreeNode.class)){
                    String errMsg = String.format("带括号语句的属性查询子树，括号内只能是逻辑运算符与字面量，子树[%s]不符合！",
                            treeNode.toSourceStr());
                    throw new DSLSyntaxException(identifierToken,source,errMsg);
                }
            }
            result.setBracketTreeNode((TreeNode.BracketTreeNode)bracketTreeNode);
        }else{
            String errMsg = String.format("属性token右侧期望一个()表达式，实际返回了一个[%s]", result.getClass().getSimpleName());
            throw new DSLSyntaxException(identifierToken, source, errMsg);
        }
        return result;
    }

    /**
     * 处理一个关系查询的子树
     * @param identifierToken
     * @param relationCalcToken
     * @return
     */
    private TreeNode processRelationCalcTree(Token identifierToken, Token relationCalcToken){
        TreeNode.RelationCalcTreeNode result = new TreeNode.RelationCalcTreeNode();
        TreeNode.FieldNameTreeNode fieldNameTreeNode = new TreeNode.FieldNameTreeNode(identifierToken);
        result.setFieldNameTreeNode(fieldNameTreeNode);
        result.setRelationCalcToken(relationCalcToken);
        Token perReadToken = lexicalAnalysis.perReadToken();
        if(perReadToken !=null){
            if(perReadToken.getType().equals(Token.Type.literalValue)){
                lexicalAnalysis.nextToken();
                TreeNode.LiteralValueTreeNode literalValueTreeNode = new TreeNode.LiteralValueTreeNode(lexicalAnalysis.token());
                result.setLiteralValueTreeNode(literalValueTreeNode);
            }else if(perReadToken.getType().equals(Token.Type.quotes)){
                lexicalAnalysis.nextToken();
                TreeNode.PhraseLiteralValueTreeNode phraseLiteralValueTreeNode = collectPhraseLiteralValue(lexicalAnalysis.token());
                result.setPhraseLiteralValueTreeNode(phraseLiteralValueTreeNode);
            }else{
                throw new DSLSyntaxException(relationCalcToken, source, "关系运算符后只能是字面量或\"");
            }
        }else {
            throw new DSLSyntaxException(relationCalcToken, source, "关系运算符后不可为空！！！");
        }
        return result;
    }


    /**
     * 处理一个优先级改变语句子树(括号语句)
     * @param greedLogicToken 匹配完成后是否向后贪婪匹配一个逻辑运算符(and,not)
     * @param lbracketToken 左括号token
     * @return
     */
    private TreeNode processBracketTree(boolean greedLogicToken, Token lbracketToken){
        TreeNode.BracketTreeNode result = new TreeNode.BracketTreeNode();
        result.setLbracketToken(lbracketToken);
        Token perReadToken = lexicalAnalysis.perReadToken();
        if(perReadToken == null){
            throw new DSLSyntaxException(lbracketToken, source, "未找到左括号(对应的右括号");
        }else if(perReadToken.getType().equals(Token.Type.rbracket)){
            // 空括号
            lexicalAnalysis.nextToken();
            result.setInnerStatement(new TreeNode.EmptyTreeNode());
            result.setRbracketToken(lexicalAnalysis.token());
            return result;
        } else{
            lexicalAnalysis.nextToken();
            TreeNode innerTreeNode = getSubTreeNode(greedLogicToken,result,lexicalAnalysis.token());
            perReadToken = lexicalAnalysis.perReadToken();
            if(perReadToken == null){
                throw new DSLSyntaxException(lbracketToken, source, "未找到左括号(对应的右括号");
            }
            while (perReadToken!=null){
                if(perReadToken.getType().equals(Token.Type.rbracket)){
                    result.setInnerStatement(innerTreeNode);
                    lexicalAnalysis.nextToken();
                    result.setRbracketToken(lexicalAnalysis.token());
                    if(greedLogicToken){
                        // [带括号的属性查询语句] 优先级比逻辑运算符高，如果是 [带括号的属性查询语句] 则不再向后探查逻辑运算符
                        perReadToken = lexicalAnalysis.perReadToken();
                        if(perReadToken!=null){
                            if(perReadToken.getType().equals(Token.Type.and)){
                                lexicalAnalysis.nextToken();
                                return processLogicCalcTree(result, lexicalAnalysis.token());
                            }
                        }
                    }
                    break;
                } else{
                    // 括号内有逻辑子树的其他部分
                    lexicalAnalysis.nextToken();
                    innerTreeNode = getSubTreeNode(false,innerTreeNode,lexicalAnalysis.token());
                    perReadToken = lexicalAnalysis.perReadToken();
                    if(perReadToken!=null){
                        continue;
                    }
                    String errMsg = String.format( "左括号期望对应位置的右括号实际为[type:%s,value:%s]",
                            perReadToken.getType().name(),
                            perReadToken.getValue());
                    throw new DSLSyntaxException(lbracketToken, source,errMsg);
                }
            }
            return result;
        }
    }

    /**
     * 处理一个逻辑运算子树
     * @param leftTree 逻辑运算树左侧子树
     * @param logicCalcToken 逻辑运算token
     * @return
     */
    private TreeNode.LogicCalcTreeNode processLogicCalcTree(TreeNode leftTree, Token logicCalcToken){
        TreeNode.LogicCalcTreeNode result = new TreeNode.LogicCalcTreeNode();
        switch (logicCalcToken.getType()){
            case not:
                if(leftTree !=null){
                    if(!leftTree.getClass().equals(TreeNode.BracketTreeNode.class)){
                        throw new DSLSyntaxException(logicCalcToken, source, "逻辑运算符not 不能有左子树");
                    }
                }
                result.setLogicCalcToken(logicCalcToken);
                lexicalAnalysis.nextToken();
                if(lexicalAnalysis.token()==null){
                    throw new DSLSyntaxException(logicCalcToken, source, "not 逻辑运算符后没有token！！！");
                }
                TreeNode rightTree = getSubTreeNode(false,null, lexicalAnalysis.token());
                TreeNode.LogicCalcTreeNode.SubTreeNode rightSubTree = new TreeNode.LogicCalcTreeNode.SubTreeNode();
                rightSubTree.setSubTreeNode(rightTree);
                result.setRightSubNode(rightSubTree);
                break;
            case and:
            case or:
                if(leftTree == null){
                    throw new DSLSyntaxException(logicCalcToken, source, "逻辑运算符[and,or]之前必须有参与运算的左子树！！");
                }
                lexicalAnalysis.nextToken();
                if(lexicalAnalysis.token()==null){
                    throw new DSLSyntaxException(logicCalcToken, source, logicCalcToken.getValue()+"逻辑运算符后没有token！！！");
                }
                rightTree = getSubTreeNode(true,null, lexicalAnalysis.token());
                Token perReadToken = lexicalAnalysis.perReadToken();
                // 逻辑运算符链接
                if(perReadToken!=null){
                    if (perReadToken.getType() == Token.Type.and) {
                        lexicalAnalysis.nextToken();
                        rightTree = processLogicCalcTree(rightTree, lexicalAnalysis.token());
                    }
                }
                rightSubTree = new TreeNode.LogicCalcTreeNode.SubTreeNode();
                rightSubTree.setSubTreeNode(rightTree);
                result.setRightSubNode(rightSubTree);
                TreeNode.LogicCalcTreeNode.SubTreeNode leftSubTree = new TreeNode.LogicCalcTreeNode.SubTreeNode();
                leftSubTree.setSubTreeNode(leftTree);
                result.setLeftSubNode(leftSubTree);
                result.setLogicCalcToken(logicCalcToken);
                break;
            default:
                throw new DSLSyntaxException(logicCalcToken,source,"逻辑运算子树的逻辑运算token应为[and,or,not]其中之一");
        }
        return result;
    }

    /**
     * 处理一个聚合子树
     * @param leftTree 聚合前的过滤子树
     * @param groupToken group token
     * @return
     */
    private TreeNode.AggregationTreeNode processAggregationTree(TreeNode leftTree, Token groupToken){
        TreeNode.AggregationTreeNode aggregationTreeNode = new TreeNode.AggregationTreeNode();
        aggregationTreeNode.setFilterTree(leftTree);
        acceptNextToken(groupToken, Collections.singletonList(Token.Type.by), "[group]后必须跟随[by]");
        lexicalAnalysis.nextToken();
        acceptNextToken(groupToken, Collections.singletonList(Token.Type.lbracket), "[group by]后必须跟随[()]");
        lexicalAnalysis.nextToken();
        TreeNode.BracketTreeNode bracketTree = (TreeNode.BracketTreeNode)processBracketTree(false, lexicalAnalysis.currentToken);
        // (LiteralValue ) 会被解析为 MultiFieldMatchTreeNode 类型的树节点
        if(bracketTree.getInnerNode().getClass().equals(TreeNode.MultiFieldMatchTreeNode.class)){
            TreeNode.MultiFieldMatchTreeNode multiFieldMatchTreeNode = (TreeNode.MultiFieldMatchTreeNode)bracketTree.getInnerNode();
            aggregationTreeNode.setAggregationFiledToken(multiFieldMatchTreeNode.getLiteralValueTreeNode());
        }else{
            String errMsg =String.format("group by()中，by()括号内仅允许为简单字面量字符串！！！,当前值[%s]",bracketTree.getInnerNode().toSourceStr());
            throw new DSLSemanticsException(errMsg);
        }
        acceptNextToken(lexicalAnalysis.currentToken, Arrays.asList(Token.Type.avg,
                Token.Type.stat,
                Token.Type.min,
                Token.Type.max,
                Token.Type.sum,
                Token.Type.count,
                Token.Type.terms), "group by() 必须是一个合法的 聚合函数,目前支持的聚合函数有[avg,stat,min,max,sum,count]");
        lexicalAnalysis.nextToken();
        aggregationTreeNode.setAggregationFunctionToken(lexicalAnalysis.currentToken);
        if(lexicalAnalysis.perReadToken()!=null){
            String errMsg = String.format("group by 聚合语句应该为整个查询语句的最后部分。token[%s]非法！！！",lexicalAnalysis.perReadToken().getValue());
            throw new DSLSemanticsException(errMsg);
        }
        return aggregationTreeNode;
    }

    /**
     * 处理一个限制查询结果集数量子树
     * @param searchStatement 限制条件前的查询表达式树
     * @param limitToken limit token
     * @return
     */
    private TreeNode.LimitResultTreeNode processLimitResultTree(TreeNode searchStatement,Token limitToken){
        TreeNode.LimitResultTreeNode result = new TreeNode.LimitResultTreeNode();
        if(searchStatement == null){
            throw new DSLSemanticsException("limit 表达式前必须有查询条件");
        }
        result.setSearchStatement(searchStatement);
        result.setLimitToken(limitToken);
        acceptNextToken(limitToken,Arrays.asList(Token.Type.identifier,Token.Type.literalValue), "limit 关键字后需要的类型为 identifier或literalValue");
        lexicalAnalysis.nextToken();
        Token limitValueToken = lexicalAnalysis.token();
        try{
            int limitValue = Integer.parseInt(limitValueToken.getValue());
            if(limitValue<=0){
                throw new DSLSyntaxException(limitValueToken,source, "limit 限制的期望值应为一个合法的大于0的int类型的值,当前值为:"+limitValue);
            }
            result.setLimitValueToken(limitValueToken);
        }catch (NumberFormatException e){
            throw new DSLSyntaxException(limitValueToken,source, "limit 限制的期望值应为一个合法的大于0的int类型的值");
        }
        return result;
    }

    /**
     * !!!!! 调整优先级，暂不用此方法，目前通过处理逻辑子树时向右探查AND处理OR不处理，处理到下一次or逻辑来保证and优先级比or高 !!!!!
     * 调整and 与 or的优先级关系并返回调整后的子树
     * @param orLogicTree  or逻辑子树
     * @param rightTreeNode and逻辑子树的右子树
     * @return
     */
    private TreeNode.LogicCalcTreeNode modifyPriorityOfAndOr(TreeNode.LogicCalcTreeNode orLogicTree, Token andToken, TreeNode rightTreeNode){
        if(orLogicTree.getLogicCalcToken().getType().equals(Token.Type.or)){
            TreeNode.LogicCalcTreeNode.SubTreeNode orTreeRightSubTree = orLogicTree.getRightSubNode();
            TreeNode.LogicCalcTreeNode midAndLogicTree = new TreeNode.LogicCalcTreeNode();
            // 调整逻辑关系
            midAndLogicTree.setLeftSubNode(orTreeRightSubTree);
            midAndLogicTree.setLogicCalcToken(andToken);
            midAndLogicTree.setRightSubNode(new TreeNode.LogicCalcTreeNode.SubTreeNode(rightTreeNode));
            orLogicTree.setRightSubNode(new TreeNode.LogicCalcTreeNode.SubTreeNode(midAndLogicTree));
            return orLogicTree;
        }else {
            throw new DSLSyntaxException(andToken,source,"调整and与or的优先级时左子树必须为逻辑为or的逻辑运算子树");
        }
    }

    /**
     * 处理一个多字段匹配子树
     * @param literalValueToken 字面量token
     * @return
     */
    private TreeNode processMultiField(Token literalValueToken){
        TreeNode.MultiFieldMatchTreeNode result = new TreeNode.MultiFieldMatchTreeNode();
        TreeNode.LiteralValueTreeNode literalValueTreeNode = new TreeNode.LiteralValueTreeNode(literalValueToken);
        result.setLiteralValueTreeNode(literalValueTreeNode);
        return result;
    }

    /**
     * 判断下一个token是不是期望的tokens类型中的一个，若不是期望他token类型，则抛出异常
     * @param currentToken 当前token
     * @param acceptTypes 期望的下一个token的类型
     * @param errMsg token类型不匹配时的异常消息
     */
    private void acceptNextToken(Token currentToken,List<Token.Type> acceptTypes,String errMsg){
        Token perReadToken = lexicalAnalysis.perReadToken();
        if(perReadToken!=null){
            if(acceptTypes.stream().noneMatch(t->t.equals(perReadToken.getType()))){
                throw new DSLSyntaxException(currentToken,source,errMsg);
            }
        }else{
            throw new DSLExpectTokenNotExistException(currentToken, source, "已达到token流末尾！！！");
        }
    }

}
