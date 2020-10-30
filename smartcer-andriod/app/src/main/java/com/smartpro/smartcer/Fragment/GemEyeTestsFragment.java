package com.smartpro.smartcer.Fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.smartpro.smartcer.Bluetooth.GemEyeIBluetoothLeService;
import com.smartpro.smartcer.BluetoothMessage.GemEyeIMessage;
import com.smartpro.smartcer.Control.LockableViewPager;
import com.smartpro.smartcer.Activity.MainActivity;
import com.smartpro.smartcer.Model.DiamondTestsModel;
import com.smartpro.smartcer.Control.PagerAdapter;
import com.smartpro.smartcer.R;
import com.smartpro.smartcer.Control.ResizableImageView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;
import java.util.List;

/**
 * Created by developer@gmail.com on 12/26/15 AD.
 */
public class GemEyeTestsFragment extends Fragment implements FragementBecameVisible {
    protected MainActivity activity;
    protected View rootView;
    protected LockableViewPager viewPager;
    protected ProgressDialog progressDialog;

    protected BluetoothAdapter mBluetoothAdapter;
    protected BluetoothSocket mmSocket;
    protected BluetoothDevice mmDevice;
    protected OutputStream mmOutputStream;
    protected InputStream mmInputStream;
    protected Thread workerThread;
    protected byte[] readBuffer;
    protected int readBufferPosition;
    protected volatile boolean stopWorker;
    protected boolean isBTConnected;

    protected GemEyeIBluetoothLeService mBluetoothLeService;
    protected Handler mHandler;

    protected String deviceAddress = "";
    protected float adcValue;
    protected String testResult;
    protected String[] gemstoneStrings;
    protected Boolean isIgnoreBTMessage = false;
    protected float stoppedIndicatorValue = 0.0f;
    public static final int ANIMATION_DELAY = 200;
    public static final int ANIMATION_METAL_DELAY = 400;

    public static GemEyeTestsFragment newInstance(Activity activity) {
        GemEyeTestsFragment fragment = new GemEyeTestsFragment();
        fragment.activity = (MainActivity) activity;
        return fragment;
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_gemeye_tests, container, false);
        viewPager = (LockableViewPager) container;
        mHandler = new Handler();

        setOnClickGemStoneTests();
        setActionButton();
        setOK();
        setIndicatorAnimationToZero();
        SetGemstoneStrings();
        setShowBTMsg();

