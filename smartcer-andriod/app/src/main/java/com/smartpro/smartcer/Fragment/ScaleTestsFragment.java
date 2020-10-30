package com.smartpro.smartcer.Fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
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
import android.speech.tts.TextToSpeech;
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

import com.smartpro.smartcer.Bluetooth.ScaleIBluetoothLeService;
import com.smartpro.smartcer.BluetoothMessage.ScaleIMessage;
import com.smartpro.smartcer.Control.LockableViewPager;
import com.smartpro.smartcer.Activity.MainActivity;
import com.smartpro.smartcer.Model.ScaleTestsModel;
import com.smartpro.smartcer.Control.PagerAdapter;
import com.smartpro.smartcer.R;
import com.smartpro.smartcer.Control.ResizableImageView;
import com.smartpro.smartcer.Control.RiseNumberTextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import java.util.List;

/**
 * Created by developer@gmail.com on 12/27/15 AD.
 */
public class ScaleTestsFragment extends Fragment implements FragementBecameVisible {
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

    protected ScaleIBluetoothLeService mBluetoothLeService;
    protected ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    protected BluetoothGattCharacteristic mNotifyCharacteristic;
    protected Handler mHandler;

    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;
    /*
    private BluetoothLeScanner mLEScanner;
    private ScanSettings settings;
    private List<ScanFilter> filters;
    */
    private BluetoothGatt mGatt;

    public static final String CT_UNIT = "ct";
    public static final String OZ_UNIT = "oz";
    protected String deviceAddress = "";
    protected float scaleValue = 0.0f;
    protected String scaleUnit = "ct";
    protected Boolean isIgnoreBTMessage = false;

    public static final int ANIMATION_DELAY = 400;
    protected float stoppedIndicatorValue = 0.0f;

    public static ScaleTestsFragment newInstance(Activity activity) {
        ScaleTestsFragment fragment = new ScaleTestsFragment();
        fragment.activity = (MainActivity) activity;
        return fragment;
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_scale_tests, container, false);
        viewPager = (LockableViewPager) container;

        if (MainActivity.IS_LE) {
            mHandler = new Handler();
        }

        //setConnectDescription();
        setFontFace();
        setOnClickScaleTests();
        setActionButton();
        setHold();
        setOK();
        setShowBTMsg();

