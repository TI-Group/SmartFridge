package com.example.irving.smartfridge;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.example.irving.smartfridge.util.Buzzer;
import com.example.irving.smartfridge.util.GpioDevice;
import com.google.android.things.pio.Gpio;

import java.io.IOException;

public class MyApplication extends Application {
    private static Context mContext;

    private static final String Buzzer_GPIO = "BCM26";  // PIN_37
    private static final String LightSwitch_GPIO = "BCM4";  // PIN_7
    private static final String Put_GPIO = "BCM21";   // PIN_40
    private static final String Take_GPIO = "BCM20";    // PIN_38

    private static Buzzer buzzer;
    private static GpioDevice lightSwitch;
    private static GpioDevice put_button;
    private static GpioDevice take_button;

    // Fridge
    private static final String fridge_id = "fridge2";
    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();
        try {
            // output device
            buzzer = new Buzzer(Buzzer_GPIO);
            // input device
            lightSwitch = new GpioDevice(LightSwitch_GPIO);
            lightSwitch.getGpio().setDirection(Gpio.DIRECTION_IN);
            lightSwitch.getGpio().setEdgeTriggerType(Gpio.EDGE_RISING);

            put_button = new GpioDevice(Put_GPIO);
            put_button.getGpio().setDirection(Gpio.DIRECTION_IN);
            put_button.getGpio().setEdgeTriggerType(Gpio.EDGE_FALLING);

            take_button = new GpioDevice(Take_GPIO);
            take_button.getGpio().setDirection(Gpio.DIRECTION_IN);
            take_button.getGpio().setEdgeTriggerType(Gpio.EDGE_FALLING);


        }catch (IOException e){
            Log.e(mContext.toString(), "onCreate: error when initialize gpio device");
            e.printStackTrace();
        }
    }

    @Override
    public void onTerminate() {
        try {
            put_button.getGpio().close();
            take_button.getGpio().close();
            lightSwitch.getGpio().close();
            buzzer.getGpio().close();
        }catch (IOException e){
            Log.e(mContext.toString(), "onTerminate: error when close gpio device");
            e.printStackTrace();
        }
        super.onTerminate();
    }

    public static Context getInstance(){
        return mContext;
    }

    public String getFridge_id(){
        return fridge_id;
    }

    public static Buzzer getBuzzer() {
        return buzzer;
    }

    public static GpioDevice getLightSwitch() {
        return lightSwitch;
    }

    public static GpioDevice getPut_button() {
        return put_button;
    }

    public static GpioDevice getTake_button() {
        return take_button;
    }
}
