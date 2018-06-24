package com.example.irving.smartfridge.util;

import org.json.JSONObject;

import java.io.IOException;
import java.util.StringJoiner;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ItemChangeService {
    private static final String put_url = "http://120.78.218.52:8080/fridge/fridgeAction/increaseItem";
    private static final String take_url = "http://120.78.218.52:8080/fridge/fridgeAction/decreaseItem";

    public static String putRequest(String item, String fridge_id){
        String url = put_url +
                "?fridgeId=" + fridge_id.substring(6) +
                "&itemName=" + item;
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .build();
        Call call = client.newCall(request);
        try{
            Response response = call.execute();
            String result = response.body().string();
            JSONObject json = new JSONObject(result);
            return (String)json.get("result");
        }catch (Exception e){
            e.printStackTrace();
        }
        return "fail";

    }

    public static String takeRequest(String item, String fridge_id, String user_id){
        String url = take_url +
                "?fridgeId=" + fridge_id +
                "&itemName=" + item +
                "&userId=" + user_id;
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .build();
        Call call = client.newCall(request);
        try{
            Response response = call.execute();
            String result = response.body().string();
            return result;
        }catch (IOException e){
            e.printStackTrace();
        }
        return "fail";
    }
}
