package cn.wkiki.kql.util;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class GsonUtil {

    private static Gson gson = new GsonBuilder().create();

    private static Gson gsonWithPretty = new GsonBuilder().setPrettyPrinting().create();

    public static Gson getInstance(){
        return gson;
    }

    public static Gson getInstanceWithPretty(){
        return gsonWithPretty;
    }
}
