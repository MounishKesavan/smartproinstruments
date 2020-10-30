package com.smartpro.smartcer.Model;

/**
 * Created by developer@gmail.com on 2/8/16 AD.
 */
public class DiamondTestsModel {
    public Boolean isTestedBySmartProDevice;
    public String testResult;
    public String smartProDeviceName;


    public DiamondTestsModel(Boolean isTestedBySmartProDevice, String smartProDeviceName, String testResult) {
        this.isTestedBySmartProDevice = isTestedBySmartProDevice;
        this.smartProDeviceName = smartProDeviceName;
        this.testResult = testResult;
    }

    public DiamondTestsModel() {
        this.isTestedBySmartProDevice = false;
        this.smartProDeviceName = "";
        this.testResult = "";
    }
}
