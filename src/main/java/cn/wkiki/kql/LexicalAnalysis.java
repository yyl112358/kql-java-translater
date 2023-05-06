package cn.wkiki.kql;


import cn.wkiki.kql.exception.CharacterOutOfRangeException;
import cn.wkiki.kql.exception.NoMatchTokenTypeException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Optional;

/**
 * 词法分析器
 */
@Slf4j
public class LexicalAnalysis {
    /**
     * 源查询语句
     */
    char[] source;

    int tokenBufferSize = 256;

    int maxTokenSize = 2048;
    /**
     * 生成token时的buffer
     */
    char[] tokenBuf =new char[tokenBufferSize];

    Token currentToken;

    /**
     * 上一个token
     */
    Token pervToken;

    // 源字符串即将要消费位置
    int sp=0;
    // tokenBuffer当前位置
    int tp=0;

    public LexicalAnalysis(String source){
        this.source = source.toCharArray();
    }

    /**
     * 消费下一个token，调用此方法后。pervToken将被当前token替代
     */
    public void nextToken(){
        tp = 0;
        pervToken = currentToken;
        currentToken = null;
        loop: while (true){
            Character ch = getChar(),nextChar;
            if(ch !=null){
                switch (ch){
                    case ' ':
                    case '\t':
                    case '\f':
                        do {
                            ch = getChar();
                            if(ch == null){
                                break loop;
                            }
                        } while (ch == ' ' || ch == '\t' || ch == '\f');
                        spitChar(1);
                        break;
                    case ':':
                    case '"':
                    case '(':
                    case ')':
                    case '{':
                    case '}':
                        currentToken = processSingleCharKey(ch);
                        break loop;
                    case '\\':
                        nextChar = getChar();
                        // 只有\ 后续跟着"或\时才处理为strEscape token 否则当作字面量处理
                        if(nextChar !=null && (nextChar == '\"'||nextChar == '\\')){
                            spitChar(1);
                            currentToken =  processSingleCharKey(ch);
                        }else{
                            processIdentifier(ch, sp - 1);
                        }
                        break loop;
                    case '<':
                    case '>':
                        nextChar = perReadChar(0); // 预读当前位
                        if(nextChar!=null && nextChar.equals('=')){
                            currentToken = processTwoCharKey(ch);
                        }else{
                            currentToken = processSingleCharKey(ch);
                        }
                        break loop;
                    default:
                        currentToken = processIdentifier(ch,sp-1);
                        break loop;
                }
            }else{
                // eof
                break;
            }
        }
    }

    /**
     * 获取当前的token
     * @return lexical 当前正在处理的token
     */
    public Token token(){
        return currentToken;
    }

    /**
     * 获取上一个token
     * @return lexical 处理的上一个token
     */
    public Token pervToken(){
        return pervToken;
    }

    /**
     * 消费一个字符,并移动当前的sourcePosition位置
     * @return 当前正消费的字符
     */
    private Character getChar(){
        if(sp<source.length){
            return source[sp++];
        }
        return null;
    }

    /**
     * 消费一个相对于当前位置的字符，但不移动当前的sourcePosition
     * @param relatePosition 消费一个相对当前位置的位置值 (大于0是向后消费；小于0时向前消费，但绝对值不能小于当前所在的位置)
     * @return 当前正消费的字符
     */
    private Character perReadChar(int relatePosition){
        if(relatePosition >0){
            int targetIndex =relatePosition+sp;
            if(targetIndex>source.length){
                String errMsg = String.format("期望消费源码的位置[%d]超过了源码的最大长度[%d]", targetIndex,source.length);
                throw new CharacterOutOfRangeException(targetIndex, source.length, errMsg);
            }
            return source[targetIndex];
        }else if(relatePosition <0){
            int targetIndex = sp - Math.abs(relatePosition);
            if(targetIndex<0){
                String errMsg = String.format("期望消费源码的位置[%d]小于0,当前sp位置[%d],期望额相对位置[%d]", targetIndex,sp,relatePosition);
                throw new CharacterOutOfRangeException(targetIndex, source.length, errMsg);
            }
            return source[targetIndex];
        }else{
            if(sp>=source.length){
                return null;
            }
            return source[sp];
        }
    }

    /**
     * 处理单字符关键字
     * @param ch 判断为关键字的 单个字符
     * @return 单个关键字的token token类型为字符对应的关键字
     */
    private Token processSingleCharKey(char ch){
        currentToken = new Token();
        currentToken.setStartIndex(sp);
        currentToken.setValue(String.valueOf(ch));
        Token.Type matchedType = Token.Type.isLangKeyWord(ch);
        if(matchedType == null){
            throw new NoMatchTokenTypeException(String.valueOf(ch));
        }else{
            currentToken.setType(matchedType);
        }
        return token();
    }

