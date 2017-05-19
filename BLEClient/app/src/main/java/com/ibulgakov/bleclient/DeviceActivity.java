package com.ibulgakov.bleclient;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.Toast;

import com.polidea.rxandroidble.exceptions.BleScanException;

import java.util.ArrayList;
import rx.Subscription;

public class DeviceActivity extends AppCompatActivity {

    private String TAG = DeviceActivity.class.getSimpleName();
    private final int REQUEST_ENABLE_BT = 1112;
    private ListView listDeviceView;
    private Subscription scanSubscription;
    private Subscription connectingStateSubscription;
    private ArrayList<Device> deviceList;
    private DeviceAdapter adapter;
    private GlobalRxBleClient rxBleClient;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);
        rxBleClient = GlobalRxBleClient.getInstance(this);
        listDeviceView = (ListView) findViewById(R.id.list_device);
        deviceList = AppData.getInstance().getDiscoveredDevices();
        adapter = new DeviceAdapter(this, deviceList);
        listDeviceView.setAdapter(adapter);
    }

    @Override
    protected void onResume(){
        super.onResume();
        startScaningBLEDevice();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(scanSubscription != null && !scanSubscription.isUnsubscribed()) {
            scanSubscription.unsubscribe();
            scanSubscription = null;
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                adapter.unSubscribeConnectionState();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_OK) {
            startScaningBLEDevice();
        }
    }

    private void startScaningBLEDevice(){
        if(scanSubscription != null && !scanSubscription.isUnsubscribed()){
            scanSubscription.unsubscribe();
            scanSubscription = null;
        }
        scanSubscription = rxBleClient.getRxBleClient().scanBleDevices()
                .filter(rxBleScanResult -> rxBleScanResult.getBleDevice().getName() != null)
                .subscribe(
                        rxBleConnection -> {
                            String deviceName = rxBleConnection.getBleDevice().getName() != null ? rxBleConnection.getBleDevice().getName() : "UnknownDevice";
                            if(!ListDeviceContains(deviceName)) {
                                Device dvc = new BLEDevice(deviceName,
                                        rxBleConnection.getBleDevice().getMacAddress(),
                                        rxBleConnection.getBleDevice());
                                deviceList.add(dvc);
                                adapter.notifyDataSetChanged();
                            }
                        },
                        throwable -> {
                            handleBleScanException((BleScanException)throwable);
                            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                        }
                );
    }

    private void handleBleScanException(BleScanException bleScanException) {
        switch (bleScanException.getReason()) {
            case BleScanException.BLUETOOTH_NOT_AVAILABLE:
                Toast.makeText(DeviceActivity.this, "Bluetooth is not available", Toast.LENGTH_SHORT).show();
                break;
            case BleScanException.BLUETOOTH_DISABLED:
                Toast.makeText(DeviceActivity.this, "Enable bluetooth and try again", Toast.LENGTH_SHORT).show();
                break;
            case BleScanException.LOCATION_PERMISSION_MISSING:
                Toast.makeText(DeviceActivity.this, "On Android 6.0 location permission is required. Implement Runtime Permissions", Toast.LENGTH_SHORT).show();
                break;
            case BleScanException.LOCATION_SERVICES_DISABLED:
                Toast.makeText(DeviceActivity.this, "Location services needs to be enabled on Android 6.0", Toast.LENGTH_SHORT).show();
                break;
            case BleScanException.BLUETOOTH_CANNOT_START:
            default:
                Toast.makeText(DeviceActivity.this, "Unable to start scanning", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private boolean ListDeviceContains(String name){
        if(deviceList.size() == 0)
            return false;
        for (Device cur : deviceList) {
            if (cur.name.equals(name))
                return true;
        }
        return false;
    }
}
