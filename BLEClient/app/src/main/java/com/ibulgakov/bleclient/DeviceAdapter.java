package com.ibulgakov.bleclient;

import android.content.Context;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import com.polidea.rxandroidble.RxBleConnection;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by User on 03.05.2017.
 */

public class DeviceAdapter extends BaseAdapter {
    private final Looper myLopper = Looper.myLooper();
    private final String TAG = DeviceAdapter.class.getSimpleName();
    private LayoutInflater inflater;
    private ArrayList<Device> objects;
    private Context context;
    private Subscription connectingStateSubscription;

    public DeviceAdapter(Context context, ArrayList<Device> items) {
        this.context = context;
        objects = items;
        inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return objects.size();
    }

    @Override
    public Object getItem(int position) {
        return objects.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View rowView = inflater.inflate(R.layout.device_list_item, parent, false);
        TextView statusDeviceTextView = (TextView) rowView.findViewById(R.id.device_status);
        TextView nameDeviceView = (TextView) rowView.findViewById(R.id.device_name);
        Switch toggleSwitchView = (Switch) rowView.findViewById(R.id.device_switch);

        Device device = (Device) getItem(position);

        statusDeviceTextView.setText(device.connecting_status);
        nameDeviceView.setText(device.name);
        toggleSwitchView.setChecked(device.getCurrentConnectionState() != RxBleConnection.RxBleConnectionState.DISCONNECTED);
        toggleSwitchView.setEnabled(device.getCurrentConnectionState() != RxBleConnection.RxBleConnectionState.CONNECTING && !device.getIsDisconnectWithExeption());
        if (connectingStateSubscription == null) {
            connectingStateSubscription = device
                    .getConnectionState()
                    .observeOn(AndroidSchedulers.from(myLopper))
                    .subscribe(
                            connectionState -> {
                                updateDevicesListItem(connectionState, device, position);
                                Log.i(TAG, "Connection state change to: " + connectionState);
                            },
                            error -> {
                                Log.i(TAG, error.getLocalizedMessage());
                            }
                    );
        }

        toggleSwitchView.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                device.Connect(context,
                        connect -> {
                            AppData.getInstance().setDevice((BLEDevice) device);
                            device.setIsDisconnectWithExeption(false);
                        }, throwable -> {
                            buttonView.setChecked(false);
                            Log.i(TAG, throwable.toString());
                        });
            } else {
                device.Disconnect()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnTerminate(() -> {
                            updateBatteryViews();
                            AppData.getInstance().setChargingState(View.INVISIBLE);
                            device.setIsDisconnectWithExeption(false);
                        })
                        .subscribe();
            }
        });
        return rowView;
    }


    private void updateBatteryViews() {

    }

    private void updateDevicesListItem(RxBleConnection.RxBleConnectionState connectionState, Device device, int position) {
        String connection_state = convertRxBleConnectionStateToString(connectionState).replace("{", "").replace("}", "");
        device.connecting_status = connection_state;
        objects.get(position).connecting_status = connection_state;
        notifyDataSetChanged();
    }

    private String convertRxBleConnectionStateToString(RxBleConnection.RxBleConnectionState connectionState) {
        String strConnectionState = "";
        Pattern pattern = Pattern.compile("\\{.*?\\}");
        Matcher matcher = pattern.matcher(connectionState.toString());
        boolean isFind = matcher.find();
        if (isFind) strConnectionState = matcher.group();
        return strConnectionState;
    }

    void unSubscribeConnectionState() {
        if (connectingStateSubscription != null && !connectingStateSubscription.isUnsubscribed()) {
            connectingStateSubscription.unsubscribe();
            connectingStateSubscription = null;
        }
    }
}