    /**
     * 处理两个字符的关键字，注意此时，lexical 应该仅消费到了关键字对应的第一个字符。<br/>
     * in other word lexical的下一个要消费的字符应该是双字符关键字的第二个字符。
     * @param ch 两个字符关键字的第一个字符。
     * @return 两个字符的关键字类型Token
     */
    private Token processTwoCharKey(char ch){
        Character secondChar = getChar();
        currentToken = new Token();
        currentToken.setStartIndex(sp);
        currentToken.setValue(String.valueOf(new char[]{ch,secondChar}));
        Token.Type matchedType = Token.Type.isLangKeyWord(currentToken.getValue());
        if(matchedType == null){
            throw new NoMatchTokenTypeException(String.valueOf(ch));
        }else{
            currentToken.setType(matchedType);
        }
        return token();
    }

    /**
     * 处理标示符 与 字面量
     * @param firstCh 标示符或字面量的第一个字符
     * @return 一个标示符或字面量Token
     */
    private Token processIdentifier(char firstCh,int startIndex){
        putTkBufChar(firstCh);
        loop: while (true) {
            Character character = getChar(),nextChar;
            if(character == null){
                break ;
            }
            switch (character) {
                case ' ':
                case '\t':
                case '\f':
                    // 语言中字符串常量可以 不以"或'包裹 意味着关键字与字符串常量之间仅有空格隔断，扫描过程中需要判断读进来的连续非空格的字符串是否为关键字
                    // 此标记用来标记每碰到一个空格或类似字符时，前面已读的部分是否有空格出现。如果有空格且最后一个连续的非空格字符串为关键字
                    // 则需要吐出已消费的对应的长度后，将前面部分收集为字符串常量。关键字token部分则留给下一次消费
                    // 简而言之 -> 在扫描 identifier时 每个 空格都要停下来检视是否有关键字被消费了
                    Optional<KeyInIdentifierResult> matchKeyWord= matchKeywordInIdentifier();
                    if(matchKeyWord.isPresent()){ // 最后一段字符可以匹配关键字
                        if(matchKeyWord.get().isMatchedAllText()){
                            // 当前所有字符既是关键字
                            Token result = new Token();
                            result.setValue(tokenBuffAsString());
                            resetTkBuff();
                            result.setType(matchKeyWord.get().getMatchKeyType());
                            result.setStartIndex(startIndex);
                            return result;
                        }else{
                            //最后一段关键字前有其他字符串，修改sp吐出对应长度的字符后修改tp位置，退出循环
                            spitChar(matchKeyWord.get().getMatchKeyType().value.length()+1);
                            tp-=matchKeyWord.get().getMatchKeyType().value.length();
                            break loop;
                        }
                    }else {
                        // 继续消费
                        putTkBufChar(character);
                        break;
                    }
                case ':':
                case '"':
                case ')':
                case '{':
                case '}':
                case '<':
                case '>':
                    spitChar(1);
                    break loop;
                case '\\':
                    nextChar = getChar();
                    // 只有\ 后续跟着"或\时才处理为strEscape token 否则当作字面量处理
                    if(nextChar !=null && (nextChar == '\"'||nextChar == '\\')){
                        spitChar(2);
                        break loop;
                    }else{
                        putTkBufChar(character);
                    }
                    break;
                case '(':
                    nextChar = getChar();
                    if(nextChar == ')'){
                        String tryMatchKeyStr = tokenBuffAsString()+"()";
                        if(Token.Type.isLangKeyWord(tryMatchKeyStr)!=null){
                            putTkBufChar('(');
                            putTkBufChar(')');
                            break loop;
                        }
                    }
                    //将读到的 '()'吐回去
                    spitChar(2);
                    break loop;
                default:
                    putTkBufChar(character);
            }
        }
        // 将 identifier 中最后一个可能为关键字的串 识别为关键字
        // 类似  a:b and | a:b and c:or中 最后的一个and 和 or 识别为关键字而非字面量 ！！！！！！
        Optional<KeyInIdentifierResult> matchKeyWord= matchKeywordInIdentifier();
        Token result =null;
        if(matchKeyWord.isPresent()){ // 最后一段字符可以匹配关键字
            if(matchKeyWord.get().isMatchedAllText()){
                // 当前所有字符既是关键字
                result = new Token();
                result.setValue(tokenBuffAsString());
                resetTkBuff();
                result.setType(matchKeyWord.get().getMatchKeyType());
                result.setStartIndex(startIndex);
                return result;
            }else{
                //最后一段关键字前有其他字符串，修改sp吐出对应长度的字符后修改tp位置，退出循环
                spitChar(matchKeyWord.get().getMatchKeyType().value.length()+1);
                tp-=matchKeyWord.get().getMatchKeyType().value.length();
            }
        }
        result = new Token();
        result.setStartIndex(startIndex);
        result.setValue(tokenBuffAsString().trim());// 移除前后两侧无用的空格
        resetTkBuff();
        result.setType(Token.Type.identifier);
        Token pervToken = pervToken();
        if(pervToken !=null && (pervToken.getType() == Token.Type.colon
                || pervToken.getType() == Token.Type.quotes
                || pervToken.getType() == Token.Type.lt
                || pervToken.getType() == Token.Type.gt
                || pervToken.getType() == Token.Type.lte
                || pervToken.getType() == Token.Type.gte)){
            // 上述几种情况提前加工为字面量，但不说明不符合上面情况下此标示符不是字面量
            result.setType(Token.Type.literalValue);
            result.setValue(result.getValue().trim()); // 移除前后两侧无用的空格
        }
        return result;
    }

