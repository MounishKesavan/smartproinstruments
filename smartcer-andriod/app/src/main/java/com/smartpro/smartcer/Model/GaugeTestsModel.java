package com.smartpro.smartcer.Model;

/**
 * Created by developer@gmail.com on 1/7/16 AD.
 */
public class GaugeTestsModel {
    public Boolean isTestedBySmartProDevice;
    public String smartProDeviceName;

    public String gemStoneType;
    public String cutStyle;
    public String specificGravity;
    public float width;
    public float length;
    public float depth;
    public String diameterUnit;
    public float weight;
    public String weightUnit;

    public GaugeTestsModel(Boolean isTestedBySmartProDevice,String smartProDeviceName,
                           String gemStoneType, String cutStyle, String specificGravity,
                           float width, float length, float depth, String diameterUnit,
                           float weight, String weightUnit) {
        this.isTestedBySmartProDevice = isTestedBySmartProDevice;
        this.smartProDeviceName = smartProDeviceName;

        this.gemStoneType = gemStoneType;
        this.cutStyle = cutStyle;
        this.specificGravity = specificGravity;
        this.width = width;
        this.length = length;
        this.depth = depth;
        this.diameterUnit = diameterUnit;
        this.weight = weight;
        this.weightUnit = weightUnit;
    }

    public GaugeTestsModel(Boolean isTestedBySmartProDevice,String smartProDeviceName,
                           float width, float length, float depth, String diameterUnit) {
        this.isTestedBySmartProDevice = isTestedBySmartProDevice;
        this.smartProDeviceName = smartProDeviceName;

        this.gemStoneType = "";
        this.cutStyle = "";
        this.specificGravity = "";
        this.width = width;
        this.length = length;
        this.depth = depth;
        this.diameterUnit = diameterUnit;
        this.weight = 0;
        this.weightUnit = "";
    }

    public GaugeTestsModel() {
        this.isTestedBySmartProDevice = false;
        this.smartProDeviceName = "";

        this.gemStoneType = "";
        this.cutStyle = "";
        this.specificGravity = "";
        this.width = 0;
        this.length = 0;
        this.depth = 0;
        this.diameterUnit = "";
        this.weight = 0;
        this.weightUnit = "";
    }
}
