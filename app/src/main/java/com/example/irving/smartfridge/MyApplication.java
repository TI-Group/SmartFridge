package com.example.irving.smartfridge;

import android.app.Application;
import android.content.Context;

import com.youtu.Youtu;

public class MyApplication extends Application {
    private static Context mContext;

    // Tencent YouTu
    public static final String APP_ID = "10136384";
    public static final String SECRET_ID = "AKID06ACILoIE6tsofBb6WrAQcjSFxeIKRuL";
    public static final String SECRET_KEY = "Y4VuJuSxPl3XyKxdHsNqmmquwunO6lQS";
    public static final String USER_ID = "1823997989";
    public static Youtu faceYoutu;

    // Fridge
    private static final String fridge_id = "1";
    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();
    }

    public static Context getInstance(){
        return mContext;
    }

    public Youtu getFaceYoutu() {
        // singleton mode
        if(faceYoutu == null)
            faceYoutu = new Youtu(APP_ID, SECRET_ID, SECRET_KEY,Youtu.API_YOUTU_END_POINT,USER_ID);
        return faceYoutu;
    }

    public String getFridge_id(){
        return fridge_id;
    }
}
