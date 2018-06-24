package com.example.irving.smartfridge.util;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManager;

import java.io.IOException;

public class GpioDevice {
    private Gpio mGpio;

    public GpioDevice(){}

    public GpioDevice(String BCM) throws IOException{
        PeripheralManager service = PeripheralManager.getInstance();
        mGpio = service.openGpio(BCM);
    }

    public Gpio getGpio(){
        return mGpio;
    }
}
