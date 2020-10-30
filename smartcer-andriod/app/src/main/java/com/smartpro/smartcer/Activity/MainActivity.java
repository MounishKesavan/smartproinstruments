package com.smartpro.smartcer.Activity;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import com.smartpro.smartcer.Control.LockableViewPager;
import com.smartpro.smartcer.Fragment.DiamondPropertiesFragment;
import com.smartpro.smartcer.Model.DiamondCerModel;
import com.smartpro.smartcer.Model.DiamondPropertiesModel;
import com.smartpro.smartcer.Model.DiamondTestsModel;
import com.smartpro.smartcer.Model.GaugeTestsModel;
import com.smartpro.smartcer.Model.ScaleTestsModel;
import com.smartpro.smartcer.Control.PagerAdapter;
import com.smartpro.smartcer.R;

import java.util.HashMap;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    public PagerAdapter pagerAdapter;
    public LockableViewPager pager;
    public DiamondPropertiesFragment diamondPropertiesFragment;

    protected DiamondCerModel diamondCerModel;
    public String bluetoothIsNotPaired;
    public String bluetoothPressToConnect;
    public String bluetoothDisConnectedDevice;
    public String bluetoothNotBle;
    public String bluetoothNotAvailable;
    public String bluetoothMightBeClosed;
    public String bluetoothNotConnected;
    public String bluetoothConnecting;
    public String bluetoothConnected;
    public String bluetoothConnectedDeviceName;
    public String btnConnect;
    public String btnDisconnect;
    public Typeface digitalFont;
    public int colorText;
    public int colorConnected;
    public int colorTextDigital;
    public int currentScreenSizeWidth;
    public float screenScale;

    public String diamondResult;
    public String gaugeLengthResult;
    public String gaugeWidthResult;
    public String gaugeDepthResult;

    public TextToSpeech TTS;

    public static final long SCAN_PERIOD = 10000;
    public static final int REQUEST_ENABLE_BT = 3;
    public static final int REQUEST_DISCOVERABLE_BT = 4;
    public static boolean IS_SHOW_BT_MSG = false;
    public static boolean IS_LE = true;
    public static float DEFAULT_SCREEN_SIZE_WIDTH = 1080;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String languageToLoad  = "en";
        Locale locale = new Locale(languageToLoad);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());


        setContentView(R.layout.activity_main);
        //digitalFont = Typeface.createFromAsset(this.getAssets(), "fonts/digital-7.ttf");
        digitalFont = Typeface.createFromAsset(this.getAssets(), "fonts/7segment.ttf");

        setViewPager();

        diamondCerModel = new DiamondCerModel(this);
        bluetoothIsNotPaired = this.getString(R.string.bt_is_not_paired);
        bluetoothPressToConnect = this.getString(R.string.bt_press_to_connect);
        bluetoothDisConnectedDevice = this.getString(R.string.disconnected_device_successfully);
        bluetoothNotBle = this.getString(R.string.bt_not_ble);
        bluetoothNotAvailable = this.getString(R.string.bt_not_work);
        bluetoothMightBeClosed = this.getString(R.string.bt_might_be_closed);
        bluetoothNotConnected = this.getString(R.string.not_connected_status);
        bluetoothConnecting = this.getString(R.string.connecting_status);
        bluetoothConnected = this.getString(R.string.connected_status);
        bluetoothConnectedDeviceName = this.getString(R.string.connected_device);
        btnConnect = this.getString(R.string.btn_connect);
        btnDisconnect = this.getString(R.string.btn_disconnect);

        diamondResult = this.getString(R.string.diamond_test_result_text);
        gaugeWidthResult = this.getString(R.string.width_result_text);
        gaugeLengthResult = this.getString(R.string.length_result_text);
        gaugeDepthResult = this.getString(R.string.depth_result_text);

        colorText = this.getResources().getColor(R.color.colorTextColor);
        colorConnected = this.getResources().getColor(R.color.colorConnectedColor);
        colorTextDigital = this.getResources().getColor(R.color.colorDigitalTextColor);

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        currentScreenSizeWidth = size.x;
        screenScale = ((float) currentScreenSizeWidth) / DEFAULT_SCREEN_SIZE_WIDTH;

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (TTS == null) {
            TTS = new TextToSpeech(MainActivity.this, new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int status) {
                    if (status != TextToSpeech.ERROR) {
                        TTS.setLanguage(Locale.UK);
                    }
                }
            });
        }
    }

    public boolean getIsShowBTMsg() {
        return IS_SHOW_BT_MSG;
    }

    private void setViewPager() {
        pagerAdapter = new PagerAdapter(getSupportFragmentManager(), this);
        pager = (LockableViewPager) findViewById(R.id.pager);
        pager.setAdapter(pagerAdapter);
        pager.setOffscreenPageLimit(PagerAdapter.PAGE_NUM-1);
    }

    public void setDiamondPropertiesFragment(DiamondPropertiesFragment f) {
        diamondPropertiesFragment = f;
    }

    public DiamondCerModel getDiamondCerModel() {
        if (this.diamondCerModel == null) {
            this.diamondCerModel = new DiamondCerModel(this);
        }
        return this.diamondCerModel;
    }

    public void setNewDiamondCerModel() {
        this.diamondCerModel = new DiamondCerModel(this);
    }

    public void setRunningNo(String runningNo) {
        this.getDiamondCerModel().runningNo = runningNo;
    }

    public void setSellerName(String name) {
        this.getDiamondCerModel().sellerName = name;
    }

    public void setDiamondTestResult(DiamondTestsModel model) {
        this.getDiamondCerModel().diamondTestsModel = model;
        diamondPropertiesFragment.setTestedBy(this.getDiamondCerModel().diamondTestsModel);
    }

    public void setGaugeTestsModel(GaugeTestsModel model) {
        this.getDiamondCerModel().gaugeTestsModel = model;
        diamondPropertiesFragment.setMeasurement(this.getDiamondCerModel().gaugeTestsModel);

        //Put Shape to Diamond property
        if (model.cutStyle != null && !model.cutStyle.equals("")) {
            this.getDiamondCerModel().getPropertiesModel().shape = model.cutStyle;
            diamondPropertiesFragment.setShape(model.cutStyle);
        }
    }

    public void setScaleTestsModel(ScaleTestsModel model) {
        this.getDiamondCerModel().scaleTestsModel = model;
        diamondPropertiesFragment.setWeight(this.getDiamondCerModel().scaleTestsModel);
    }

    public void setSelectedPhoto(String path) {
        this.getDiamondCerModel().selectedPhotoPath = path;
    }

    public void setDiamondPropertiesModel(DiamondPropertiesModel model) {
        this.getDiamondCerModel().diamondPropertiesModel = model;
    }

    public void speak(String text) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Bundle params = new Bundle();
            params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f);
            TTS.speak(text, TextToSpeech.QUEUE_FLUSH, params, "");
        } else {
            HashMap<String, String> params = new HashMap<String, String>();
            params.put(TextToSpeech.Engine.KEY_PARAM_VOLUME, "1.0");
            TTS.speak(text, TextToSpeech.QUEUE_FLUSH, params);
        }
    }

    public int getCurrentPageShowing() {
        return pager.getCurrentItem();
    }

    public boolean isInternetAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    public void log(String title, String desc, Boolean isSend) {
        Log.i(title, "" + desc);
    }
}
