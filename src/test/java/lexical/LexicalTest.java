package lexical;

import cn.wkiki.kql.LexicalAnalysis;
import cn.wkiki.kql.Token;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import javax.swing.border.AbstractBorder;

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

    @Test
    public void testOrderIdentify(){
        String statement = "apartment_city:北京 limit 10 order by(unit_price desc)";
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
}