        return rootView;
    }

    public void setShowBTMsg() {
        if (activity.getIsShowBTMsg()) {
            LinearLayout btMsg = (LinearLayout) rootView.findViewById(R.id.gemstone_bluetooth_message_panel);
            btMsg.setVisibility(View.VISIBLE);
        }
    }

    public void SetGemstoneStrings() {
        gemstoneStrings = activity.getResources().getStringArray(R.array.gemstone_test_result_array);
    }

    public void setOnClickGemStoneTests() {
        ResizableImageView gemStoneIcon  = (ResizableImageView) rootView.findViewById(R.id.gemstone_icon);
        gemStoneIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickConnect();
            }
        });

        LinearLayout blueToothPanel = (LinearLayout) rootView.findViewById(R.id.gemstone_bluetooth_panel);
        blueToothPanel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickConnect();
            }
        });
        Button btnConnect = (Button) rootView.findViewById(R.id.btn_gemstone_connect);
        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickConnect();
            }
        });
    }

    private void onClickConnect() {
        if (isBTConnected) {
            try {
                resetBluetoothPort();
            } catch (Exception ex) {
                activity.log("resetBluetoothPort", ex.getMessage(), true);
            }
        } else {
            TextView txtStatus = (TextView) rootView.findViewById(R.id.gemstone_connected_status);
            if (txtStatus.getText() == "Status : Scanning...") {
                scanLeDevice(false);
                setNotConnectedDevice();
            } else {
                txtStatus.setText("Status : Scanning...");
                Button btnConnect = (Button) rootView.findViewById(R.id.btn_gemstone_connect);
                btnConnect.setText("Stop scan");
                EnableBluetooth();
            }
        }
    }

    private void EnableBluetooth() {
        //Is Bluetooth Adapter
        if (mBluetoothAdapter == null) {
            if (!activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
                Toast.makeText(getActivity(), activity.bluetoothNotBle, Toast.LENGTH_SHORT).show();
                return;
            }
            BluetoothManager bluetoothManager = (BluetoothManager) activity.getSystemService(Context.BLUETOOTH_SERVICE);
            mBluetoothAdapter = bluetoothManager.getAdapter();

            if (mBluetoothAdapter == null) {
                Toast.makeText(getActivity(), activity.bluetoothNotAvailable, Toast.LENGTH_SHORT).show();
                return;
            }
        }

        //Is Enable
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, MainActivity.REQUEST_ENABLE_BT);
        } else {
            progressToNextCheck();
        }
    }

    private void progressToNextCheck(){
        scanLeDevice(true);
    }

    private void goToTheTask() {
        String deviceName = "";
        deviceAddress = "";

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        activity.log("size", pairedDevices.size()+"", false);

        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                activity.log("BT device", device.getName() + "(" + device.getAddress() + ")", false);
                if (device.getName().equals(GemEyeIBluetoothLeService.GEMEYE_I_DEVICE_NAME)) {
                    deviceName = device.getName();
                    deviceAddress = device.getAddress();
                    break;
                }
            }
        }
        activity.log("Device Nm", deviceName, false);

        if (deviceName.equals(GemEyeIBluetoothLeService.GEMEYE_I_DEVICE_NAME) && !deviceAddress.equals("")) {
            openBluetoothPort();
        } else {
            Toast.makeText(activity, String.format(activity.bluetoothIsNotPaired, GemEyeIBluetoothLeService.GEMEYE_I_DEVICE_NAME ), Toast.LENGTH_SHORT).show();
            activity.log("Bt Device", "Device is not paired", false);
        }
    }

    private void openBluetoothPort() {
        progressDialog = ProgressDialog.show(activity, "Gem-eye-I Tests", "Connecting " + GemEyeIBluetoothLeService.GEMEYE_I_DEVICE_NAME + " ...", true);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    //if (mmSocket == null) {
                    mmDevice = mBluetoothAdapter.getRemoteDevice(deviceAddress);
                    UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
                    mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
                    mmSocket.connect();

                    mmOutputStream = mmSocket.getOutputStream();
                    mmInputStream = mmSocket.getInputStream();
                    //}
                } catch (Exception ex) {
                    activity.log("openBluetoothPort", ex.getMessage(), false);
                    ex.printStackTrace();
                }

                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.dismiss();
                        if (mmSocket.isConnected()) {
                            isBTConnected = true;
                            setConnectedDevice();
                            Toast.makeText(activity, GemEyeIBluetoothLeService.GEMEYE_I_DEVICE_NAME + " connected", Toast.LENGTH_SHORT).show();
                            activity.log("openBluetoothPort", GemEyeIBluetoothLeService.GEMEYE_I_DEVICE_NAME + " connected (" + deviceAddress + ")", false);
                            setDataReceiver();
                        } else {
                            Toast.makeText(activity, String.format(activity.bluetoothMightBeClosed, GemEyeIBluetoothLeService.GEMEYE_I_DEVICE_NAME ), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        }).start();
    }

    private void setConnectedDevice() {
        this.isBTConnected = true;
        TextView txtStatus = (TextView) rootView.findViewById(R.id.gemstone_connected_status);
        txtStatus.setTextColor(activity.colorConnected);
        txtStatus.setText(activity.bluetoothConnected);
        Button btnConnect = (Button) rootView.findViewById(R.id.btn_gemstone_connect);
        btnConnect.setText(activity.btnDisconnect);
        activity.log("onServiceConnected", "Connected", false);
    }

    private void setNotConnectedDevice() {
        this.isBTConnected = false;
        TextView txtStatus = (TextView) rootView.findViewById(R.id.gemstone_connected_status);
        txtStatus.setTextColor(activity.colorText);
        txtStatus.setText(activity.bluetoothNotConnected);
        Button btnConnect = (Button) rootView.findViewById(R.id.btn_gemstone_connect);
        btnConnect.setText(activity.btnConnect);
        activity.log("onServiceDisconnected","Disconnected", false);
    }

    protected void resetBluetoothPort() throws IOException {
        mBluetoothLeService.disconnect();
    }

    protected void setDataReceiver() {
        final Handler handler = new Handler();
        final byte delimiter = 10; //This is the ASCII code for a newline character

        stopWorker = false;
        readBufferPosition = 0;
        readBuffer = new byte[1024];
        workerThread = new Thread(new Runnable()
        {
            public void run()
            {
                while(!Thread.currentThread().isInterrupted() && !stopWorker)
                {
                    try
                    {
                        int bytesAvailable = mmInputStream.available();
                        if(bytesAvailable > 0)
                        {
                            byte[] packetBytes = new byte[bytesAvailable];
                            mmInputStream.read(packetBytes);
                            for(int i=0;i<bytesAvailable;i++)
                            {
                                byte b = packetBytes[i];
                                if(b == delimiter)
                                {
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes, "US-ASCII");
                                    readBufferPosition = 0;

                                    handler.post(new Runnable()
                                    {
                                        public void run()
                                        {
                                            displayBluetoothMessage(data);
                                        }
                                    });
                                }
                                else
                                {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                        }
                    }
                    catch (IOException ex)
                    {
                        stopWorker = true;
                    }
                }
            }
        });

        workerThread.start();
    }

    private void displayBluetoothMessage(String data) {
        if (isIgnoreBTMessage == false) {
            if (data != null && activity.getCurrentPageShowing() == PagerAdapter.PAGE_GEMEYE_TESTS) {
                isIgnoreBTMessage = true;

                GemEyeIMessage device = new GemEyeIMessage();
                GemEyeIMessage.Message message = device.getMessage(data);
                if (message instanceof GemEyeIMessage.BatteryMessage) {
                    displayBattery((GemEyeIMessage.BatteryMessage) message);
                } else if (message instanceof GemEyeIMessage.MetalMessage) {
                    displayMetal();
                } else if (message instanceof GemEyeIMessage.TestResultsMessage) {
                    displayResult((GemEyeIMessage.TestResultsMessage) message);
                }
                isIgnoreBTMessage = false;
            }
        }
    }

    public void displayMetal() {
        TextView metalResult = (TextView) rootView.findViewById(R.id.gemstone_metal_text);
        metalResult.setBackgroundResource(R.drawable.textview_rounded_corner_stable_in);

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                TextView metalResult = (TextView) rootView.findViewById(R.id.gemstone_metal_text);
                metalResult.setBackgroundResource(R.drawable.textview_rounded_corner_stable_out);
            }
        }, ANIMATION_METAL_DELAY);

    }

    public void displayBattery(GemEyeIMessage.BatteryMessage message) {
        ResizableImageView batteryIcon = (ResizableImageView) rootView.findViewById(R.id.gemstone_battery_icon);

        if (message.value.equals("1")) {
            batteryIcon.setImageResource(R.drawable.battery_status_1);
        } else if (message.value.equals("2")) {
            batteryIcon.setImageResource(R.drawable.battery_status_2);
        } else if (message.value.equals("3")) {
            batteryIcon.setImageResource(R.drawable.battery_status_3);
        } else if (message.value.equals("4")) {
            batteryIcon.setImageResource(R.drawable.battery_status_4);
        } else if (message.value.equals("5")) {
            batteryIcon.setImageResource(R.drawable.battery_status_5);
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == MainActivity.REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                Toast.makeText(getActivity(), "Bluetooth started", Toast.LENGTH_SHORT).show();
                progressToNextCheck();
            }
        } else if (requestCode == MainActivity.REQUEST_DISCOVERABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                goToTheTask();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void displayResult(GemEyeIMessage.TestResultsMessage message) {
        adcValue = message.value;
        playIndicatorAnimation(message.value);
        setPanelAnimationResult(message.value);
    }

    private String getGemstoneText(float value) {
        if (value < 0.0) {
            return gemstoneStrings[0];
        } else if (value == 0.0) {
            return gemstoneStrings[1];
        } else if (value <= 5.1) {
            return gemstoneStrings[2];
        } else {
            return gemstoneStrings[3];
        }
    }

    private void setIndicatorAnimationToZero() {
        adcValue = 0.0f;
        stoppedIndicatorValue = -60.2f ;
        ResizableImageView indicator = (ResizableImageView) rootView.findViewById(R.id.gemstone_indicator);
        RotateAnimation animation = new RotateAnimation(stoppedIndicatorValue, stoppedIndicatorValue
                , Animation.RELATIVE_TO_PARENT, 0.5f
                , Animation.RELATIVE_TO_PARENT, 1.0f);
        animation.setDuration(ANIMATION_DELAY);
        animation.setRepeatCount(0);
        animation.setFillAfter(true);
        indicator.startAnimation(animation);
    }

    private void playIndicatorAnimation(float value) {
        if (value < 0.0f) {
            value = 0.0f;
        }

        float fromLastStoppedIndicatorValue = stoppedIndicatorValue;
        if (value == 3.5f) {
            stoppedIndicatorValue = 0.0f;
        } else {
            stoppedIndicatorValue = -60.2f + ((60.2f / 3.5f) * value);
        }

        ResizableImageView indicator = (ResizableImageView) rootView.findViewById(R.id.gemstone_indicator);
        RotateAnimation animation = new RotateAnimation(fromLastStoppedIndicatorValue, stoppedIndicatorValue
                , Animation.RELATIVE_TO_PARENT, 0.5f
                , Animation.RELATIVE_TO_PARENT, 1.0f);
        animation.setDuration(ANIMATION_DELAY);
        animation.setRepeatCount(0);
        animation.setFillAfter(true);
        indicator.startAnimation(animation);
    }

    private void setPanelAnimationToNone() {
        ResizableImageView panel = (ResizableImageView) rootView.findViewById(R.id.gemstone_panel);
        panel.setImageResource(R.drawable.gemeye_panel_none);
    }

    public void setPanelAnimationToReady() {
        ResizableImageView panel =  (ResizableImageView) rootView.findViewById(R.id.gemstone_panel);
        panel.setImageResource(R.drawable.gemeye_panel_ready);
        activity.speak("Ready");
    }

    public void setPanelAnimationToGemstone() {
        ResizableImageView panel =  (ResizableImageView) rootView.findViewById(R.id.gemstone_panel);
        panel.setImageResource(R.drawable.gemeye_panel_ready);
    }

    public void setPanelAnimationToDiamond() {
        ResizableImageView panel =  (ResizableImageView) rootView.findViewById(R.id.gemstone_panel);
        panel.setImageResource(R.drawable.gemeye_panel_diamond);
        activity.speak("Diamond");
    }

    private void setPanelAnimationResult(float value) {
        testResult = getGemstoneText(adcValue);

        Handler handler = new Handler();
        if (value <= 5.1f) {
            handler.postDelayed(new Runnable() {
                public void run() {
                    setPanelAnimationToGemstone();
                }
            }, ANIMATION_DELAY);
        } else {
            handler.postDelayed(new Runnable() {
                public void run() {
                    setPanelAnimationToDiamond();
                }
            }, ANIMATION_DELAY);
        }
    }

    public void setActionButton() {
        ResizableImageView actionDiamond = (ResizableImageView) rootView.findViewById(R.id.action_reader_tests);
        actionDiamond.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                viewPager.goToDiamondTestsView();
            }
        });
        ResizableImageView actionScreen = (ResizableImageView) rootView.findViewById(R.id.action_screen_tests);
        actionScreen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                viewPager.goToScreenTestsView();
            }
        });
        ResizableImageView actionGauge = (ResizableImageView) rootView.findViewById(R.id.action_gague_tests);
        actionGauge.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                viewPager.goToGaugeTestsView();
            }
        });
        ResizableImageView actionScale = (ResizableImageView) rootView.findViewById(R.id.action_scale_tests);
        actionScale.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                viewPager.goToScaleTestsView();
            }
        });
        ResizableImageView actionCertification = (ResizableImageView) rootView.findViewById(R.id.action_certification);
        actionCertification.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                viewPager.goToCertificationView();
            }
        });
    }

    private void setOK() {
        Button btnOK = (Button) rootView.findViewById(R.id.btnOK);
        btnOK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                isIgnoreBTMessage = true;

                if (adcValue <= 0.0f) {
                    AlertDialog.Builder alert = new AlertDialog.Builder(activity);
                    alert.setTitle("Gem-eye-I Tests");
                    alert.setMessage("Have no test recording.\nPlease continue to record.");
                    alert.setPositiveButton("OK", new AlertDialog.OnClickListener() {
                        public void onClick(DialogInterface dialog, int arg1) {
                            isIgnoreBTMessage = false;
                        }});
                    alert.show();
                } else {
                    if (getGemstoneText(adcValue).equals("Diamond")) {
                        //Add Cert
                        DiamondTestsModel model = new DiamondTestsModel(true, GemEyeIBluetoothLeService.GEMEYE_I_DEVICE_NAME, "Diamond");
                        activity.setDiamondTestResult(model);

                        Toast.makeText(activity, "Added to Certificate", Toast.LENGTH_SHORT).show();
                        activity.log("OK", "Add gemstone to cer", false);
                        viewPager.goToCertificationView();
                    } else {
                        AlertDialog.Builder alert = new AlertDialog.Builder(activity);
                        alert.setTitle("Gem-eye-I Tests");
                        alert.setMessage("Your gemstone is not diamond.");
                        alert.setPositiveButton("OK", new AlertDialog.OnClickListener() {
                            public void onClick(DialogInterface dialog, int arg1) {
                                isIgnoreBTMessage = false;
                            }});
                        alert.show();
                    }
                }
            }
        });
    }

    @Override
    public void fragmentBecameVisible() {
        activity.log("fragmentBecameVisible","back to diamond", false);
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (Build.VERSION.SDK_INT < 21) {
                        mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    } else {
                        mBluetoothAdapter.getBluetoothLeScanner().stopScan(mScanCallback);
                    }
                }
            }, MainActivity.SCAN_PERIOD);

            if (android.os.Build.VERSION.SDK_INT < 21) {
                mBluetoothAdapter.startLeScan(mLeScanCallback);
            } else {
                mBluetoothAdapter.getBluetoothLeScanner().startScan(mScanCallback);
            }

        } else {
            if (mBluetoothAdapter != null) {
                try {
                    if (Build.VERSION.SDK_INT < 21) {
                        mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    } else {
                        mBluetoothAdapter.getBluetoothLeScanner().stopScan(mScanCallback);
                    }
                } catch (Exception ex) {}
            }
        }
    }

    // API 18 to 20 Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    activity.log("BT device", device.getName() + "(" + device.getAddress() + ")", false);

                    if (device.getName() != null && device.getName().equals(GemEyeIBluetoothLeService.GEMEYE_I_DEVICE_NAME)) {
                        deviceAddress = device.getAddress();
                        scanLeDevice(false);
                        connectToDevice();
                    }
                }
            });
        }
    };

    // Api 21 up
    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            if (device.getName() != null && device.getName().equals(GemEyeIBluetoothLeService.GEMEYE_I_DEVICE_NAME)) {
                deviceAddress = device.getAddress();
                scanLeDevice(false);
                connectToDevice();
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult sr : results) {
                activity.log("ScanResult - Results", sr.toString(), false);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            activity.log("Scan Failed", "Error Code: " + errorCode, false);
        }
    };

    public void connectToDevice() {
        Intent gattServiceIntent = new Intent(activity, GemEyeIBluetoothLeService.class);
        activity.bindService(gattServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
        activity.registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
    }

    @Override
    public void onResume() {
        super.onResume();
        activity.log("onResume","gemeye", false);
    }

    @Override
    public void onPause() {
        super.onPause();
        scanLeDevice(false);

        activity.log("onPause","gemeye", false);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (isBTConnected) {
            activity.unbindService(mServiceConnection);
            activity.unregisterReceiver(mGattUpdateReceiver);
            mBluetoothLeService = null;
        }
    }

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((GemEyeIBluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Toast.makeText(activity, String.format(activity.bluetoothMightBeClosed, GemEyeIBluetoothLeService.GEMEYE_I_DEVICE_NAME ), Toast.LENGTH_SHORT).show();
                activity.log("onServiceConnected","device may be closed", false);
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(deviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (GemEyeIBluetoothLeService.ACTION_GEMEYE_I_GATT_CONNECTED.equals(action)) {
                setConnectedDevice();
                setPanelAnimationToReady();
                setIndicatorAnimationToZero();
            } else if (GemEyeIBluetoothLeService.ACTION_GEMEYE_I_GATT_DISCONNECTED.equals(action)) {
                setNotConnectedDevice();
                setPanelAnimationToNone();
                setIndicatorAnimationToZero();
                activity.unbindService(mServiceConnection);
                activity.unregisterReceiver(mGattUpdateReceiver);
            } else if (GemEyeIBluetoothLeService.ACTION_GEMEYE_I_GATT_SERVICES_DISCOVERED.equals(action)) {
                mBluetoothLeService.displayGattServices();
            } else if (GemEyeIBluetoothLeService.ACTION_GEMEYE_I_DATA_AVAILABLE.equals(action)) {
                displayBluetoothMessage(intent.getStringExtra(GemEyeIBluetoothLeService.EXTRA_DATA_GEMEYE_I));
            }
        }
    };

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(GemEyeIBluetoothLeService.ACTION_GEMEYE_I_GATT_CONNECTED);
        intentFilter.addAction(GemEyeIBluetoothLeService.ACTION_GEMEYE_I_GATT_DISCONNECTED);
        intentFilter.addAction(GemEyeIBluetoothLeService.ACTION_GEMEYE_I_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(GemEyeIBluetoothLeService.ACTION_GEMEYE_I_DATA_AVAILABLE);
        return intentFilter;
    }
}
