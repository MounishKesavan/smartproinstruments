package com.smartpro.smartcer.Model;

import android.content.Context;
import android.provider.Settings.Secure;

import com.smartpro.smartcer.Bluetooth.GaugeIBluetoothLeService;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by developer@gmail.com on 1/7/16 AD.
 */
public class DiamondCerModel {
    public String customerId = "";
    public String runningNo = "";
    public String sellerName = "";
    public DiamondTestsModel diamondTestsModel;
    public GaugeTestsModel gaugeTestsModel;
    public ScaleTestsModel scaleTestsModel;
    public String selectedPhotoPath;
    public DiamondPropertiesModel diamondPropertiesModel;

    public DiamondCerModel(Context context) {
        String deviceId = Secure.getString(context.getContentResolver(),Secure.ANDROID_ID);
        this.customerId = deviceId.substring(4).toUpperCase();
        this.diamondTestsModel = new DiamondTestsModel();
        this.gaugeTestsModel = new GaugeTestsModel();
        this.scaleTestsModel = new ScaleTestsModel();
        this.diamondPropertiesModel = new DiamondPropertiesModel();
    }

    public String getIssueDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy");
        return sdf.format(new Date());
    }

    public String getSPINo() {
        return this.customerId + "-" + this.runningNo;
    }

    public DiamondPropertiesModel getPropertiesModel() {
        if (this.diamondPropertiesModel == null) {
            this.diamondPropertiesModel = new DiamondPropertiesModel();
        }
        return this.diamondPropertiesModel;
    }

    public boolean IsCvdHphtGradingReport() {
        return (this.diamondTestsModel != null && this.diamondTestsModel.testResult.equals("CVD/HPHT"));
    }

    public boolean IsPrimoTests() {
        return (this.gaugeTestsModel != null && this.gaugeTestsModel.smartProDeviceName.equals(GaugeIBluetoothLeService.GAGUE_I_DEVICE_NAME));
    }
}







