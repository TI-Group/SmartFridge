package com.example.irving.smartfridge.util;

import android.util.Log;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.GpioCallback;
import com.google.android.things.pio.PeripheralManager;

import java.io.IOException;

public class Buzzer {
    private static final String GPIO = "BCM26";  // PIN_37
    private static Gpio mGpio;

    private static final String TAG = "Buzzer";

    public Buzzer() throws IOException{
        if(mGpio == null)
            initGpio();
    }

    public Gpio getGpio(){
        if(mGpio != null)
            return mGpio;
        else{
            initGpio();
            return mGpio;
        }
    }

    private void initGpio(){
        try {
            PeripheralManager service = PeripheralManager.getInstance();
            mGpio = service.openGpio(GPIO);

            // set Direction
            mGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
            mGpio.setActiveType(Gpio.ACTIVE_HIGH);
        }catch (IOException e){
            Log.e(TAG, "initGpio: open gpio error");
            e.printStackTrace();
        }
    }

    public void buzz(){

        try {
            mGpio.setValue(true);
            Thread.sleep(500);
            mGpio.setValue(false);
        }catch (InterruptedException e){
            // TODO
        }catch (IOException e){
            e.printStackTrace();
            Log.e(TAG, "buzz: ioexception");
        }
        return;

    }

}
