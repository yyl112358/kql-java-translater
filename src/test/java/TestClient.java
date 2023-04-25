import cn.wkiki.kql.Parser;
import cn.wkiki.kql.Token;
import cn.wkiki.kql.TranslateContext;
import cn.wkiki.kql.exception.DSLSemanticsException;
import cn.wkiki.kql.model.QueryModel;
import cn.wkiki.kql.queryUnit.QueryUnit;
import cn.wkiki.kql.tree.TreeNode;
import cn.wkiki.kql.util.GsonUtil;
import com.google.gson.reflect.TypeToken;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

public class TestClient {


    public static void main(String[] args) throws IOException {
        RestHighLevelClient client = getClient();
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String prompt ="P>>> ";
        while (true){
            System.out.printf(prompt);
            String readLine = reader.readLine().trim();
            if (readLine.equals("exit();")) {
                System.out.println("good bye!");
                break;
            }
            try{
                Parser parser = new Parser(readLine);
                TreeNode astRootNode = parser.getAST();
                QueryUnit queryUnit = astRootNode.toTranslateUnit(null);
                String esQueryEntity = queryUnit.toESQueryJsonEntity();
                Request request = new Request("GET","/lj/_search");
                QueryModel queryModel = new QueryModel();
                if(astRootNode!=null&&astRootNode.getClass().equals(TreeNode.AggregationTreeNode.class)){
                    processQueryLangAggs(queryModel, (TreeNode.AggregationTreeNode) astRootNode,null);
                }
                queryModel.setQuery(GsonUtil.getInstance().fromJson(esQueryEntity,new TypeToken<HashMap<String,Object>>(){}.getType()));
                request.setJsonEntity(GsonUtil.getInstance().toJson(queryModel));
                Response response = client.getLowLevelClient().performRequest(request);
                String responseEntity = EntityUtils.toString(response.getEntity());
                HashMap<String,Object> responseObj = GsonUtil.getInstance().fromJson(responseEntity, new TypeToken<HashMap<String,Object>>(){}.getType());
                System.out.println(GsonUtil.getInstanceWithPretty().toJson(responseObj));
            }catch (Exception e){
                System.out.println(e.getMessage());
            }
        }
        System.exit(0);
    }

    private static void processQueryLangAggs(QueryModel translateParam,
                                             TreeNode.AggregationTreeNode aggregationTreeNode,
                                      TranslateContext translateContext){
        Token aggFunctionToken = aggregationTreeNode.getAggregationFunctionToken();
        String aggFieldName  = aggregationTreeNode.getLiteralValueTreeNode().getLiteralValueToken().getValue();
        if(translateContext!=null && translateContext.getFields().stream().noneMatch(t->t.getName().equals(aggFieldName))){
            String errMsg =String.format("要聚合的字段[%s]不在索引的属性列表中", aggFieldName);
            throw new DSLSemanticsException(errMsg);
        }
        if(translateParam.getAggs() == null){
            translateParam.setAggs(new HashMap<>());
        }
        String aggName ="";
        Map<String, Map<String,String>> aggBody = new HashMap();
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
        translateParam.getAggs().put(aggName, aggBody);
    }

    public static RestHighLevelClient getClient() {
        String instance1 = "localhost:9200";
        HttpHost[] clusters = new HttpHost[]{
                new HttpHost(instance1.split(":")[0], Integer.parseInt(instance1.split(":")[1]))
        };
        RestClientBuilder builder = RestClient.builder(clusters);
        /**
         * 异步httpclient的连接延时配置
         */
        builder.setRequestConfigCallback(requestConfigBuilder -> {
            requestConfigBuilder.setConnectTimeout(10000);
            requestConfigBuilder.setSocketTimeout(60000);
            requestConfigBuilder.setConnectionRequestTimeout(60000);
            return requestConfigBuilder;
        });
        /**
         * 异步httpclient的连接数配置
         * 和basic验证
         */
        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        return new RestHighLevelClient(builder);
    }
}
