package com.ibulgakov.bleclient;

import android.content.Context;
import android.view.View;

import java.util.ArrayList;

/**
 * Created by User on 03.05.2017.
 */

public class AppData {
    private static AppData instance = null;
    private ArrayList<Device> discoveredDevices;
    private BLEDevice device;
    private Context context;
    private int chargingState = View.INVISIBLE;

    public static AppData getInstance() {
        if (instance == null)
            synchronized (AppData.class) {
                instance = new AppData();
            }
        return instance;
    }

    public Context getContext() {
        return context;
    }
    public void setContext(Context context) {
        this.context = context;
    }

    public ArrayList<Device> getDiscoveredDevices() {
        return discoveredDevices;
    }

    public AppData() {
        discoveredDevices = new ArrayList<>();
    }

    public BLEDevice getDevice(){
        return device;
    }
    public  void setDevice(BLEDevice device){
        this.device = device;
    }

    public Integer getChargingState() {
        return chargingState;
    }
    public void setChargingState(Integer chargingState) {
        this.chargingState = chargingState;
    }
}
