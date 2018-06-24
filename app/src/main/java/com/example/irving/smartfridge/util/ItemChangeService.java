package com.example.irving.smartfridge.util;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ItemChangeService {
    private static final String put_url = "/fridge/userAction/increaseItem";
    private static final String take_url = "/fridge/userAction/decreaseItem";

    public static String putRequest(String item, String fridge_id){
        String url = put_url +
                "?fridgeId=" + fridge_id +
                "&itemName=" + item;
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
