package com.example.irving.smartfridge.util;

import android.util.Log;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.GpioCallback;
import com.google.android.things.pio.PeripheralManager;

import java.io.IOException;

public class Buzzer extends GpioDevice{
    private Gpio mGpio;
    private static final String TAG = "Buzzer";

    public Buzzer(String BCM) throws IOException{
        PeripheralManager service = PeripheralManager.getInstance();
        mGpio = service.openGpio(BCM);
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
