package prepareData;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.http.HttpHost;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.core.CountRequest;
import org.elasticsearch.client.core.CountResponse;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.rest.RestStatus;

import java.io.IOException;
import java.sql.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class InitEsData {

    static RestHighLevelClient restHighLevelClient;

    static final int maxConn = 20;

    static {
        HttpHost[] httpHosts = new HttpHost[]{new HttpHost("ubuntu", 9200)};
        RestClientBuilder builder = RestClient.builder(httpHosts);
        builder.setHttpClientConfigCallback((config) -> {
            config.setMaxConnTotal(maxConn);
            return config;
        });
        restHighLevelClient = new RestHighLevelClient(builder);
    }

    static final String indexName = "lj";

    public static void main(String[] args) {
        if(!isIndexExists(indexName)){
            createIndex(indexName);
        }
        try {
            int storedSize = storeDataToEs(indexName);
            System.out.println("stored size: "+storedSize);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean createIndex(String indexName){
        CreateIndexRequest request = new CreateIndexRequest(indexName);
        request.settings(Settings.builder().put("number_of_shards",1)
                .put("number_of_replicas",1));
        String mapping ="{\n" +
                "    \"properties\": {\n" +
                "        \"apartment_id\": {\n" +
                "            \"type\": \"long\"\n" +
                "        },\n" +
                "        \"main_desc\": {\n" +
                "            \"type\": \"text\"\n" +
                "        },\n" +
                "        \"watch_count\": {\n" +
                "            \"type\": \"integer\"\n" +
                "        },\n" +
                "        \"price\": {\n" +
                "            \"type\": \"integer\"\n" +
                "        },\n" +
                "        \"unit_price\": {\n" +
                "            \"type\": \"integer\"\n" +
                "        },\n" +
                "        \"room_desc\": {\n" +
                "            \"type\": \"keyword\"\n" +
                "        },\n" +
                "        \"room_direction\": {\n" +
                "            \"type\": \"keyword\"\n" +
                "        },\n" +
                "        \"build_area\": {\n" +
                "            \"type\": \"float\"\n" +
                "        },\n" +
                "        \"build_type_desc\": {\n" +
                "            \"type\": \"keyword\"\n" +
                "        },\n" +
                "        \"apartment_name\": {\n" +
                "            \"type\": \"text\"\n" +
                "        },\n" +
                "        \"community_name\": {\n" +
                "            \"type\": \"text\"\n" +
                "        },\n" +
                "        \"seal_start_time\": {\n" +
                "            \"type\": \"date\",\n" +
                "            \"format\": \"yyyy-MM-dd HH:mm:ss||yyyy-MM-dd||epoch_millis\"\n" +
                "        },\n" +
                "        \"last_trade_time\": {\n" +
                "            \"type\": \"date\",\n" +
                "            \"format\": \"yyyy-MM-dd HH:mm:ss||yyyy-MM-dd||epoch_millis\"\n" +
                "        },\n" +
                "        \"down_payment\": {\n" +
                "            \"type\": \"float\"\n" +
                "        },\n" +
                "        \"monthly_supply\": {\n" +
                "            \"type\": \"integer\"\n" +
                "        },\n" +
                "        \"apartment_city\": {\n" +
                "            \"type\": \"keyword\"\n" +
                "        },\n" +
                "        \"update_time\": {\n" +
                "            \"type\": \"date\",\n" +
                "            \"format\": \"yyyy-MM-dd HH:mm:ss||yyyy-MM-dd||epoch_millis\"\n" +
                "        },\n" +
                "        \"update_time_inner\":{\n" +
                "            \"type\":\"nested\",\n" +
                "            \"properties\":{\n" +
                "                \"year\":{\n" +
                "                    \"type\": \"integer\"\n" +
                "                },\n" +
                "                \"month\":{\n" +
                "                    \"type\": \"integer\"\n" +
                "                },\n" +
                "                \"day\":{\n" +
                "                    \"type\": \"integer\"\n" +
                "                }\n" +
                "            }\n" +
                "        }\n" +
                "    }\n" +
                "}";
        request.mapping(mapping, XContentType.JSON);
        try {
            CreateIndexResponse response = getClient().indices().create(request,RequestOptions.DEFAULT);
            return response.isAcknowledged();
        } catch (IOException e) {
             throw new RuntimeException(e);
        }
    }

    public static boolean isIndexExists(String indexName){
        GetIndexRequest getIndexRequest = new GetIndexRequest(indexName);
        getIndexRequest.local(true);
        getIndexRequest.humanReadable(true);
        try {
            return getClient().indices().exists(getIndexRequest,RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static int storeDataToEs(String indexName) throws ClassNotFoundException, SQLException, IOException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection connection = DriverManager.getConnection("jdbc:mysql://router.beijing.wkiki.cn:3306/home","home","112358?@");
        PreparedStatement statement = connection.prepareStatement("select * from lianjia_data");
        ResultSet resultSet = statement.executeQuery();
        AtomicInteger count = new AtomicInteger();
        Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd hh:mm:ss").create();
        ThreadPoolExecutor executor = new ThreadPoolExecutor(4, 20,
                60, TimeUnit.SECONDS,new LinkedBlockingQueue<>(10),new ThreadPoolExecutor.CallerRunsPolicy());
        reportIndexCount();
        while (resultSet.next()){
            Map<String,Object> record = new HashMap<>();
            record.put("apartment_id",resultSet.getLong("apartment_id"));
            record.put("main_desc",resultSet.getString("main_desc"));
            record.put("watch_count",resultSet.getInt("watch_count"));
            record.put("price",resultSet.getInt("price"));
            record.put("unit_price",resultSet.getInt("unit_price"));
            record.put("room_desc",resultSet.getString("room_desc"));
            record.put("room_direction",resultSet.getString("room_direction"));
            record.put("build_area",resultSet.getFloat("build_area"));
            record.put("build_type_desc",resultSet.getString("build_type_desc"));
            record.put("apartment_name",resultSet.getString("apartment_name"));
            record.put("community_name",resultSet.getString("community_name"));
            java.util.Date sealStartTime = resultSet.getDate("seal_start_time");
            java.util.Date lastTradeTime = resultSet.getDate("last_trade_time");
            record.put("seal_start_time",sealStartTime==null?null:new Date(sealStartTime.getTime()));
            record.put("last_trade_time",lastTradeTime==null?null:new Date(lastTradeTime.getTime()));
            record.put("down_payment",resultSet.getFloat("down_payment"));
            record.put("monthly_supply",resultSet.getInt("monthly_supply"));
            record.put("apartment_city",resultSet.getString("apartment_city"));
            record.put("update_time",new java.util.Date(resultSet.getDate("update_time").getTime()));
            Map<String,Integer> nestedObject = new HashMap<>();
            LocalDateTime updateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(resultSet.getDate("update_time").getTime()), TimeZone.getDefault().toZoneId());
            nestedObject.put("year",  updateTime.getYear());
            nestedObject.put("month", updateTime.getMonthValue());
            nestedObject.put("day", updateTime.getDayOfMonth());
            record.put("update_time_inner",nestedObject);
            executor.execute(()->{
                IndexRequest request = new IndexRequest(indexName);
                request.source(gson.toJson(record),XContentType.JSON);
                IndexResponse response = null;
                try {
                    response = getClient().index(request, RequestOptions.DEFAULT);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                if(response.status() == RestStatus.CREATED){
                    count.incrementAndGet();
                }
            });
        }
        return count.get();
    }

    private static void reportIndexCount(){
        Thread reportThread = new Thread(()->{
            CountRequest countRequest = new CountRequest(indexName);
            while (true) {
                try {
                    CountResponse countResponse = getClient().count(countRequest, RequestOptions.DEFAULT);
                    long count = countResponse.getCount();
                    System.out.printf("current index doc [%d]\n", count);
                    Thread.sleep(1000);
                } catch (IOException e) {
                    System.out.printf("请求count时发生IO异常[%s]\n", e.getMessage());
                } catch (InterruptedException e) {
                    // ignore
                }
            }
        });
        reportThread.start();
    }

    public static RestHighLevelClient getClient() {
        return restHighLevelClient;
    }
}
