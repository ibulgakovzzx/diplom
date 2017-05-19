package com.ibulgakov.bleclient;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.YAxis.AxisDependency;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;

import com.polidea.rxandroidble.RxBleConnection;

import java.util.Timer;
import java.util.TimerTask;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity implements BluetoothReceiver.BluetoothConnectionReceiverListener, OnChartValueSelectedListener {

    private final String TAG = MainActivity.class.getSimpleName();
    private Observable<RxBleConnection> connectionObservable;
    private Subscription subscription_ecg;
    private Subscription subscription_battery;
    private Subscription subscription_battery_charging_state;
    private Button view_scan_device;
    private Button btn_start_observ_ecg;
    private Button btn_stop_observ_ecg;
    private TextView battery_lvl;
    private TextView charging;
    private ImageView bluetooth_indicator;
    private LineChart mChart;
    private static int xValue = 0;
    private static double yValue = 0.0f;
    private BluetoothReceiver bluetoothReceiver;


    public enum BATTERY_LEVEL_TYPE {
        UNKNOWN,
        NORMAL,
        CHARGING,
        LOW
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        AppData.getInstance().setContext(this);
        bluetoothReceiver = new BluetoothReceiver();
        initUIElements();
        initUIElementsListener();
    }


    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(bluetoothReceiver,new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        BLEClientApp.getInstance().setBluetoothListener(this);
        if (AppData.getInstance() != null && AppData.getInstance().getDevice() != null) {
            if (AppData.getInstance().getDevice().isConnected()) {
                //subscription from battery changes
                if (subscription_battery != null && !subscription_battery.isUnsubscribed()) {
                    subscription_battery.unsubscribe();
                    subscription_battery = null;
                } else {
                    final Observable<Integer> connection_battery = AppData.getInstance().getDevice().getBatteryLevel();
                    subscription_battery = connection_battery
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribeOn(Schedulers.io())
                            .subscribe(
                                    battery_lvl_value -> {
                                        updateBatteryTextView(battery_lvl_value);
                                    },
                                    throwable -> Log.i(TAG, throwable.toString())
                            );
                }
                //subscription from battery_charging changes
                if (subscription_battery_charging_state != null && !subscription_battery_charging_state.isUnsubscribed()) {
                    subscription_battery_charging_state.unsubscribe();
                    subscription_battery_charging_state = null;
                } else {
                    final Observable<Boolean> connection_charging_state = AppData.getInstance().getDevice().getChargingState();
                    subscription_battery_charging_state = connection_charging_state
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribeOn(Schedulers.io())
                            .subscribe(
                                    charging_state -> {
                                        if (charging_state) {
                                            AppData.getInstance().getDevice().setSend_ECG(false);
                                            charging.setText(getString(R.string.txt_charging));
                                        } else {
                                            AppData.getInstance().getDevice().setSend_ECG(true);
                                            charging.setText(getString(R.string.txt_unknown_charging));
                                        }
                                    },
                                    throwable -> Log.i(TAG, throwable.toString())
                            );
                }
            } else {
                battery_lvl.setText(getString(R.string.lable_unknown_battery_status));
                charging.setText(getString(R.string.txt_unknown_charging));
                updateBatteryTextView(0);
                Toast.makeText(getApplicationContext(), "Device status: Not connected", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(bluetoothReceiver);
    }


    @Override
    public void onBluetoothStateChanged(boolean isEnabled) {
        if (!isEnabled) {
            //bluetooth_indicator.startAnimation(getBlinkingAnimation());
            if (AppData.getInstance().getDevice() != null) {
                //bluetooth_indicator.startAnimation(getBlinkingAnimation());
                AppData.getInstance()
                        .getDevice()
                        .Disconnect()
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeOn(Schedulers.io())
                        .doOnTerminate(() -> {
                            battery_lvl.setText(getString(R.string.lable_unknown_battery_status));
                            charging.setText(getString(R.string.txt_unknown_charging));
                            AppData.getInstance().getDevice().connecting_status = "Disconnected";
                        })
                        .subscribe();
                AppData.getInstance().getDevice().setIsDisconnectWithExeption(false);
            }
        }
        //  else bluetooth_indicator.clearAnimation();
    }


    private void initUIElements() {
        view_scan_device = (Button) findViewById(R.id.btn_scan_device);
        battery_lvl = (TextView) findViewById(R.id.battery_level);
        charging = (TextView) findViewById(R.id.charging_state);
        btn_start_observ_ecg = (Button) findViewById(R.id.btn_observ_ecg_start);
        btn_stop_observ_ecg = (Button) findViewById(R.id.btn_observ_ecg_stop);

        mChart = (LineChart) findViewById(R.id.ecg_chart);
        mChart.setOnChartValueSelectedListener(this);
        mChart.setDrawGridBackground(false);
        mChart.getDescription().setEnabled(false);

        // add an empty data object
        LineData data = new LineData();
        data.setValueTextColor(Color.WHITE);
        mChart.setData(data);

        //bluetooth_indicator = (ImageView) findViewById(R.id.bluetooth_icon);
        //if(!BluetoothReceiver.isBluetoothEnabled()) bluetooth_indicator.startAnimation(getBlinkingAnimation());
    }


    private void initUIElementsListener() {
        view_scan_device.setOnClickListener(view -> {
            startActivity(new Intent(MainActivity.this, DeviceActivity.class));
        });

        btn_start_observ_ecg.setOnClickListener(view -> {
            if (AppData.getInstance().getDevice() != null && AppData.getInstance().getDevice().isConnected()) {
                if (subscription_ecg != null && !subscription_ecg.isUnsubscribed()) {
                    subscription_ecg.unsubscribe();
                    subscription_ecg = null;
                }
                final Observable<Integer> connection_ecg = AppData.getInstance().getDevice().getECG();
                subscription_ecg = connection_ecg
                        .subscribe(
                                data -> {
                                    Log.d(TAG, "ECG Data = " + data);
                                    if (data != null) {
                                        addEntry(xValue++, data);
                                    }
                                },
                                throwable -> {
                                    Log.i(TAG, throwable.toString());
                                }
                        );
            } else
                Toast.makeText(getApplicationContext(), "Device status: Not connected", Toast.LENGTH_SHORT).show();
        });
        btn_stop_observ_ecg.setOnClickListener(view -> {
            if (AppData.getInstance().getDevice() != null && AppData.getInstance().getDevice().isConnected()) {
                if (subscription_ecg != null && !subscription_ecg.isUnsubscribed()) {
                    subscription_ecg.unsubscribe();
                    subscription_ecg = null;
                }
            }
        });
    }

    private Animation getBlinkingAnimation() {
        final Animation animation = new AlphaAnimation(1, 0);
        animation.setDuration(500);
        animation.setInterpolator(new LinearInterpolator());
        animation.setRepeatCount(Animation.INFINITE);
        animation.setRepeatMode(Animation.REVERSE);
        return animation;
    }

    private void updateBatteryTextView(int batlvl) {
        BATTERY_LEVEL_TYPE blt;
        if (batlvl < 20 && batlvl != 0 && AppData.getInstance().getChargingState() != View.VISIBLE)
            blt = BATTERY_LEVEL_TYPE.LOW;
        else if (AppData.getInstance().getChargingState() == View.VISIBLE)
            blt = BATTERY_LEVEL_TYPE.CHARGING;
        else if (AppData.getInstance().getDevice().isConnected())
            blt = BATTERY_LEVEL_TYPE.NORMAL;
        else blt = BATTERY_LEVEL_TYPE.UNKNOWN;
        switch (blt) {
            case LOW:
                battery_lvl.setText(getString(R.string.lable_low_battery_lvl) + " " + batlvl + "%");
                break;
            case NORMAL:
                battery_lvl.setText(getString(R.string.lable_normal_battery_lvl) + " " + batlvl + "%");
                break;
            case CHARGING:
                battery_lvl.setText(getString(R.string.lable_charging_battery));
                break;
            case UNKNOWN:
                battery_lvl.setText(getString(R.string.lable_unknown_battery_status));
                break;
        }
    }


    private void addEntry(final int xValue, final double yValue) {
        runOnUiThread(() -> {
            LineData data = mChart.getData();
            ILineDataSet set = data.getDataSetByIndex(0);

            if (set == null) {
                set = createSet();
                data.addDataSet(set);
            }

            data.addEntry(new Entry(xValue, (float) yValue), 0);
            data.notifyDataChanged();

            // let the chart know it's data has changed
            mChart.notifyDataSetChanged();
            mChart.setVisibleXRangeMaximum(250);

            mChart.moveViewToX(data.getEntryCount());
        });

    }

    private LineDataSet createSet() {
        LineDataSet set = new LineDataSet(null, "ECG Data");
        set.setLineWidth(2.5f);
        set.setColor(Color.rgb(240, 99, 99));
        set.setHighLightColor(Color.rgb(190, 190, 190));
        set.setAxisDependency(AxisDependency.LEFT);
        set.setValueTextSize(10f);

        return set;
    }

    @Override
    public void onValueSelected(Entry e, Highlight h) {
        Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onNothingSelected() {

    }
}
