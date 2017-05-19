package com.ibulgakov.bleclient;
import android.content.Context;
import android.util.Log;

import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.RxBleDevice;
import com.polidea.rxandroidble.exceptions.BleDisconnectedException;
import com.polidea.rxandroidble.exceptions.BleGattException;

import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;

public abstract  class Device {
    private final String TAG = Device.class.getName();
    private static boolean isDisconnectWithException = false;
    public String name;
    public String connecting_status;
    public String status;
    public int device_ID;
    public String hexDevice_ID = "";
    public RxBleDevice device;
    public RxBleConnection connection;
    private Subscription connSubscription = null;

    public Device(RxBleDevice device){
        this.device = device;
    }

    public void Connect(Context context, Action1<RxBleConnection> success, Action1<Throwable> failure){
        connSubscription = device
                .establishConnection(context, false)
                .retryWhen(Device::retryWhenDisconnectedWithDelay)
                .subscribe(conn -> {
                    connection = conn;
                    success.call(conn);
                }, throwable -> {
                    Log.i(TAG,throwable.getLocalizedMessage());
                    failure.call(throwable);
                });
    }

    private static Observable<?> retryWhenDisconnectedWithDelay(Observable<? extends Throwable> observable) {
        return observable.flatMap(error -> {
            // For BleDisconnectedException or BleGattException, we  retry
            if (error instanceof BleDisconnectedException || error instanceof BleGattException) {
                isDisconnectWithException = true;
                return Observable.timer(5, TimeUnit.SECONDS);
            }
            isDisconnectWithException = false;
            // For anything else, don't retry
            return Observable.error(error);
        });
    }


    public rx.Observable Disconnect() {
        if(connSubscription != null && !connSubscription.isUnsubscribed()) {
            connSubscription.unsubscribe();
            connSubscription = null;
            connection = null;
        }
        return Observable.empty();
    }

    public RxBleConnection.RxBleConnectionState getCurrentConnectionState() {
        if(device != null)
            return device.getConnectionState();
        else
            return RxBleConnection.RxBleConnectionState.DISCONNECTED;
    }


    public boolean isConnected() {
        if(device != null) {
            return device.getConnectionState() == RxBleConnection.RxBleConnectionState.CONNECTED;
        } else {
            return false;
        }
    }

    public boolean getIsDisconnectWithExeption() {
        return isDisconnectWithException;
    }
    public void setIsDisconnectWithExeption(boolean value) {
        isDisconnectWithException = value;
    }

    public rx.Observable<RxBleConnection.RxBleConnectionState> getConnectionState(){
        return device.observeConnectionStateChanges();
    }

    public abstract rx.Observable<Integer> getBatteryLevel();

    public abstract int getDeviceID();

}

