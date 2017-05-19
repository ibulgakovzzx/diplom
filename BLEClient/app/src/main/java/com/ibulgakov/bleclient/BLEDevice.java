package com.ibulgakov.bleclient;

import android.content.Context;
import android.util.Log;

import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.RxBleDevice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import rx.Subscription;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;
import rx.Observable;

/**
 * Created by ibulgakov on 25.11.2016.
 */

public class BLEDevice extends Device {

    private static final String TAG = BLEDevice.class.getSimpleName();

    private static final byte CMD_ECGDATA_START = 0x13;
    private static final byte CMD_ECGDATA_STOP  = 0x14;
    private static final byte CMD_SIMULATE_ON   = 0x17;
    private static final byte CMD_SIMULATE_OFF  = 0x18;
    private static final byte CMD_ENABLE_FALL_DETECT = 0x25;
    private static final byte CMD_DISABLED_FALL_DETECT = 0x26;
    private static final byte CMD_RESET_FALL_DETECT = 0x27;

    private static final UUID dataCharacteristic = UUID.fromString("0BB93C6C-CFE3-4D03-8687-9CE1CD3CA239");
    private static final UUID batteryCharacteristic = UUID.fromString("0000E101-0000-1000-8000-00805F9B34FB");

    private static final int ECG_SAMPLES_IN_PACKET = 8;
    private static final int FALLSTATE_SAMPLES_IN_PACKET = 8;

    private PublishSubject<Integer> ecgSubject;
    private PublishSubject<String> postureSubject;
    private PublishSubject<Boolean> fallStateSubject;
    private PublishSubject<Boolean> chargingStateSubject;
    private Subscription dataSubscription;

    private boolean last_charging_state = false;
    private int battery_lvl;
    private boolean send_ecg = true;

    public BLEDevice(String name, String status, RxBleDevice rxBleDevice){
        super(rxBleDevice);
        this.name = name;
        this.connecting_status = status;
//        device_ID = getDeviceID();

        ecgSubject = PublishSubject.create();
        fallStateSubject = PublishSubject.create();
        chargingStateSubject = PublishSubject.create();
    }


    private byte[] createCmd(byte command) {
        return createCmd(command, null);
    }

    private byte[] createCmd(byte command, byte[] data){
        byte pid = 0; // default

        byte i;
        byte cs;
        byte[] bytes = new byte[20];

        bytes[0] = 0x55;    // sync
        bytes[1] = command; // add the command
        bytes[2] = pid;     // packet id

        // copy the 16 bytes of data
        for(i=0, cs=0; i<16; i++) {
            if(data != null && i < data.length) {
                bytes[3+i] = data[i];
            } else {
                bytes[3+i] = 0;
            }
        }
        // calculate the checksum
        for(i=0, cs=0; i<19; i++) {
            cs += bytes[i];
        }
        bytes[19] = cs;
        return bytes;
    }


    @Override
    public rx.Observable<Integer> getBatteryLevel() {
        return connection
                .setupNotification(batteryCharacteristic)
                .flatMap(observable  -> observable )
                .map(bytes  -> {
                            int level = bytes[0];
                            level &= 0xFF;
                            boolean charging_state = (level > 127);
                            if(level > 127)
                                level -= 128;
                            if(charging_state != last_charging_state) {
                                last_charging_state = charging_state;
                                chargingStateSubject.onNext(charging_state);
                            }
                            last_charging_state = charging_state;
                            battery_lvl = level;
                            Log.i(TAG, "Battery level: " +  level );
                            return level;
                        }
                );
    }


    private byte[] ecgPacketsCount() {
        int maxSessionTime = 20 * 60;  // in seconds

        int sampleRate = 250;
        int packets = ( maxSessionTime * sampleRate ) / 8; // 8 samples per packet
        byte low = (byte)(packets & 0xFF);
        byte high = (byte)((packets & 0xFF00) >> 8);

        return new byte[] { low, high };
    }


