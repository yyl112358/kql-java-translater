package syntax;

import cn.wkiki.kql.Parser;
import cn.wkiki.kql.tree.TreeNode;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.List;

@RunWith(JUnit4.class)
public class syntaxTest {

    @Test
    public void testIdentifyLimitSyntax(){
        String statement = "apartment_city:北京 limit 50";
        String errStatement = "apartment_city:北京 limit NaN";
        String errStatement2 = "apartment_city:北京 limit ";
        String errStatement3 = "limit 50";
        String[] statements = new String[]{statement,errStatement,errStatement2,errStatement3};
        List<Exception> exceptions = new ArrayList<>(3);
        TreeNode successNode =null;
        for (String s : statements) {
            Parser parser = new Parser(s);
            try{
                TreeNode rootNode = parser.getAST();
                if(rootNode !=null){
                    successNode = rootNode;
                }
            }catch (Exception e){
                exceptions.add(e);
            }
        }
        Assert.assertNotNull(successNode);
        System.out.println(successNode.toSourceStr());
        for (Exception exception : exceptions) {
            System.out.println(exception.getMessage());
            System.out.println();
        }
        Assert.assertEquals(exceptions.size(), 3);
    }
}
