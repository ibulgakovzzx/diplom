package com.ibulgakov.bleclient;

import android.content.Context;
import com.polidea.rxandroidble.RxBleClient;

public class GlobalRxBleClient  {

    private static GlobalRxBleClient instance = null;
    private static RxBleClient rxBleClient;

    public static synchronized GlobalRxBleClient getInstance(Context context){
        if(instance == null) {
            instance = new GlobalRxBleClient();
            rxBleClient = RxBleClient.create(context);
        }
        return  instance;
    }
    public RxBleClient getRxBleClient(){
        return rxBleClient;
    }

}
