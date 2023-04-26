import cn.wkiki.kql.Parser;
import cn.wkiki.kql.model.DetailQueryParamBody;
import cn.wkiki.kql.tree.TreeNode;
import cn.wkiki.kql.util.GsonUtil;
import com.google.gson.reflect.TypeToken;
import org.apache.http.HttpHost;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;

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
                DetailQueryParamBody queryParamBody = astRootNode.toQueryParamBody(null);
                Request request = new Request("GET", "/lj/_search");
                request.setJsonEntity(GsonUtil.getInstance().toJson(queryParamBody));
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