        return rootView;
    }

    public void setShowBTMsg() {
        if (activity.getIsShowBTMsg()) {
            LinearLayout btMsg = (LinearLayout) rootView.findViewById(R.id.scale_bluetooth_message_panel);
            btMsg.setVisibility(View.VISIBLE);
        }
    }

    /*
    public void setConnectDescription()  {
        try {
            TextView pressToConnect = (TextView) rootView.findViewById(R.id.scale_press_to_connect);
            pressToConnect.setText(String.format(activity.bluetoothPressToConnect, ScaleIBluetoothLeService.SCALE_I_DEVICE_NAME));
        } catch (Exception ex) {
        }
    }
    */

    public void setFontFace() {
        TextView scaleWidthDigit = (TextView) rootView.findViewById(R.id.scale_width_unit_digit);
        scaleWidthDigit.setTypeface(activity.digitalFont);
        TextView scaleValueDigit = (TextView) rootView.findViewById(R.id.scale_value_digit);
        scaleValueDigit.setTypeface(activity.digitalFont);
        TextView scaleUnitDigit = (TextView) rootView.findViewById(R.id.scale_unit_digit);
        scaleUnitDigit.setTypeface(activity.digitalFont);
    }

    public void setOnClickScaleTests() {
        ResizableImageView scaleIcon  = (ResizableImageView) rootView.findViewById(R.id.scale_icon);
        scaleIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickConnect();
            }
        });

        LinearLayout blueToothPanel = (LinearLayout) rootView.findViewById(R.id.scale_bluetooth_panel);
        blueToothPanel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickConnect();
            }
        });
        Button btnConnect = (Button) rootView.findViewById(R.id.btn_scale_connect);
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
                Button btnConnect = (Button) rootView.findViewById(R.id.btn_scale_connect);
                btnConnect.setText(activity.btnConnect);
            } catch (Exception ex) {
                activity.log("resetBluetoothPort", ex.getMessage(), true);
            }
        } else {
            TextView txtStatus = (TextView) rootView.findViewById(R.id.scale_connected_status);
            if (txtStatus.getText() == "Status : Scanning...") {
                scanLeDevice(false);
                setNotConnectedDevice();
                setStableResult(false);
            } else {
                txtStatus.setText("Status : Scanning...");
                Button btnConnect = (Button) rootView.findViewById(R.id.btn_scale_connect);
                btnConnect.setText("Stop scan");
                EnableBluetooth();
            }
        }
    }

    private void EnableBluetooth() {
        //Is Bluetooth Adapter
        if (mBluetoothAdapter == null) {
            if (MainActivity.IS_LE) {
                if (!activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
                    Toast.makeText(getActivity(), activity.bluetoothNotBle, Toast.LENGTH_SHORT).show();
                    return;
                }
                BluetoothManager bluetoothManager = (BluetoothManager) activity.getSystemService(Context.BLUETOOTH_SERVICE);
                mBluetoothAdapter = bluetoothManager.getAdapter();
            } else {
                mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            }

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
        if (MainActivity.IS_LE) {
            if (mBluetoothLeService != null && deviceAddress != "") {
                mBluetoothLeService.connect(deviceAddress);
                //setConnectingDevice();
            } else {
                scanLeDevice(true);
            }
        } else {
            if (mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
                Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0);
                startActivityForResult(discoverableIntent, MainActivity.REQUEST_DISCOVERABLE_BT);
            } else {
                goToTheTask();
            }
        }
    }

    private void goToTheTask() {
        String deviceName = "";
        deviceAddress = "";

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        activity.log("size", pairedDevices.size()+"", false);

        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                activity.log("BT device", device.getName() + "(" + device.getAddress() + ")", false);
                if (device.getName().equals(ScaleIBluetoothLeService.SCALE_I_DEVICE_NAME)) {
                    deviceName = device.getName();
                    deviceAddress = device.getAddress();
                    break;
                }
            }
        }
        activity.log("Device Nm", deviceName, false);

        if (deviceName.equals(ScaleIBluetoothLeService.SCALE_I_DEVICE_NAME) && !deviceAddress.equals("")) {
            openBluetoothPort();
        } else {
            Toast.makeText(activity, String.format(activity.bluetoothIsNotPaired, ScaleIBluetoothLeService.SCALE_I_DEVICE_NAME ), Toast.LENGTH_SHORT).show();
            activity.log("Bt Device", "Device is not paired", false);
        }
    }

    private void openBluetoothPort() {
        progressDialog = ProgressDialog.show(activity, "Scale Tests", "Connecting " + ScaleIBluetoothLeService.SCALE_I_DEVICE_NAME + " ...", true);
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
                            Toast.makeText(activity, ScaleIBluetoothLeService.SCALE_I_DEVICE_NAME + " connected", Toast.LENGTH_SHORT).show();
                            activity.log("openBluetoothPort", ScaleIBluetoothLeService.SCALE_I_DEVICE_NAME + " connected (" + deviceAddress + ")", false);
                            setDataReceiver();
                        } else {
                            Toast.makeText(activity, String.format(activity.bluetoothMightBeClosed, ScaleIBluetoothLeService.SCALE_I_DEVICE_NAME ), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        }).start();
    }

    private void setConnectedDevice() {
        this.isBTConnected = true;
        TextView txtStatus = (TextView) rootView.findViewById(R.id.scale_connected_status);
        txtStatus.setTextColor(activity.colorConnected);
        txtStatus.setText(activity.bluetoothConnected);
        Button btnConnect = (Button) rootView.findViewById(R.id.btn_scale_connect);
        btnConnect.setText(activity.btnDisconnect);
        activity.log("onServiceConnected", "Connected", false);
    }

    private void setNotConnectedDevice() {
        this.isBTConnected = false;
        TextView txtStatus = (TextView) rootView.findViewById(R.id.scale_connected_status);
        txtStatus.setTextColor(activity.colorText);
        txtStatus.setText(activity.bluetoothNotConnected);
        Button btnConnect = (Button) rootView.findViewById(R.id.btn_scale_connect);
        btnConnect.setText(activity.btnConnect);
        activity.log("onServiceDisconnected","Disconnected", false);
    }

    protected void resetBluetoothPort() throws IOException {
        if (MainActivity.IS_LE){
            mBluetoothLeService.disconnect();
        } else {
            stopWorker = true;
            if (mmOutputStream != null) {
                mmOutputStream.close();
                mmOutputStream = null;
            }
            if (mmInputStream != null) {
                mmInputStream.close();
                mmInputStream = null;
            }
            if (mmSocket != null) {
                mmSocket.close();
                mmSocket = null;
            }
            setNotConnectedDevice();
        }
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
            if (data != null && activity.getCurrentPageShowing() == PagerAdapter.PAGE_SCALE_TESTS_DUMMY) {
                Button btnHold = (Button) rootView.findViewById(R.id.btn_scale_hold);
                if (btnHold.getText().equals("Hold")) {
                    isIgnoreBTMessage = true;

                    ScaleIMessage device = new ScaleIMessage();
                    ScaleIMessage.Message message = device.getMessage(data);

                    if (message instanceof ScaleIMessage.BatteryMessage) {
                        displayBattery((ScaleIMessage.BatteryMessage) message);
                        displayBTMessage("Low Msg:", data);
                    } else if (message instanceof ScaleIMessage.TestResultsMessage) {
                        displayResult((ScaleIMessage.TestResultsMessage) message);
                        displayBTMessage("Rest Msg:", data);
                    } else {
                        displayBTMessage("Unk Msg:", data);
                    }

                    isIgnoreBTMessage = false;
                }
            }
        }
    }

    public void displayBTMessage(String title, String desc) {
        TextView txtBTMsg = (TextView) rootView.findViewById(R.id.scale_bluetooth_message);
        txtBTMsg.setText(title + " " + desc);

        activity.log(title, desc, false);
    }

    public void displayBattery(ScaleIMessage.BatteryMessage message) {
        ResizableImageView batteryIcon = (ResizableImageView) rootView.findViewById(R.id.scale_battery_icon);

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

    private void displayResult(ScaleIMessage.TestResultsMessage message) {
        setDigital(message.value, false);
        setDigitalUnit(message.unit);
        setStableResult(true);
        playIndicatorAnimation(message.value);
        //setScaleResultText(message.value, message.unit, message.isStable);
    }

    private void setStableResult(boolean isStableResult) {
        if (isStableResult) {
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                public void run() {
                    ResizableImageView icon = (ResizableImageView) rootView.findViewById(R.id.scale_stable_icon);
                    icon.setImageResource(R.drawable.stable_in);

                    TextView text = (TextView) rootView.findViewById(R.id.scale_stable_text);
                    text.setTextColor(activity.colorText);
                    text.setBackgroundResource(R.drawable.textview_rounded_corner_stable_in);
                }
            }, ANIMATION_DELAY);
        } else {
            ResizableImageView i = (ResizableImageView) rootView.findViewById(R.id.scale_stable_icon);
            i.setImageResource(R.drawable.stable_out);
            TextView text = (TextView) rootView.findViewById(R.id.scale_stable_text);
            text.setTextColor(activity.colorText);
            text.setBackgroundResource(R.drawable.textview_rounded_corner_stable_out);
        }
    }

    private void playIndicatorAnimation(float value) {
        ResizableImageView indicator = (ResizableImageView) rootView.findViewById(R.id.scale_panel);
        int img = R.drawable.scale_panel_1;
        if (value <= 1) {
            img = R.drawable.scale_panel_2;
        } else if (value <= 2) {
            img = R.drawable.scale_panel_3;
        } else if (value <= 3) {
            img = R.drawable.scale_panel_4;
        } else if (value <= 4) {
            img = R.drawable.scale_panel_5;
        } else if (value <= 5) {
            img = R.drawable.scale_panel_6;
        } else if (value <= 6) {
            img = R.drawable.scale_panel_7;
        } else if (value <= 7) {
            img = R.drawable.scale_panel_8;
        } else if (value <= 8) {
            img = R.drawable.scale_panel_9;
        } else if (value <= 9) {
            img = R.drawable.scale_panel_10;
        } else {
            img = R.drawable.scale_panel_11;
        }
        indicator.setImageResource(img);
    }

    private void setDigital(float value, Boolean isAnimation) {
        RiseNumberTextView digitalValue = (RiseNumberTextView) rootView.findViewById(R.id.scale_value_digit);
        digitalValue.fnum = new DecimalFormat("##0.0000");
        if (isAnimation) {
            digitalValue.withNumber(value).start();
        } else {
            digitalValue.setFloat(value);
        }
    }

    private void setDigitalUnit(String unit) {
        TextView txtUnit = (TextView) rootView.findViewById(R.id.scale_unit_digit);
        txtUnit.setText(unit);
    }

    public void setActionButton() {
        ResizableImageView actionDaimond = (ResizableImageView) rootView.findViewById(R.id.action_reader_tests);
        actionDaimond.setOnClickListener(new View.OnClickListener() {
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
        ResizableImageView actionGemstone = (ResizableImageView) rootView.findViewById(R.id.action_gemstone_tests);
        actionGemstone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                viewPager.goToGemStoneTestsView();
            }
        });
        ResizableImageView actionGauge = (ResizableImageView) rootView.findViewById(R.id.action_gague_tests);
        actionGauge.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                viewPager.goToGaugeTestsView();
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

    private void setHold() {
        Button btnHold = (Button) rootView.findViewById(R.id.btn_scale_hold);
        btnHold.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                setHoldResultText();
            }
        });
    }

    public void setHoldResultText() {
        Button btnHold = (Button) rootView.findViewById(R.id.btn_scale_hold);
        if (btnHold.getText().equals("Hold")) {
            isIgnoreBTMessage = true;

            TextView txtValue = (TextView) rootView.findViewById(R.id.scale_value_digit);
            TextView txtUnit = (TextView) rootView.findViewById(R.id.scale_unit_digit);

            Float value = Float.parseFloat(txtValue.getText().toString());
            String unit = txtUnit.getText().toString();

            if (value == 0 || unit.equals("--")) {
                isIgnoreBTMessage = false;
                return;
            }
            setStableResult(false);
            scaleValue = value;
            scaleUnit = unit;

            String textToSpeech = "";
            if (unit.equals(CT_UNIT)) {
                textToSpeech = textToSpeech + " carat";
            } else if (unit.equals(OZ_UNIT)) {
                textToSpeech = textToSpeech + " ounce";
            }
            btnHold.setText("Unhold");
            activity.speak(textToSpeech);
        } else {
            btnHold.setText("Hold");
            isIgnoreBTMessage = false;
        }
    }

    private void setOK() {
        Button btnOK = (Button) rootView.findViewById(R.id.btnOK);
        btnOK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Button btnHold = (Button) rootView.findViewById(R.id.btn_scale_hold);
                if (btnHold.getText().equals("Unhold")) {
                    if (scaleValue == 0.0f) {
                        AlertDialog.Builder alert = new AlertDialog.Builder(activity);
                        alert.setTitle("Scale Tests");
                        alert.setMessage("Have no test recording.\nPlease continue to record.");
                        alert.setPositiveButton("OK", new AlertDialog.OnClickListener() {
                            public void onClick(DialogInterface dialog, int arg1) {
                                isIgnoreBTMessage = false;
                            }
                        });
                        alert.show();
                    } else {
                        //Add cert
                        ScaleTestsModel model = new ScaleTestsModel(true, ScaleIBluetoothLeService.SCALE_I_DEVICE_NAME, scaleValue, scaleUnit);
                        activity.setScaleTestsModel(model);
                        Toast.makeText(activity, "Add to Certificate", Toast.LENGTH_SHORT).show();
                        activity.log("OK", "Add to cer", false);
                    }
                } else {
                    isIgnoreBTMessage = true;
                    AlertDialog.Builder alert = new AlertDialog.Builder(activity);
                    alert.setTitle("Scale Tests");
                    alert.setMessage("The result movement is very sensitive. Please hold result first, then add cert.");
                    alert.setPositiveButton("Ok", new AlertDialog.OnClickListener() {
                        public void onClick(DialogInterface dialog, int arg1) {
                            isIgnoreBTMessage = false;
                        }
                    });
                    alert.show();
                }
            }
        });
    }

    @Override
    public void onAttach(final Activity activity) {
        super.onAttach(activity);
        setHasOptionsMenu(true);
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

                    if (device.getName() != null && device.getName().equals(ScaleIBluetoothLeService.SCALE_I_DEVICE_NAME)) {
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
            if (device.getName() != null && device.getName().equals(ScaleIBluetoothLeService.SCALE_I_DEVICE_NAME)) {
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
        Intent gattServiceIntent = new Intent(activity, ScaleIBluetoothLeService.class);
        activity.bindService(gattServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);

        activity.registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
    }


    @Override
    public void onResume() {
        super.onResume();
        activity.log("onResume","diamond", false);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (MainActivity.IS_LE) {
        }
        activity.log("onPause","diamond", false);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (MainActivity.IS_LE && isBTConnected) {
            activity.unbindService(mServiceConnection);
            activity.unregisterReceiver(mGattUpdateReceiver);
            mBluetoothLeService = null;
            mGatt.close();
            mGatt = null;
        }
    }

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((ScaleIBluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Toast.makeText(activity, String.format(activity.bluetoothMightBeClosed, ScaleIBluetoothLeService.SCALE_I_DEVICE_NAME ), Toast.LENGTH_SHORT).show();
                activity.log("onServiceConnected","device may be closed", false);
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(deviceAddress);
            setConnectedDevice();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;

            setNotConnectedDevice();
        }
    };

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (ScaleIBluetoothLeService.ACTION_SCALE_I_GATT_CONNECTED.equals(action)) {
                isBTConnected = true;
                setConnectedDevice();
                Button btnConnect = (Button) rootView.findViewById(R.id.btn_scale_connect);
                btnConnect.setText(activity.btnDisconnect);
            } else if (ScaleIBluetoothLeService.ACTION_SCALE_I_GATT_DISCONNECTED.equals(action)) {
                isBTConnected = false;
                setNotConnectedDevice();
                activity.unbindService(mServiceConnection);
                activity.unregisterReceiver(mGattUpdateReceiver);
            } else if (ScaleIBluetoothLeService.ACTION_SCALE_I_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                mBluetoothLeService.displayGattServices();

            } else if (ScaleIBluetoothLeService.ACTION_SCALE_I_DATA_AVAILABLE.equals(action)) {
                displayData(intent.getStringExtra(ScaleIBluetoothLeService.EXTRA_DATA_SCALE_I));
            }
        }
    };

    private void displayData(String data) {
        if (data != null &&
                activity.getCurrentPageShowing() == PagerAdapter.PAGE_SCALE_TESTS_DUMMY &&
                isIgnoreBTMessage == false) {
            displayBluetoothMessage(data);
        }
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ScaleIBluetoothLeService.ACTION_SCALE_I_GATT_CONNECTED);
        intentFilter.addAction(ScaleIBluetoothLeService.ACTION_SCALE_I_GATT_DISCONNECTED);
        intentFilter.addAction(ScaleIBluetoothLeService.ACTION_SCALE_I_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(ScaleIBluetoothLeService.ACTION_SCALE_I_DATA_AVAILABLE);
        return intentFilter;
    }
}
