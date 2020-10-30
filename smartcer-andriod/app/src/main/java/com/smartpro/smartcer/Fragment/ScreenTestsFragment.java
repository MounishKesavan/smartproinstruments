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
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.smartpro.smartcer.Bluetooth.ScreenIBluetoothLeService;
import com.smartpro.smartcer.BluetoothMessage.ScreenIMessage;
import com.smartpro.smartcer.Control.LockableViewPager;
import com.smartpro.smartcer.Activity.MainActivity;
import com.smartpro.smartcer.Model.DiamondTestsModel;
import com.smartpro.smartcer.Control.PagerAdapter;
import com.smartpro.smartcer.Model.GaugeTestsModel;
import com.smartpro.smartcer.Model.ScaleTestsModel;
import com.smartpro.smartcer.R;
import com.smartpro.smartcer.Control.ResizableImageView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;
import java.util.List;

/**
 * Created by developer@gmail.com on 2/8/16 AD.
 */
public class ScreenTestsFragment extends Fragment implements FragementBecameVisible {
    protected MainActivity activity;
    protected View rootView;
    protected LockableViewPager viewPager;
    protected ProgressDialog progressDialog;
    protected AlertDialog dialog;

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

    protected ScreenIBluetoothLeService mBluetoothLeService;
    protected Handler mHandler;

    protected String deviceAddress = "";
    protected int counter = 0;
    protected String[] testResult = new String[5];
    protected String[] screenIStrings;
    protected Boolean isIgnoreBTMessage = false;

    public static ScreenTestsFragment newInstance(Activity activity) {
        ScreenTestsFragment fragment = new ScreenTestsFragment();
        fragment.activity = (MainActivity) activity;
        return fragment;
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_screen_tests, container, false);
        viewPager = (LockableViewPager) container;
        mHandler = new Handler();

        setOnClickScreenTests();
        setActionButton();
        setOK();
        setNewTest();
        setScreenIStrings();
        setShowBTMsg();

