package com.smartpro.smartcer.Fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
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

import com.smartpro.smartcer.Bluetooth.GaugeIBluetoothLeService;
import com.smartpro.smartcer.BluetoothMessage.GaugeIMessage;
import com.smartpro.smartcer.BluetoothMessage.GaugeHelperMessage;
import com.smartpro.smartcer.Control.LockableViewPager;
import com.smartpro.smartcer.Activity.MainActivity;
import com.smartpro.smartcer.Model.GaugeTestsModel;
import com.smartpro.smartcer.Control.PagerAdapter;
import com.smartpro.smartcer.Model.ScaleTestsModel;
import com.smartpro.smartcer.R;
import com.smartpro.smartcer.Control.ResizableImageView;
import com.smartpro.smartcer.Control.RiseNumberTextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;
import java.util.List;

/**
 * Created by developer@gmail.com on 12/27/15 AD.
 */
public class GaugeTestsFragment extends Fragment implements FragementBecameVisible {
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

    protected GaugeIBluetoothLeService mBluetoothLeService;
    protected Handler mHandler;

    public enum GaugeMode {
        Weight
        , Inner
        , Outer
        , Unspecific
    }

    public GaugeMode viewMode = GaugeMode.Weight;
    protected String deviceAddress = "";
    protected String material = "";
    protected String shape = "";
    protected String gravity = "";
    protected float widthValue = 0.0f;
    protected String widthUnit = "";
    protected float lengthValue = 0.0f;
    protected String lengthUnit = "";
    protected float depthValue = 0.0f;
    protected String depthUnit = "";
    protected float weightValue = 0.0f;
    protected String weightUnit = "";
    protected Boolean isIgnoreBTMessage = false;
    protected Boolean isHoldMessage = false;
    protected GaugeHelperMessage helperMessage;

    public static final String MM_UNIT = "mm";
    public static final String INCH_UNIT = "in";

    public static GaugeTestsFragment newInstance(Activity activity) {
        GaugeTestsFragment fragment = new GaugeTestsFragment();
        fragment.activity = (MainActivity) activity;
        return fragment;
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_gauge_tests, container, false);
        viewPager = (LockableViewPager) container;
        mHandler = new Handler();
        helperMessage = new GaugeHelperMessage();

        setFontFace();
        setOnClickGaugeTests();
        setActionButton();
        setOK();
        setNewTest();
        setShowBTMsg();

