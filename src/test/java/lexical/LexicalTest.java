package lexical;

import cn.wkiki.kql.LexicalAnalysis;
import cn.wkiki.kql.Token;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class LexicalTest {

    /**
     * 测试匹配到 limit 关键字
     */
    @Test
    public void testLimitIdentify(){
        String statement = "apartment_city:北京 limit 10";
        LexicalAnalysis lexicalAnalysis = new LexicalAnalysis(statement);
        lexicalAnalysis.nextToken();
        boolean matched = false;
        while (lexicalAnalysis.token() !=null){
            System.out.println(lexicalAnalysis.token());
            if(lexicalAnalysis.token().getType().equals(Token.Type.limit)){
                matched = true;
            }
            lexicalAnalysis.nextToken();
        }
        assert matched;
    }

    /**
     * 测试匹配到 order 关键字
     */
    @Test
    public void testOrderIdentify(){
        String statement = "apartment_city:北京 limit 10 order by(unit_price desc)";
        LexicalAnalysis lexicalAnalysis = new LexicalAnalysis(statement);
        lexicalAnalysis.nextToken();
        boolean matched = false;
        while (lexicalAnalysis.token() !=null){
            System.out.println(lexicalAnalysis.token());
            if(lexicalAnalysis.token().getType().equals(Token.Type.order)){
                matched = true;
            }
            lexicalAnalysis.nextToken();
        }
        assert matched;
    }

    @Test
    public void testMatchLastKeyWord(){
        String[] statements = new String[]{"a:b and","a:b and c:or"};
        boolean matchedAnd = false,matchedOr = false;
        for (String statement : statements) {
            LexicalAnalysis lexicalAnalysis = new LexicalAnalysis(statement);
            lexicalAnalysis.nextToken();
            while (lexicalAnalysis.token() !=null){
                System.out.println(lexicalAnalysis.token());
                if(lexicalAnalysis.token().getType().equals(Token.Type.and)){
                    matchedAnd = true;
                }
                if(lexicalAnalysis.token().getType().equals(Token.Type.or)){
                    matchedOr = true;
                }
                lexicalAnalysis.nextToken();
            }
        }
        assert matchedAnd;
        assert matchedOr;
    }
}
