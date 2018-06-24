package com.example.irving.smartfridge;

import android.app.Application;
import android.content.Context;

import com.example.irving.smartfridge.util.Youtu;

public class MyApplication extends Application {
    private static Context mContext;

    // Fridge
    private static final String fridge_id = "fridge1";
    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();
    }

    public static Context getInstance(){
        return mContext;
    }

    public String getFridge_id(){
        return fridge_id;
    }
}
