package com.example.irving.smartfridge.util;

import android.util.Log;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.GpioCallback;
import com.google.android.things.pio.PeripheralManager;

import java.io.IOException;

public class LightSwitch {
    private static final String GPIO = "BCM4";  // PIN_7
    private static Gpio mGpio;

    private static final String TAG = "LightSwitch";

    public LightSwitch() throws IOException {
        if(mGpio == null)
            initGpio();
    }

    public Gpio getGpio(){
        return mGpio;
    }

    private void initGpio() throws IOException{
        PeripheralManager service = PeripheralManager.getInstance();
        mGpio = service.openGpio(GPIO);

        // set Direction
        mGpio.setDirection(Gpio.DIRECTION_IN);
        mGpio.setEdgeTriggerType(Gpio.EDGE_RISING);
    }

    private void setGpio(Gpio gpio){

    }

}