    private boolean getXYZ(Packet packet){
        List<Short> data = new ArrayList<>();
        for(int i = 0; i < FALLSTATE_SAMPLES_IN_PACKET; ++i){
            data.add(packet.getShortByIndex(i));
        }
        return ((int)data.get(4) == 1);
    }


    public Observable<Integer> getECG() {
        return ecgSubject;
    }

    public Observable<Boolean> getFallState(){
        return fallStateSubject;
    }

    public Observable<Boolean> getChargingState(){
        return chargingStateSubject;
    }

    public void setSend_ECG(boolean send_ecg){ this.send_ecg = send_ecg; }

    public int getBatteryLvlValue(){ return battery_lvl; }

    public int getSampleRate(){
        return 250;
    }

    public String getHexDeviceID(){
        return hexDevice_ID;
    }

    public boolean getCurrentChargingState(){
        return last_charging_state;
    }

    private void startObservingData(RxBleConnection conn) {
        dataSubscription = conn.setupNotification(dataCharacteristic)
                .flatMap(O -> O)
                .subscribe(bytes -> {
                    Packet packet = new Packet(bytes);
                    switch (packet.type()) {
                        case Packet.TYPE_ECG: {
                            if(send_ecg) {
                                for(int i = 0; i < ECG_SAMPLES_IN_PACKET; ++i) {
                                    ecgSubject.onNext((int)packet.getShortByIndex(i));
                                }
                                Log.i(TAG,"ECG data: " + packet.getShortByIndex(0));
                            }
                            break;
                        }
                        case Packet.TYPE_CONFIRM: {
                            Log.i(TAG, "Command confirm");
                            break;
                        }
                    }
                }, throwable -> {
                    Log.i(TAG, throwable.toString());
                });
    }

    @Override
    public void Connect(Context context, Action1<RxBleConnection> success, Action1<Throwable> failure) {
        super.Connect(context, conn -> {
            send_ecg = true;
            startObservingData(conn);
            success.call(conn);
            connection.writeCharacteristic(dataCharacteristic, rejivaSimulatorOff())
                    .flatMap(resp -> connection.writeCharacteristic(dataCharacteristic, rejivaStartECGStream()))
                    .flatMap(resp -> connection.writeCharacteristic(dataCharacteristic, rejivaEnableFallDetection()))
                    .subscribe(
                            bytes -> Log.i(TAG, "Write done"),
                            throwable -> Log.i(TAG, throwable.toString())
                    );

        }, failure);
    }


    @Override
    public rx.Observable Disconnect() {
        if(dataSubscription != null && !dataSubscription.isUnsubscribed()) {
            return connection.writeCharacteristic(dataCharacteristic, rejivaStopECGStream())
                    .flatMap(resp -> connection.writeCharacteristic(dataCharacteristic, rejivaDisabledFallDetection()))
                    .doOnTerminate(() -> {
                        dataSubscription.unsubscribe();
                        dataSubscription = null;
                        super.Disconnect();
                    })
                    .flatMap(next -> Observable.empty(),error -> super.Disconnect(),super::Disconnect);
        } else
            return super.Disconnect();

    }

    private byte[] rejivaStartECGStream(){
        return createCmd(CMD_ECGDATA_START, ecgPacketsCount());
    }

    private byte[] rejivaStopECGStream(){
        return createCmd(CMD_ECGDATA_STOP, ecgPacketsCount());
    }

    private byte[] rejivaSimulatorOff(){
        return createCmd(CMD_SIMULATE_OFF);
    }

    private byte[] rejivaEnableFallDetection(){
        return createCmd(CMD_ENABLE_FALL_DETECT);
    }

    private byte[] rejivaDisabledFallDetection(){
        return createCmd(CMD_RESET_FALL_DETECT);
    }

    @Override
    public int getDeviceID() {
        String hexID = name.substring(name.lastIndexOf(":") + 1);
        hexDevice_ID = hexID;
        return Integer.parseInt(hexID,16);
    }

}
