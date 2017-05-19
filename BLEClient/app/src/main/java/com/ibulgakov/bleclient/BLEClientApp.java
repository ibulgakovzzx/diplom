package com.ibulgakov.bleclient;

import android.app.Application;

/**
 * Created by User on 03.05.2017.
 */

public class BLEClientApp extends Application{
    private static BLEClientApp mInstance;
    public static synchronized BLEClientApp getInstance() {
        if(mInstance == null)
            mInstance = new BLEClientApp();
        return mInstance;
    }

    public void setBluetoothListener(BluetoothReceiver.BluetoothConnectionReceiverListener listener){
        BluetoothReceiver.bluetoothConnectionReceiverListener = listener;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;
    }
}
