package translate;

import cn.wkiki.kql.Parser;
import cn.wkiki.kql.queryUnit.QueryUnit;
import cn.wkiki.kql.tree.TreeNode;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@RunWith(JUnit4.class)
public class TranslateTest {

    @Test
    public void testNestedMatchTranslate(){
        String[] statements = new String[]{
                "update_time_inner:{month:5}",
                "update_time_inner:{month:5 and day>1}",
                "update_time_inner:{(month:5 and day>1)}",
                "update_time_inner:{month:5 and day>1 or test:\"hello world\"}",
                "update_time_inner:{month:5 and day>1 and test:{q:fdsf}}"
        };
        Exception[] exceptions = new Exception[statements.length];
        List<QueryUnit> queryUnits = new ArrayList<>();
        for (int i = 0; i < statements.length; i++) {
            String statement = statements[i];
            try{
                Parser parser = new Parser(statement);
                TreeNode treeNode = parser.getAST();
                QueryUnit queryUnit = treeNode.toTranslateUnit(null);
                queryUnits.add(queryUnit);
            }catch (Exception e){
                exceptions[i] = e;
            }
        }
        System.out.printf("translate success count [%d]\n,exception count [%d]\n",queryUnits.size(),exceptions.length);
        System.out.println("---------success---------");
        for (QueryUnit queryUnit : queryUnits) {
            System.out.println(queryUnit.prettyToESQueryJsonEntity());
            System.out.println();
            System.out.println("-------------------------------------");
            System.out.println();
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

        assert queryUnits.size() == 4;
        assert Arrays.stream(exceptions).filter(Objects::nonNull).count() == statements.length - queryUnits.size();
    }
}