        return rootView;
    }

    public void setShowBTMsg() {
        if (activity.getIsShowBTMsg()) {
            LinearLayout btMsg = (LinearLayout) rootView.findViewById(R.id.screen_bluetooth_message_panel);
            btMsg.setVisibility(View.VISIBLE);
        }
    }

    public void setScreenIStrings() {
        screenIStrings = activity.getResources().getStringArray(R.array.screen_test_result_array);
    }

    public void setOnClickScreenTests() {
        ResizableImageView screenIcon  = (ResizableImageView) rootView.findViewById(R.id.screen_icon);
        screenIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickConnect();
            }
        });

        LinearLayout blueToothPanel = (LinearLayout) rootView.findViewById(R.id.screen_bluetooth_panel);
        blueToothPanel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickConnect();
            }
        });
        Button btnConnect = (Button) rootView.findViewById(R.id.btn_screen_connect);
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
            TextView txtStatus = (TextView) rootView.findViewById(R.id.screen_connected_status);
            if (txtStatus.getText() == "Status : Scanning...") {
                scanLeDevice(false);
                setNotConnectedDevice();
            } else {
                txtStatus.setText("Status : Scanning...");
                Button btnConnect = (Button) rootView.findViewById(R.id.btn_screen_connect);
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
                if (device.getName().equals(ScreenIBluetoothLeService.SCREEN_I_DEVICE_NAME)) {
                    deviceName = device.getName();
                    deviceAddress = device.getAddress();
                    break;
                }
            }
        }
        activity.log("Device Nm", deviceName, false);

        if (deviceName.equals(ScreenIBluetoothLeService.SCREEN_I_DEVICE_NAME) && !deviceAddress.equals("")) {
            openBluetoothPort();
        } else {
            Toast.makeText(activity, String.format(activity.bluetoothIsNotPaired, ScreenIBluetoothLeService.SCREEN_I_DEVICE_NAME ), Toast.LENGTH_SHORT).show();
            activity.log("Bt Device", "Device is not paired", false);
        }
    }

    private void openBluetoothPort() {
        progressDialog = ProgressDialog.show(activity, "Screen-I Tests", "Connecting " + ScreenIBluetoothLeService.SCREEN_I_DEVICE_NAME + " ...", true);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    //if (mmSocket == null) {
                    mmDevice = mBluetoothAdapter.getRemoteDevice(deviceAddress);
                    UUID uuid = UUID.fromString("00002a05-0000-1000-8000-00805f9b23fb");
                    mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
                    mmSocket.connect();
                    activity.log("BLE connected", "OK", false);
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
                            setConnectedDevice();
                            setDataReceiver();
                        } else {
                            Toast.makeText(activity, String.format(activity.bluetoothMightBeClosed, ScreenIBluetoothLeService.SCREEN_I_DEVICE_NAME ), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        }).start();
    }

    private void setConnectedDevice() {
        this.isBTConnected = true;
        TextView txtStatus = (TextView) rootView.findViewById(R.id.screen_connected_status);
        txtStatus.setTextColor(activity.colorConnected);
        txtStatus.setText(activity.bluetoothConnected);
        Button btnConnect = (Button) rootView.findViewById(R.id.btn_screen_connect);
        btnConnect.setText(activity.btnDisconnect);
        activity.log("onServiceConnected", "Connected", false);
    }

    private void setNotConnectedDevice() {
        this.isBTConnected = false;
        TextView txtStatus = (TextView) rootView.findViewById(R.id.screen_connected_status);
        txtStatus.setTextColor(activity.colorText);
        txtStatus.setText(activity.bluetoothNotConnected);
        Button btnConnect = (Button) rootView.findViewById(R.id.btn_screen_connect);
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
            if (data != null && activity.getCurrentPageShowing() == PagerAdapter.PAGE_SCREEN_TESTS &&
                    ((dialog == null) || (dialog != null && !dialog.isShowing()))) {
                isIgnoreBTMessage = true;

                ScreenIMessage device = new ScreenIMessage();
                ScreenIMessage.Message message = device.getMessage(data);

                if (message instanceof ScreenIMessage.BatteryMessage) {
                    displayBattery((ScreenIMessage.BatteryMessage) message);
                    displayBTMessage("Low Msg:", data);
                } else if (message instanceof ScreenIMessage.TestResultsMessage) {
                    displayResult((ScreenIMessage.TestResultsMessage) message);
                    displayBTMessage("Rst Msg:", data);
                } else if (message instanceof ScreenIMessage.ReadyMessage) {
                    setIndicatorAnimationToReady(false);
                }
                isIgnoreBTMessage = false;
            }
        }
    }

    public void displayBTMessage(String title, String desc) {
        TextView txtBTMsg = (TextView) rootView.findViewById(R.id.screen_bluetooth_message);
        txtBTMsg.setText(title + " " + desc);
        activity.log(title, desc, false);
    }

    public void displayBattery(ScreenIMessage.BatteryMessage message) {
        ResizableImageView batteryIcon = (ResizableImageView) rootView.findViewById(R.id.screen_battery_icon);
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

    private void displayResult(ScreenIMessage.TestResultsMessage message) {
        if (counter >= 5) {
            resetAllTestsResults();
        }

        if (message.value.equals("Tra")) {
            playIndicatorAnimation(message.value);
            return;
        }

        counter++;
        testResult[counter-1] = message.value;
        setDiamondResultText(message.value);
        playIndicatorAnimation(message.value);

        Button btnAddCert = (Button) rootView.findViewById(R.id.btnOK);
        btnAddCert.setBackgroundResource(counter == 5 ? R.drawable.button_selector_pressable : R.drawable.button_selector_disable);
        Button btnClear = (Button) rootView.findViewById(R.id.btnNewTest);
        btnClear.setBackgroundResource(counter == 5 ? R.drawable.button_selector_pressable : R.drawable.button_selector_disable);

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                setIndicatorAnimationToReady(false);
            }
        }, 2500);
    }

    private void setDiamondResultText(String result) {
        LinearLayout panel =  (LinearLayout) rootView.findViewById(R.id.screen_result_panel);
        panel.setVisibility(View.VISIBLE);

        TextView txtView;
        if (counter == 1) {
            txtView = (TextView) rootView.findViewById(R.id.test_results_1);
            txtView.setText(String.format(activity.diamondResult, counter, getScreenIText(result)));
        } else if (counter == 2) {
            txtView = (TextView) rootView.findViewById(R.id.test_results_2);
            txtView.setText(String.format(activity.diamondResult, counter, getScreenIText(result)));
        } else if (counter == 3) {
            txtView = (TextView) rootView.findViewById(R.id.test_results_3);
            txtView.setText(String.format(activity.diamondResult, counter, getScreenIText(result)));
        } else if (counter == 4) {
            txtView = (TextView) rootView.findViewById(R.id.test_results_4);
            txtView.setText(String.format(activity.diamondResult, counter, getScreenIText(result)));
        } else if (counter == 5) {
            txtView = (TextView) rootView.findViewById(R.id.test_results_5);
            txtView.setText(String.format(activity.diamondResult, counter, getScreenIText(result)));
        }
    }

    private String getScreenIText(String abbr) {
        if (abbr.equals("Dia")) {
            return screenIStrings[0];
        } else if (abbr.equals("CVD")) {
            return screenIStrings[1];
        } else if (abbr.equals("Tra")) {
            return screenIStrings[2];
        } else {
            return "";
        }
    }

    private void setIndicatorAnimationToNone() {
        ResizableImageView indicator =  (ResizableImageView) rootView.findViewById(R.id.screen_indicator);
        indicator.setImageResource(R.drawable.screen_indicator_idle);
    }

    private void setIndicatorAnimationToReady(Boolean isSpeak) {
        ResizableImageView indicator =  (ResizableImageView) rootView.findViewById(R.id.screen_indicator);
        indicator.setImageResource(R.drawable.screen_indicator_ready);
        if (isSpeak) {
            activity.speak("Ready");
        }
    }

    public void playIndicatorAnimation(String abbr) {
        setIndicatorAnimationToNone();

        Handler handler = new Handler();
        //5:Dia;, 5:CVD;, 5:Tra;
        if (abbr.equals("Dia")) {
            handler.postDelayed(new Runnable() {
                public void run() {
                    ResizableImageView indicator =  (ResizableImageView) rootView.findViewById(R.id.screen_indicator);
                    indicator.setImageResource(R.drawable.screen_indicator_diamond);
                    activity.speak(getScreenIText("Dia"));
                }
            }, 100);
        } else if (abbr.equals("CVD")) {
            handler.postDelayed(new Runnable() {
                public void run() {
                    ResizableImageView indicator =  (ResizableImageView) rootView.findViewById(R.id.screen_indicator);
                    indicator.setImageResource(R.drawable.screen_indicator_cvd);
                    activity.speak("C V D or H P H T");
                }
            }, 100);
        } else if (abbr.equals("Tra")) {
            handler.postDelayed(new Runnable() {
                public void run() {
                    ResizableImageView indicator =  (ResizableImageView) rootView.findViewById(R.id.screen_indicator);
                    indicator.setImageResource(R.drawable.screen_indicator_transparent);
                    activity.speak(getScreenIText("Tra"));
                }
            }, 100);
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
        ResizableImageView actionGemStone = (ResizableImageView) rootView.findViewById(R.id.action_gemstone_tests);
        actionGemStone.setOnClickListener(new View.OnClickListener() {
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
                addToCertification();
            }
        });
    }

    private void setNewTest() {
        Button btnNewTest = (Button) rootView.findViewById(R.id.btnNewTest);
        btnNewTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                AlertDialog.Builder alert = new AlertDialog.Builder(activity);
                alert.setTitle("Screen-I Tests");
                alert.setMessage("Do you want to start new tests?");
                alert.setNegativeButton("Cancel", null);
                alert.setPositiveButton("Ok", new AlertDialog.OnClickListener() {
                    public void onClick(DialogInterface dialog, int arg1) {
                        if (isBTConnected) {
                            setIndicatorAnimationToReady(false);
                        } else {
                            setIndicatorAnimationToNone();
                        }
                        resetAllTestsResults();
                        isIgnoreBTMessage = false;
                    }
                });
                dialog = alert.create();
                dialog.show();
            }
        });
    }

    private boolean isAllResultsAre(String materail) {
        int diamondCounter = 0;
        for (int i = 0; i < testResult.length; i++) {
            activity.log("save","i "+ i + ", value "+testResult[i], false);
            if (testResult[i] != null && testResult[i].equals(materail)) {
                diamondCounter = diamondCounter + 1;
                activity.log("save yes","i "+ i + ", value "+testResult[i], false);
            } else {
                activity.log("save no","i "+ i + ", value "+testResult[i], false);
            }
        }
        return diamondCounter == 5;
    }

    private void addToCertification() {
        isIgnoreBTMessage = true;

        if (counter < 5) {
            AlertDialog.Builder alert = new AlertDialog.Builder(activity);
            alert.setTitle("Screen-I Tests");
            alert.setMessage("Record tests results "+counter+" / 5.\nPlease continue to record.");
            alert.setPositiveButton("OK", new AlertDialog.OnClickListener() {
                public void onClick(DialogInterface dialog, int arg1) {
                    isIgnoreBTMessage = false;
                }
            });
            dialog = alert.create();
            dialog.show();
        } else {
            if (isAllResultsAre("Dia")) {
                //Add Cert
                DiamondTestsModel model = new DiamondTestsModel(true, "Screen-I", "Diamond");
                activity.setDiamondTestResult(model);

                Toast.makeText(activity, "Added Screen-I to diamond certificate", Toast.LENGTH_SHORT).show();
                activity.log("OK", "Add diamond to cer", false);
                viewPager.goToCertificationView();
            } else if (isAllResultsAre("CVD")) {

                if (activity.getDiamondCerModel().IsPrimoTests()) {
                    AlertDialog.Builder alert = new AlertDialog.Builder(activity);
                    alert.setTitle("Add CVD/HPHT to cert");
                    alert.setMessage("To continue, the measurement, weight of Primo-I will be clear. Please measure, weight again.");
                    alert.setNegativeButton("Cancel", null);
                    alert.setPositiveButton("Ok", new AlertDialog.OnClickListener() {
                        public void onClick(DialogInterface dialog, int arg1) {
                            activity.setGaugeTestsModel(new GaugeTestsModel(false,"", 0, 0, 0, "mm"));
                            activity.setScaleTestsModel(new ScaleTestsModel(false,"", 0, "ct"));

                            DiamondTestsModel model = new DiamondTestsModel(true, "Screen-I", "CVD/HPHT");
                            activity.setDiamondTestResult(model);

                            Toast.makeText(activity, "Added Screen-I to CVD/HPHT certificate", Toast.LENGTH_SHORT).show();
                            activity.log("OK", "Add CVD/HPHT to cer", false);
                            viewPager.goToCertificationView();
                        }
                    });
                    alert.show();
                } else {
                    //Add Cert
                    DiamondTestsModel model = new DiamondTestsModel(true, "Screen-I", "CVD/HPHT");
                    activity.setDiamondTestResult(model);

                    Toast.makeText(activity, "Added Screen-I to CVD/HPHT certificate", Toast.LENGTH_SHORT).show();
                    activity.log("OK", "Add CVD/HPHT to cer", false);
                    viewPager.goToCertificationView();
                }

            } else {
                AlertDialog.Builder alert = new AlertDialog.Builder(activity);
                alert.setTitle("Screen-I Tests");
                alert.setMessage("Tests Results are not identical or result is \"too transparent\".\nDo you want to start new tests?");
                alert.setNegativeButton("Cancel", new AlertDialog.OnClickListener() {
                    public void onClick(DialogInterface dialog, int arg1) {
                        isIgnoreBTMessage = false;
                    }
                });
                alert.setPositiveButton("Ok", new AlertDialog.OnClickListener() {
                    public void onClick(DialogInterface dialog, int arg1) {
                        setIndicatorAnimationToNone();
                        resetAllTestsResults();
                        isIgnoreBTMessage = false;
                    }
                });
                isIgnoreBTMessage = true;
                dialog = alert.create();
                dialog.show();
            }
        }
    }

    private void resetAllTestsResults() {
        testResult = new String[5];
        counter = 0;
        TextView txtView1 = (TextView) rootView.findViewById(R.id.test_results_1);
        txtView1.setText(String.format(activity.diamondResult, 1, "-"));
        TextView txtView2 = (TextView) rootView.findViewById(R.id.test_results_2);
        txtView2.setText(String.format(activity.diamondResult, 2, "-"));
        TextView txtView3 = (TextView) rootView.findViewById(R.id.test_results_3);
        txtView3.setText(String.format(activity.diamondResult, 3, "-"));
        TextView txtView4 = (TextView) rootView.findViewById(R.id.test_results_4);
        txtView4.setText(String.format(activity.diamondResult, 4, "-"));
        TextView txtView5 = (TextView) rootView.findViewById(R.id.test_results_5);
        txtView5.setText(String.format(activity.diamondResult, 5, "-"));

        Button btnAddCert = (Button) rootView.findViewById(R.id.btnOK);
        btnAddCert.setBackgroundResource(R.drawable.button_selector_disable);
        Button btnClear = (Button) rootView.findViewById(R.id.btnNewTest);
        btnClear.setBackgroundResource(R.drawable.button_selector_disable);
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

                            if (device.getName() != null && device.getName().equals(ScreenIBluetoothLeService.SCREEN_I_DEVICE_NAME)) {
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
            if (device.getName() != null && device.getName().equals(ScreenIBluetoothLeService.SCREEN_I_DEVICE_NAME)) {
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
        Intent gattServiceIntent = new Intent(activity, ScreenIBluetoothLeService.class);
        activity.bindService(gattServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
        activity.registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
    }

    @Override
    public void onResume() {
        super.onResume();
        activity.log("onResume","screen", false);
    }

    @Override
    public void onPause() {
        super.onPause();
        scanLeDevice(false);

        activity.log("onPause","screen", false);
    }

    @Override
    public void onDestroy() {
        if (isBTConnected) {
            activity.unbindService(mServiceConnection);
            activity.unregisterReceiver(mGattUpdateReceiver);
            mBluetoothLeService = null;
        }
        super.onDestroy();
    }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((ScreenIBluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Toast.makeText(activity, String.format(activity.bluetoothMightBeClosed, ScreenIBluetoothLeService.SCREEN_I_DEVICE_NAME ), Toast.LENGTH_SHORT).show();
                activity.log("onServiceConnected","device may be closed", false);
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(deviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
            //setNotConnectedDevice();
        }
    };

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (ScreenIBluetoothLeService.ACTION_SCREEN_I_GATT_CONNECTED.equals(action)) {
                setConnectedDevice();
                setIndicatorAnimationToReady(true);
            } else if (ScreenIBluetoothLeService.ACTION_SCREEN_I_GATT_DISCONNECTED.equals(action)) {
                setNotConnectedDevice();
                setIndicatorAnimationToNone();
                activity.unbindService(mServiceConnection);
                activity.unregisterReceiver(mGattUpdateReceiver);
            } else if (ScreenIBluetoothLeService.ACTION_SCREEN_I_GATT_SERVICES_DISCOVERED.equals(action)) {
                mBluetoothLeService.displayGattServices();
            } else if (ScreenIBluetoothLeService.ACTION_SCREEN_I_DATA_AVAILABLE.equals(action)) {
                displayBluetoothMessage(intent.getStringExtra(ScreenIBluetoothLeService.EXTRA_DATA_SCREEN_I));
            }
        }
    };

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ScreenIBluetoothLeService.ACTION_SCREEN_I_GATT_CONNECTED);
        intentFilter.addAction(ScreenIBluetoothLeService.ACTION_SCREEN_I_GATT_DISCONNECTED);
        intentFilter.addAction(ScreenIBluetoothLeService.ACTION_SCREEN_I_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(ScreenIBluetoothLeService.ACTION_SCREEN_I_DATA_AVAILABLE);
        return intentFilter;
    }
}
