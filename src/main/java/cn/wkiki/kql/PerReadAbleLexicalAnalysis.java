package cn.wkiki.kql;

import org.apache.commons.collections4.CollectionUtils;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 可预读的
 */
public class PerReadAbleLexicalAnalysis extends LexicalAnalysis{

    LinkedList<Token> tokensBuffer= new LinkedList<>();

    public PerReadAbleLexicalAnalysis(String source) {
        super(source);
    }

    @Override
    public void nextToken() {
        if(tokensBuffer.size()>0){
            // buffer中有从buffer中直接读取
            pervToken = token();
            currentToken = tokensBuffer.poll();
        }else{
            super.nextToken();
        }
        //词法分析部分对 identifier 的确定比较宽松，此处重新确定一个单词是否是一个属性名
        if(currentToken !=null && currentToken.getType().equals(Token.Type.identifier)){
            Token perReadToken = perReadToken();
            if(perReadToken == null){
                currentToken.setType(Token.Type.literalValue);
            }else{
                switch (perReadToken.getType()){
                    case colon:
                    case gt:
                    case gte:
                    case lt:
                    case lte:
                        break;
                    default:
                        currentToken.setType(Token.Type.literalValue);
                }
            }
        }
    }

    /**
     * 预读指定个token，但不移动当前消费到的token的位置,此操作不影响pervToken的值，pervToken仍为预读前的值
     * @param perReadSize 预读的个数
     * @return 预读个数的列表，若token流中剩余的数量已不满足期望的数量，则只返回当前位置到token流结尾中间的token
     */
    public List<Token> perReadToken(int perReadSize){
        if(tokensBuffer.size()<perReadSize){
            int missSize = perReadSize - tokensBuffer.size();
            // 保护现场
            Token oldPervToken = pervToken;
            Token oldCurrentToken = currentToken;
            while (missSize>0){
                super.nextToken();
                if(token()!=null){
                    tokensBuffer.addLast(token());
                }else{
                    break;
                }
                missSize--;
            }
            // 恢复现场
            pervToken = oldPervToken;
            currentToken = oldCurrentToken;
        }
        return tokensBuffer.stream().limit(perReadSize).map(Token::copySelf).collect(Collectors.toList());
    }

    /**
     * 向后预读一个token ，但不移动当前消费到的token的位置,此操作不影响pervToken的值，pervToken仍为预读前的值
     * @return 预读到的token或null(token流已空)
     */
    public Token perReadToken(){
        List<Token> perReadTokens = perReadToken(1);
        if(CollectionUtils.isNotEmpty(perReadTokens)){
            return perReadTokens.get(0);
        }
        return null;
    }
}