        return rootView;
    }

    public void setShowBTMsg() {
        if (activity.getIsShowBTMsg()) {
            LinearLayout btMsg = (LinearLayout) rootView.findViewById(R.id.gauge_bluetooth_message_panel);
            btMsg.setVisibility(View.VISIBLE);
        }
    }

    public void setFontFace(){
        TextView gaugeValue = (TextView) rootView.findViewById(R.id.gauge_value);
        gaugeValue.setTypeface(activity.digitalFont);
        TextView gaugeWeight = (TextView) rootView.findViewById(R.id.gauge_weight_value);
        gaugeWeight.setTypeface(activity.digitalFont);
    }

    public void setOnClickGaugeTests() {
        ResizableImageView gaugeIcon = (ResizableImageView) rootView.findViewById(R.id.gauge_icon);
        gaugeIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickConnect();
            }
        });

        LinearLayout blueToothPanel = (LinearLayout) rootView.findViewById(R.id.gauge_bluetooth_panel);
        blueToothPanel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickConnect();
            }
        });
        Button btnConnect = (Button) rootView.findViewById(R.id.btn_gauge_connect);
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
            TextView txtStatus = (TextView) rootView.findViewById(R.id.gauge_connected_status);
            if (txtStatus.getText() == "Status : Scanning...") {
                scanLeDevice(false);
                setNotConnectedDevice();
            } else {
                txtStatus.setText("Status : Scanning...");
                Button btnConnect = (Button) rootView.findViewById(R.id.btn_gauge_connect);
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
                if (device.getName().equals(GaugeIBluetoothLeService.GAGUE_I_DEVICE_NAME)) {
                    deviceName = device.getName();
                    deviceAddress = device.getAddress();
                    break;
                }
            }
        }
        activity.log("Device Nm", deviceName, false);

        if (deviceName.equals(GaugeIBluetoothLeService.GAGUE_I_DEVICE_NAME) && !deviceAddress.equals("")) {
            openBluetoothPort();
        } else {
            Toast.makeText(activity, String.format(activity.bluetoothIsNotPaired, GaugeIBluetoothLeService.GAGUE_I_DEVICE_NAME ), Toast.LENGTH_SHORT).show();
            activity.log("Bt Device", "Device is not paired", false);
        }
    }

    private void openBluetoothPort() {
        progressDialog = ProgressDialog.show(activity, "Primo-I Tests", "Connecting " + GaugeIBluetoothLeService.GAGUE_I_DEVICE_NAME + " ...", true);
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
                            Toast.makeText(activity, GaugeIBluetoothLeService.GAGUE_I_DEVICE_NAME + " connected", Toast.LENGTH_SHORT).show();
                            activity.log("openBluetoothPort", GaugeIBluetoothLeService.GAGUE_I_DEVICE_NAME + " connected (" + deviceAddress + ")", false);
                            setDataReceiver();
                        } else {
                            Toast.makeText(activity, String.format(activity.bluetoothMightBeClosed, GaugeIBluetoothLeService.GAGUE_I_DEVICE_NAME ), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        }).start();
    }

    private void setConnectedDevice() {
        this.isBTConnected = true;
        TextView txtStatus = (TextView) rootView.findViewById(R.id.gauge_connected_status);
        txtStatus.setTextColor(activity.colorConnected);
        txtStatus.setText(activity.bluetoothConnected);
        Button btnConnect = (Button) rootView.findViewById(R.id.btn_gauge_connect);
        btnConnect.setText(activity.btnDisconnect);
        activity.log("onServiceConnected", "Connected", false);
    }

    private void setNotConnectedDevice() {
        this.isBTConnected = false;
        TextView txtStatus = (TextView) rootView.findViewById(R.id.gauge_connected_status);
        txtStatus.setTextColor(activity.colorText);
        txtStatus.setText(activity.bluetoothNotConnected);
        Button btnConnect = (Button) rootView.findViewById(R.id.btn_gauge_connect);
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
            if (data != null && activity.getCurrentPageShowing() == PagerAdapter.PAGE_GAUGE_TESTS) {
                isIgnoreBTMessage = true;

                GaugeIMessage device = new GaugeIMessage();
                GaugeIMessage.Message message = device.getMessage(data);

                if (message instanceof GaugeIMessage.LengthMessage) {
                    displayLength((GaugeIMessage.LengthMessage) message);
                } else if (message instanceof GaugeIMessage.WidthMessage) {
                    displayWidth((GaugeIMessage.WidthMessage) message);
                } else if (message instanceof GaugeIMessage.DepthMessage) {
                    displayDepth((GaugeIMessage.DepthMessage) message);
                } else if (message instanceof GaugeIMessage.WeightMessage) {
                    displayWeight((GaugeIMessage.WeightMessage) message);
                } else if (message instanceof GaugeIMessage.WeightDetailsMessage) {
                    displayWeightDetails((GaugeIMessage.WeightDetailsMessage) message);
                } else if (message instanceof GaugeIMessage.InnerDetailsMessage) {
                    displayInnerMeasurement((GaugeIMessage.InnerDetailsMessage) message);
                } else if (message instanceof GaugeIMessage.OuterDiameterMessage) {
                    displayOuterMeasurement((GaugeIMessage.OuterDiameterMessage) message);
                } else if (message instanceof GaugeIMessage.OuterWeightMessage) {
                    displayOuterWeight((GaugeIMessage.OuterWeightMessage) message);
                } else if (message instanceof GaugeIMessage.OuterDetailsMessage) {
                    displayOuterDetail((GaugeIMessage.OuterDetailsMessage) message);
                } else if (message instanceof GaugeIMessage.BatteryMessage) {
                    displayBattery((GaugeIMessage.BatteryMessage) message);
                } else if (message instanceof GaugeIMessage.ModeMessage) {
                    displayMode(((GaugeIMessage.ModeMessage) message).value);
                } else if (message instanceof GaugeIMessage.MaterialMessage) {
                    displayMaterialType(((GaugeIMessage.MaterialMessage) message).value);
                } else if (message instanceof GaugeIMessage.CutStyleMessage) {
                    displayCutStyle(((GaugeIMessage.CutStyleMessage) message).value);
                } else if (message instanceof GaugeIMessage.SpecificGravityMessage) {
                    displaySpecificGravity(((GaugeIMessage.SpecificGravityMessage) message).value);
                } else if (message instanceof GaugeIMessage.HoldMessage) {
                    displayHoldResult(true);
                } else if (message instanceof GaugeIMessage.UnholdMessage) {
                    displayHoldResult(false);
                }

                isIgnoreBTMessage = false;
            }
        }
    }

    public void displayBattery(GaugeIMessage.BatteryMessage message) {
        ResizableImageView batteryIcon = (ResizableImageView) rootView.findViewById(R.id.gauge_battery_icon);
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

    public void displayHoldResult(boolean isHold) {
        isHoldMessage = isHold;
        if (isHold) {
            isIgnoreBTMessage = true;
        }
        setHoldResult(isHold);
        SpeakHoldResult(isHold);
    }

    public void SpeakHoldResult(boolean isHold) {
        if (!isHold) {
            activity.speak("Unhold");
            return;
        }

        //Weight,   speak ct
        //Inner,   speak mm
        //Outer,   speak ct, mm
        TextView txtWeightValue = (TextView) rootView.findViewById(R.id.gauge_weight_value);
        Float weightValue = Float.parseFloat(txtWeightValue.getText().toString());
        TextView txtValue = (TextView) rootView.findViewById(R.id.gauge_value);
        Float value = Float.parseFloat(txtValue.getText().toString());
        TextView txtUnit = (TextView) rootView.findViewById(R.id.gauge_unit);
        String unit = txtUnit.getText().toString();
        if (this.viewMode == GaugeMode.Weight) {
            if (weightValue <= 0 ) {
                activity.speak("Hold");
                return;
            } else {
                activity.speak("Hold " + weightValue + " carat");
            }
        } else if (this.viewMode == GaugeMode.Inner) {
            if (value <= 0) {
                activity.speak("Hold");
                return;
            } else {
                if (unit.equals("mm")) {
                    activity.speak("Hold " + value + " millimetre");
                } else if (unit.equals(INCH_UNIT)) {
                    activity.speak("Hold " + value + " inch");
                }
            }
        } else if (this.viewMode == GaugeMode.Outer) {
            if (value <= 0) {
                activity.speak("Hold");
                return;
            } else {
                if (unit.equals("mm")) {
                    activity.speak("Hold " + value + " millimetre weight " + weightValue + " carat");
                } else if (unit.equals(INCH_UNIT)) {
                    activity.speak("Hold " + value + " inch weight "  + weightValue + " carat");
                }
            }
        }
    }

    public void displayMode(String mode) {
        TextView txtView = (TextView) rootView.findViewById(R.id.gauge_mode);
        LinearLayout panel = (LinearLayout) rootView.findViewById(R.id.gauge_result_panel);
        if (mode.equals("W")) {
            this.viewMode = GaugeMode.Weight;
            txtView.setText(activity.getString(R.string.gauge_weight_mode));
            setPanelAnimationToReady(GaugeMode.Weight);
            panel.setVisibility(View.VISIBLE);
        } else if (mode.equals("I")) {
            this.viewMode = GaugeMode.Inner;
            txtView.setText(activity.getString(R.string.gauge_inner_mode));
            setPanelAnimationToReady(GaugeMode.Inner);
            panel.setVisibility(View.GONE);
        } else if (mode.equals("O")) {
            this.viewMode = GaugeMode.Outer;
            txtView.setText(activity.getString(R.string.gauge_outer_mode));
            setPanelAnimationToReady(GaugeMode.Outer);
            panel.setVisibility(View.GONE);
        } else {
            this.viewMode = GaugeMode.Unspecific;
            txtView.setText(activity.getString(R.string.gauge_mode_blank));
            panel.setVisibility(View.GONE);
        }
        resetScreen();
    }

    public void displayMaterialType(String shortCode) {
        material = shortCode;
        setMaterialType(helperMessage.GetMaterialTypeDisplayedText(shortCode));
        setMeasurementDigital(0.00f,"mm");
        setWeightDigital(0.000f, "ct");
        setCutStyle("-");
        setBlankDiameterResult();
        setSpecificGravity("-");
        setAddCertEnable(false);
    }

    public void displaySpecificGravity(String text) {
        gravity = text;
        setSpecificGravity(text);
        setAddCertEnable(false);
    }

    public void displayCutStyle(String shortCode) {
        shape = shortCode;
        setCutStyle(helperMessage.GetCutStyleDisplayedText(shortCode));
        setMeasurementDigital(0.00f,"mm");
        setWeightDigital(0.000f, "ct");
        setBlankDiameterResult();
        setSpecificGravity("-");
        setAddCertEnable(false);
    }

    public void displayLength(GaugeIMessage.LengthMessage message) {
        //setMeasurementDigital(message.value, message.unit);
        TextView txtView = (TextView) rootView.findViewById(R.id.gauge_result_length);
        txtView.setText(String.format(activity.gaugeLengthResult, message.value, message.unit));
        lengthValue = message.value;
        lengthUnit = message.unit;
        setWeightDigital(0.000f,"ct");
        setAddCertEnable(false);
    }

    public void displayWidth(GaugeIMessage.WidthMessage message) {
        //setMeasurementDigital(message.value, message.unit);
        TextView txtView = (TextView) rootView.findViewById(R.id.gauge_result_width);
        txtView.setText(String.format(activity.gaugeWidthResult, message.value, message.unit));
        widthValue = message.value;
        widthUnit = message.unit;
        setWeightDigital(0.000f,"ct");
        setAddCertEnable(false);
    }

    public void displayDepth(GaugeIMessage.DepthMessage message) {
        //setMeasurementDigital(message.value, message.unit);
        TextView txtView = (TextView) rootView.findViewById(R.id.gauge_result_depth);
        txtView.setText(String.format(activity.gaugeDepthResult, message.value, message.unit));
        depthValue = message.value;
        depthUnit = message.unit;
        setWeightDigital(0.000f,"ct");
        setAddCertEnable(false);
    }

    public void displayWeight(GaugeIMessage.WeightMessage message) {
        setWeightDigital(message.value, message.unit);
        weightValue = message.value;
        weightUnit = message.unit;

        if (this.viewMode == GaugeMode.Weight &&
                (material.equals("47") || material.equals("93")) &&
                message.value > 0) {
            setAddCertEnable(true);
        } else {
            setAddCertEnable(false);
        }

        activity.speak("Weight " + message.value + " carat");
    }

    public void displayWeightDetails(GaugeIMessage.WeightDetailsMessage message) {
        //setMeasurementDigital(message.depthValue, message.depthUnit);
        displayMaterialType(message.gemstoneType);
        displayCutStyle(message.shape);
        displaySpecificGravity(message.specificGravity);
        setWeightDigital(message.weightValue, message.weightUnit);
        weightValue = message.weightValue;
        weightUnit = message.weightUnit;

        ((TextView) rootView.findViewById(R.id.gauge_result_length)).setText(String.format(activity.gaugeLengthResult,
                message.lengthValue, message.lengthUnit));
        ((TextView) rootView.findViewById(R.id.gauge_result_width)).setText(String.format(activity.gaugeWidthResult,
                message.widthValue, message.widthUnit));
        ((TextView) rootView.findViewById(R.id.gauge_result_depth)).setText(String.format(activity.gaugeDepthResult,
                message.depthValue, message.depthUnit));
        if (this.viewMode == GaugeMode.Weight &&
                (message.gemstoneType.equals("47") || message.gemstoneType.equals("93")) &&
                message.weightValue > 0) {
            setAddCertEnable(true);
            return;
        }
        setAddCertEnable(false);
    }

    public void setAddCertEnable(Boolean isEnable) {
        Button btnAddCert = (Button) rootView.findViewById(R.id.btn_gauge_ok);
        if (isEnable) {
            btnAddCert.setBackgroundResource(R.drawable.button_selector_pressable);
        } else {
            btnAddCert.setBackgroundResource(R.drawable.button_selector_disable);
        }
    }

    public void displayInnerMeasurement(GaugeIMessage.InnerDetailsMessage message) {
        setMeasurementDigital(message.value, message.unit);
        setWeightDigital(0.000f,"ct");
    }

    public void displayOuterMeasurement(GaugeIMessage.OuterDiameterMessage message) {
        setMeasurementDigital(message.value, message.unit);
    }

    public void displayOuterWeight(GaugeIMessage.OuterWeightMessage message) {
        setWeightDigital(message.value, message.unit);
    }

    public void displayOuterDetail(GaugeIMessage.OuterDetailsMessage message) {
        setMeasurementDigital(message.diameterValue, message.diameterUnit);
        setWeightDigital(message.weightValue, message.weightUnit);
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

    private void setHoldResult(boolean isHoldResult) {
        if (isHoldResult) {
            TextView text = (TextView) rootView.findViewById(R.id.gauge_hold);
            text.setBackgroundResource(R.drawable.textview_rounded_corner_hold_in);
        } else {
            TextView text = (TextView) rootView.findViewById(R.id.gauge_hold);
            text.setBackgroundResource(R.drawable.textview_rounded_corner_hold_out);
        }
    }

    private float getMMValue(float inchValue) {
        return inchValue * 25.4f;   // Convert from Inch to mm.
    }

    /*
    private void setGaugeResultText(String dimension, float value, String unit, boolean isHoldResult) {
        if (!isHoldResult) {
            return;
        }

        String textToSpeak = "";
        if (dimension.equals("Width")) {
            widthValue = value;
            widthUnit = unit;
            TextView txtView = (TextView) rootView.findViewById(R.id.gauge_result_width);
            txtView.setText(String.format(activity.gaugeWidthResult,value, unit));
            textToSpeak = "Width " + value;
        } else if (dimension.equals("Length")) {
            lengthValue = value;
            lengthUnit = unit;
            TextView txtView = (TextView) rootView.findViewById(R.id.gauge_result_length);
            txtView.setText(String.format(activity.gaugeLengthResult,value, unit));
            textToSpeak = "Length " + value;
        } else if (dimension.equals("Height")) {
            heightValue = value;
            heightUnit = unit;
            TextView txtView = (TextView) rootView.findViewById(R.id.gauge_result_depth);
            txtView.setText(String.format(activity.gaugeDepthResult,value, unit));
            textToSpeak = "Height " + value;
        }

        if (unit.equals("MM.")) {
            textToSpeak = textToSpeak + " millimeter";
        } else if (unit.equals("IN.")) {
            textToSpeak = textToSpeak + " Inch";
        }
        activity.speak(textToSpeak);
    }
    */

    private void setMeasurementDigital(float value, String unit) {
        RiseNumberTextView measurementValue = (RiseNumberTextView) rootView.findViewById(R.id.gauge_value);
        measurementValue.setFloat(value, "##0.00");
        TextView measurementUnit = (TextView) rootView.findViewById(R.id.gauge_unit);
        measurementUnit.setText(unit);
    }

    private void setWeightDigital(float value, String unit) {
        RiseNumberTextView weightValue = (RiseNumberTextView) rootView.findViewById(R.id.gauge_weight_value);
        weightValue.setFloat(value);
        TextView weightUnit = (TextView) rootView.findViewById(R.id.gauge_weight_unit);
        weightUnit.setText(unit);
    }

    private void setSpecificGravity(String gravity) {
        TextView txtView = (TextView) rootView.findViewById(R.id.gauge_result_gravity);
        txtView.setText(String.format(activity.getString(R.string.gravity_result_text), gravity));
    }

    private void setMaterialType(String type) {
        TextView txtView = (TextView) rootView.findViewById(R.id.gauge_material);
        txtView.setText(String.format(activity.getString(R.string.material_result_text), type));
    }

    private void setCutStyle(String shape) {
        TextView txtView = (TextView) rootView.findViewById(R.id.gauge_result_shape);
        txtView.setText(String.format(activity.getString(R.string.cutting_result_text), shape));
    }

    private void setBlankDiameterResult() {
        TextView txtWidth = (TextView) rootView.findViewById(R.id.gauge_result_width);
        txtWidth.setText(String.format(activity.gaugeWidthResult,"-", ""));
        TextView txtLength = (TextView) rootView.findViewById(R.id.gauge_result_length);
        txtLength.setText(String.format(activity.gaugeLengthResult,"-", ""));
        TextView txtHeight = (TextView) rootView.findViewById(R.id.gauge_result_depth);
        txtHeight.setText(String.format(activity.gaugeDepthResult,"-", ""));
    }

    private void setPanelAnimationToReady(GaugeMode mode) {
        TextView gaugeValue = (TextView) rootView.findViewById(R.id.gauge_value);
        TextView gaugeUnit = (TextView) rootView.findViewById(R.id.gauge_unit);
        setMeasurementDigital(0.00f, "mm");
        if (mode == GaugeMode.Weight) {
            gaugeValue.setTextColor(activity.getResources().getColor(R.color.colorGrayColor));
            gaugeUnit.setTextColor(activity.getResources().getColor(R.color.colorGrayColor));
        } else {
            gaugeValue.setTextColor(activity.getResources().getColor(R.color.colorTextColor));
            gaugeUnit.setTextColor(activity.getResources().getColor(R.color.colorTextColor));
        }

        TextView weightValue = (TextView) rootView.findViewById(R.id.gauge_weight_value);
        TextView weightUnit = (TextView) rootView.findViewById(R.id.gauge_weight_unit);
        setWeightDigital(0.000f, "ct");
        if (mode == GaugeMode.Inner) {
            weightValue.setTextColor(activity.getResources().getColor(R.color.colorGrayColor));
            weightUnit.setTextColor(activity.getResources().getColor(R.color.colorGrayColor));
        } else {
            weightValue.setTextColor(activity.getResources().getColor(R.color.colorYellowColor));
            weightUnit.setTextColor(activity.getResources().getColor(R.color.colorYellowColor));
        }
    }

    private void setPanelAnimationToNone() {
        TextView gaugeValue = (TextView) rootView.findViewById(R.id.gauge_value);
        TextView gaugeUnit = (TextView) rootView.findViewById(R.id.gauge_unit);
        TextView weightValue = (TextView) rootView.findViewById(R.id.gauge_weight_value);
        TextView weightUnit = (TextView) rootView.findViewById(R.id.gauge_weight_unit);
        gaugeValue.setTextColor(activity.getResources().getColor(R.color.colorGrayColor));
        gaugeUnit.setTextColor(activity.getResources().getColor(R.color.colorGrayColor));
        weightValue.setTextColor(activity.getResources().getColor(R.color.colorGrayColor));
        weightUnit.setTextColor(activity.getResources().getColor(R.color.colorGrayColor));
    }

    @Override
    public void onAttach(final Activity activity) {
        super.onAttach(activity);

        setHasOptionsMenu(true);
    }

    /*
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        //GaugeTestsModel gaugeTestsModel = new GaugeTestsModel();
        //gaugeTestsModel.value = widthValue;
        //gaugeTestsModel.unit = widthUnit;
        //activity.diamondCerModel.gaugeTestsModel = gaugeTestsModel;
    }*/

    /*
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        DiamondCerModel diamondCerModel = activity.diamondCerModel;
        if (diamondCerModel != null && diamondCerModel.gaugeTestsModel != null) {
            widthValue = diamondCerModel.gaugeTestsModel.width;
            widthUnit = diamondCerModel.gaugeTestsModel.unit;

            setDigital(widthValue,false);
            setDigitalUnit(widthUnit);
        }
    }*/

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

    private void setNewTest() {
        Button btnNewTest = (Button) rootView.findViewById(R.id.btn_gauge_new_tests);
        btnNewTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                AlertDialog.Builder alert = new AlertDialog.Builder(activity);
                alert.setTitle("Primo-I Tests");
                alert.setMessage("Do you want to start new tests?");
                alert.setNegativeButton("Cancel", null);
                alert.setPositiveButton("Ok", new AlertDialog.OnClickListener() {
                    public void onClick(DialogInterface dialog, int arg1) {
                        resetScreen();
                        isIgnoreBTMessage = false;
                    }
                });
                alert.show();
            }
        });
    }

    private void setOK() {
        Button btnOK = (Button) rootView.findViewById(R.id.btn_gauge_ok);
        btnOK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (viewMode != GaugeMode.Weight || (!(material.equals("93") || material.equals("47")))) {
                    AlertDialog.Builder alert = new AlertDialog.Builder(activity);
                    alert.setTitle("Primo-I Tests");
                    alert.setMessage("Only Diamond and Moissanite’s weight estimation may add in certificate");
                    alert.setPositiveButton("OK", new AlertDialog.OnClickListener() {
                        public void onClick(DialogInterface dialog, int arg1) {
                            isIgnoreBTMessage = false;
                        }
                    });
                    alert.show();
                    return;
                }

                //if (isHoldMessage) {
                    if (weightValue == 0.0f) {
                        AlertDialog.Builder alert = new AlertDialog.Builder(activity);
                        alert.setTitle("Primo-I Tests");
                        alert.setMessage("Weight estimation result is not completed yet\nPlease continue.");
                        alert.setPositiveButton("OK", new AlertDialog.OnClickListener() {
                            public void onClick(DialogInterface dialog, int arg1) {
                                isIgnoreBTMessage = false;
                            }
                        });
                        alert.show();
                    } else {
                        if ((!widthUnit.isEmpty() && !lengthUnit.isEmpty() && !widthUnit.equals(lengthUnit)) ||
                                (!widthUnit.isEmpty() && !depthUnit.isEmpty() && !widthUnit.equals(depthUnit)) ||
                                (!lengthUnit.isEmpty() && !depthUnit.isEmpty() && !lengthUnit.equals(depthUnit))) {
                            AlertDialog.Builder alert = new AlertDialog.Builder(activity);
                            alert.setTitle("Primo-I Tests");
                            alert.setMessage("Unit are not the same.\nDo you want to convert to mm.?");
                            alert.setNegativeButton("Cancel", new AlertDialog.OnClickListener() {
                                public void onClick(DialogInterface dialog, int arg1) {
                                    isIgnoreBTMessage = false;
                                }
                            });
                            alert.setPositiveButton("Ok", new AlertDialog.OnClickListener() {
                                public void onClick(DialogInterface dialog, int arg1) {
                                    if (!widthUnit.equals(MM_UNIT)) {
                                        widthUnit = MM_UNIT;
                                        widthValue = getMMValue(widthValue);
                                        TextView txtWidth = (TextView) rootView.findViewById(R.id.gauge_result_width);
                                        txtWidth.setText(String.format(activity.gaugeWidthResult, widthValue, widthUnit));
                                    }
                                    if (!lengthUnit.equals(MM_UNIT)) {
                                        lengthUnit = MM_UNIT;
                                        lengthValue = getMMValue(lengthValue);
                                        TextView txtLength = (TextView) rootView.findViewById(R.id.gauge_result_length);
                                        txtLength.setText(String.format(activity.gaugeLengthResult, lengthValue, lengthUnit));
                                    }
                                    if (!depthUnit.equals(MM_UNIT)) {
                                        depthUnit = MM_UNIT;
                                        depthValue = getMMValue(depthValue);
                                        TextView txtHeight = (TextView) rootView.findViewById(R.id.gauge_result_depth);
                                        txtHeight.setText(String.format(activity.gaugeDepthResult, depthValue, depthUnit));
                                    }
                                    addToCertification();
                                }
                            });
                            alert.show();
                        } else {
                            addToCertification();
                        }
                    }
                /*} else {
                    isIgnoreBTMessage = true;
                    AlertDialog.Builder alert = new AlertDialog.Builder(activity);
                    alert.setTitle("Primo-I Tests");
                    alert.setMessage("The result movement is very sensitive. Please hold result first, then add cert.");
                    alert.setPositiveButton("Ok", new AlertDialog.OnClickListener() {
                        public void onClick(DialogInterface dialog, int arg1) {
                            isIgnoreBTMessage = false;
                        }
                    });
                    alert.show();
                }*/
            }
        });
    }

    public void addToCertification() {
        /*if (widthValue == 0.0f || lengthValue == 0.0f || depthValue == 0.0f) {
            AlertDialog.Builder alert = new AlertDialog.Builder(activity);
            alert.setTitle("Primo-I Tests");
            alert.setMessage("Tests Dimensions (Width * Length * Depth) are " + widthValue + " x " + lengthValue + " x " + depthValue +".\nDo you want to add to certification?");
            alert.setNegativeButton("Cancel", null);
            alert.setPositiveButton("Ok", new AlertDialog.OnClickListener() {
                public void onClick(DialogInterface dialog, int arg1) {
                    //Add cert
                    GaugeTestsModel model = new GaugeTestsModel(true, GaugeIBluetoothLeService.GAGUE_I_DEVICE_NAME, widthValue, lengthValue, depthValue, widthUnit);
                    activity.setGaugeTestsModel(model);

                    Toast.makeText(activity, "Added to Certificate", Toast.LENGTH_SHORT).show();
                    activity.log("OK", "Add to certificate", false);
                }
            });
            alert.show();
        } else {*/
            GaugeTestsModel model = new GaugeTestsModel(true, GaugeIBluetoothLeService.GAGUE_I_DEVICE_NAME,
                    helperMessage.GetMaterialTypeDisplayedText(material), helperMessage.GetCutStyleDisplayedText(shape),gravity,
                    widthValue, lengthValue, depthValue, widthUnit,
                    weightValue, weightUnit);
            activity.setGaugeTestsModel(model);

            ScaleTestsModel scaleModel = new ScaleTestsModel(true, GaugeIBluetoothLeService.GAGUE_I_DEVICE_NAME,
                weightValue, weightUnit);
            activity.setScaleTestsModel(scaleModel);


            Toast.makeText(activity, "Added to Certificate", Toast.LENGTH_SHORT).show();
            viewPager.goToCertificationView();
        //}
    }

    public void resetScreen() {
        setMeasurementDigital(0.00f,"mm");
        setWeightDigital(0.000f, "ct");
        setMaterialType("-");
        setCutStyle("-");
        setBlankDiameterResult();
        setSpecificGravity("-");

        material = "";
        shape = "";
        gravity = "";
        widthValue = 0.0f;
        widthUnit = "";
        lengthValue = 0.0f;
        lengthUnit = "";
        depthValue = 0.0f;
        depthUnit = "";
        weightValue = 0.0f;
        weightUnit = "";

        setAddCertEnable(false);
    }

    @Override
    public void fragmentBecameVisible() {
        activity.log("fragmentBecameVisible","back to gauge", false);
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

                    if (device.getName() != null && device.getName().equals(GaugeIBluetoothLeService.GAGUE_I_DEVICE_NAME)) {
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
            if (device.getName() != null && device.getName().equals(GaugeIBluetoothLeService.GAGUE_I_DEVICE_NAME)) {
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
        Intent gattServiceIntent = new Intent(activity, GaugeIBluetoothLeService.class);
        activity.bindService(gattServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
        activity.registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
    }


    @Override
    public void onResume() {
        super.onResume();
        activity.log("onResume","gauge", false);
    }

    @Override
    public void onPause() {
        super.onPause();
        scanLeDevice(false);

        activity.log("onPause","gauge", false);
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
            mBluetoothLeService = ((GaugeIBluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Toast.makeText(activity, String.format(activity.bluetoothMightBeClosed, GaugeIBluetoothLeService.GAGUE_I_DEVICE_NAME ), Toast.LENGTH_SHORT).show();
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
            if (GaugeIBluetoothLeService.ACTION_GAUGE_I_GATT_CONNECTED.equals(action)) {
                setConnectedDevice();
                setPanelAnimationToReady(GaugeMode.Weight);
            } else if (GaugeIBluetoothLeService.ACTION_GAUGE_I_GATT_DISCONNECTED.equals(action)) {
                setNotConnectedDevice();
                setPanelAnimationToNone();
                activity.unbindService(mServiceConnection);
                activity.unregisterReceiver(mGattUpdateReceiver);
            } else if (GaugeIBluetoothLeService.ACTION_GAUGE_I_GATT_SERVICES_DISCOVERED.equals(action)) {
                mBluetoothLeService.displayGattServices();
            } else if (GaugeIBluetoothLeService.ACTION_GAUGE_I_DATA_AVAILABLE.equals(action)) {
                displayBluetoothMessage(intent.getStringExtra(GaugeIBluetoothLeService.EXTRA_DATA_GAUGE_I));
            }
        }
    };

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(GaugeIBluetoothLeService.ACTION_GAUGE_I_GATT_CONNECTED);
        intentFilter.addAction(GaugeIBluetoothLeService.ACTION_GAUGE_I_GATT_DISCONNECTED);
        intentFilter.addAction(GaugeIBluetoothLeService.ACTION_GAUGE_I_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(GaugeIBluetoothLeService.ACTION_GAUGE_I_DATA_AVAILABLE);
        return intentFilter;
    }
}
