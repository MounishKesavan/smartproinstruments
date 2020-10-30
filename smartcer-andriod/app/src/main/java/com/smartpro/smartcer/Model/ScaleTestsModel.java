package com.smartpro.smartcer.Model;

/**
 * Created by developer@gmail.com on 1/7/16 AD.
 */
public class ScaleTestsModel {
    public Boolean isTestedBySmartProDevice;
    public String smartProDeviceName;

    public float value;
    public String unit;

    public ScaleTestsModel(Boolean isTestedBySmartProDevice, String smartProDeviceName, float value, String unit) {
        this.isTestedBySmartProDevice = isTestedBySmartProDevice;
        this.smartProDeviceName = smartProDeviceName;
        this.value = value;
        this.unit = unit;
    }

    public ScaleTestsModel() {
        this.isTestedBySmartProDevice = false;
        this.smartProDeviceName = "";
        this.value = 0;
        this.unit = "";
    }

    public String getUnitText() {
        if (this.unit == "ct") {
            return "carat";
        } else {
            return this.unit;
        }
    }
}