    /**
     * 尝试从当前的tokenBuffer匹配最后的部分是否为一个关键字
     * @return 匹配到Keyword 类型的Type及标记buffer中是否均为keyword 或 null（当前 token buffer中的所有内容均为 identifier）
     */
    private Optional<KeyInIdentifierResult> matchKeywordInIdentifier(){
        boolean matchLastWhiteChar = false;
        char[] lastIdentifierPart = null;
        for (int i = tp-1;i>=0;i--){
            if (tokenBuf[i] == ' '||tokenBuf[i]=='\t'||tokenBuf[i]=='\f'){
                matchLastWhiteChar = true;
                lastIdentifierPart = Arrays.copyOfRange(tokenBuf, i+1, tp);
                break;
            }
        }
        if(lastIdentifierPart == null){
            lastIdentifierPart = Arrays.copyOf(tokenBuf, tp);
        }
        Token.Type lastPartMatchedKey = Token.Type.isLangKeyWord(String.valueOf(lastIdentifierPart));
        KeyInIdentifierResult result = null;
        if(lastPartMatchedKey!=null){
            if(!matchLastWhiteChar){
                result = new KeyInIdentifierResult(lastPartMatchedKey,true);
            }else{
                result = new KeyInIdentifierResult(lastPartMatchedKey,false);
            }
        }
        return Optional.ofNullable(result);
    }

    /**
     * 尝试从 identifier 匹配Keyword时的结果
     */
    @Getter
    private class KeyInIdentifierResult{

        protected KeyInIdentifierResult(Token.Type matchKeyType,boolean matchedAllText){
            this.matchKeyType = matchKeyType;
            this.matchedAllText = matchedAllText;
        }

        Token.Type matchKeyType;

        boolean matchedAllText;
    }

    /**
     * 将一个字符追到到tokenBuffer中
     * @param ch 要追加的字符
     */
    private void putTkBufChar(char ch){
        ensureTkBuffSize();
        if(tp< tokenBufferSize - 1){
            tokenBuf[tp++] = ch;
        }else{
            String errMsg = String.format("单个token最大长度允许为[%d],当前token[%s]已达到最大长度，无法读取下一个字符！！！", tokenBufferSize, tokenBuffAsString());
            throw new RuntimeException(errMsg);
        }
    }

    private void ensureTkBuffSize(){
        if(tp == tokenBufferSize - 1){
            //full
            int newSize = new Float(tokenBufferSize * 1.5).intValue();
            newSize = Math.min(newSize, maxTokenSize);
            char[] newTokenBuff = Arrays.copyOf(tokenBuf, newSize);
            synchronized (this){
                tokenBuf = newTokenBuff;
                tokenBufferSize = newSize;
            }
        }
    }

    /**
     * 读取token buffer中的字符，返回新的字符串引用
     * @return token buffer中存放本次读取token的 字符串 或 空字符(当前token扫描未在buffer中存放任何字符)
     */
    private String tokenBuffAsString(){
        if(tp>0){
            char[] chars = Arrays.copyOf(tokenBuf,tp);
            return String.valueOf(chars);
        }
        return "";
    }

    /**
     * 重置tokenBuffer
     */
    private void resetTkBuff(){
        tp = 0;
    }

    /**
     * 吐出指定个已经消费的字符(将sp向前退指定位)
     * @param len 回退的指定长度
     */
    private void spitChar(int len){
        if(len <=0){
            throw new RuntimeException("吐出字符串时len不能为0");
        }
        if(sp-len>0){
            sp-=len;
        }else{
            String errMsg = String.format("当前sp位置[%d]，期望回退[%d]，回退后位置小于等于0", sp,len);
            throw new RuntimeException(errMsg);
        }
    }
}
