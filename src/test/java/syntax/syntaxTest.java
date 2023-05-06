package syntax;

import cn.wkiki.kql.Parser;
import cn.wkiki.kql.tree.TreeNode;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

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

    @Test
    public void testIdentifyOrderStatement(){
        String[] statements = new String[]{
                    "apartment_city:北京 order by(monthly_supply asc) limit 5",
                    "apartment_city:北京 order by(monthly_supply asc",
                    "apartment_city:北京 order by(asc) limit 5",
                    "apartment_city:北京 order by(monthly_supply) limit 5",
                    "apartment_city:北京 order by() limit 5",
                    "apartment_city:北京 order by( ",
                    "apartment_city:北京 order by monthly_supply asc)",
                    "order by(monthly_supply asc)",
                    "() order by(monthly_supply asc)"
                };
        Exception[] exceptions = new Exception[statements.length];
        List<TreeNode> treeNodes = new ArrayList<>(10);
        for (int i = 0; i < statements.length; i++) {
            String statement = statements[i];
            Parser parser = new Parser(statement);
            try{
                TreeNode rootNode = parser.getAST();
                if(rootNode !=null){
                    treeNodes.add(rootNode);
                }
            }catch (Exception e){
                exceptions[i] = e;
            }
        }
        System.out.printf("identify success count [%d]\n,exception count [%d]\n",treeNodes.size(),exceptions.length);
        System.out.println("---------success---------");
        for (TreeNode node : treeNodes) {
            System.out.println(node.toSourceStr());
        }
        System.out.println("\n---------exception---------\n");
        for (int i = 0; i < exceptions.length; i++) {
            Exception exception = exceptions[i];
            if(exception != null){
                System.out.println("source ---> "+statements[i]);
                System.out.println("exception ---> "+exceptions[i]);
                System.out.println("-------------------------------------");
            }
        }
        assert treeNodes.size() == 2;
        assert Arrays.stream(exceptions).filter(Objects::nonNull).count() == statements.length - treeNodes.size();
    }
}
