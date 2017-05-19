package com.ibulgakov.bleclient;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

/**
 * Created by User on 03.05.2017.
 */

public class BluetoothReceiver extends BroadcastReceiver {

    public BluetoothReceiver() {
        super();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        final Bundle extras = intent.getExtras();
        if (extras == null) return;

        if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
            final int state = extras.getInt(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
            switch (state) {
                case BluetoothAdapter.STATE_OFF:
                    bluetoothConnectionReceiverListener.onBluetoothStateChanged(false);
                    break;
                case BluetoothAdapter.STATE_TURNING_OFF:
                    break;
                case BluetoothAdapter.STATE_ON:
                    bluetoothConnectionReceiverListener.onBluetoothStateChanged(true);
                    break;
                case BluetoothAdapter.STATE_TURNING_ON:
                    break;
            }
        }
    }

    public static boolean isBluetoothEnabled() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }


    public static BluetoothConnectionReceiverListener bluetoothConnectionReceiverListener;
    public interface BluetoothConnectionReceiverListener {
        void onBluetoothStateChanged(boolean isEnabled);
    }
}

