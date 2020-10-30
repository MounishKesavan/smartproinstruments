package com.smartpro.smartcer.BluetoothMessage;

import android.util.Log;

/**
 * Created by developer@gmail.com on 1/18/16 AD.
 */
public class SmartProMessage {
    public static final String NOT_DEFINED_TESTER_CATEGORY = "0";
    public static final String READER_II_TESTER_CATEGORY = "1";   //1:Dia;, 1:Moi;, 1:Sim:1; , 1:Met,  1:Rea;
    public static final String SCALE_I_TESTER_CATEGORY = "2"; //ct, oz
    public static final String GAUGE_I_TESTER_CATEGORY = "3"; // mm, in
    public static final String GEMEYE_I_TESTER_CATEGORY = "4";  //-4 metal, 4:[value]
    public static final String SCREEN_I_TESTER_CATEGORY = "5";  // 5:Dia;, 5:CVD;, 5:Tra;, 5:Rea;

    public static final int CATETORY_MESSAGE_INDEX = 0;
    public static final String MESSAGE_SEPERATOR = ":";
    public static final String MESSAGE_FINISHER = ";\n\r";
    public static final String LOW_BATTERRY_MESSAGE = "LB";   // LB:1, LB:2, LB:3, LB:4, LB:5,    pooling for 1 minute
    public static final String READY_MESSAGE = "Rea";

    public SmartProMessage() {}

    public String getTesterCategory() {
        return NOT_DEFINED_TESTER_CATEGORY;
    }

    public Boolean isValidMessage(String btMessage) {
        if(btMessage != null && !btMessage.isEmpty()) {
            Log.i("isValidMessage msg ", btMessage );
            Log.i("isValidMessage Start ", Boolean.toString( btMessage.startsWith(getTesterCategory()+ MESSAGE_SEPERATOR)) );
            Log.i("isValidMessage End ", Boolean.toString( btMessage.endsWith(MESSAGE_FINISHER)) );
            return btMessage.startsWith(getTesterCategory()+ MESSAGE_SEPERATOR) && btMessage.endsWith(MESSAGE_FINISHER);
        }
        return false;
    }

    public String[] getStringsMessage(String btMessage) {
        String[] strs = btMessage.replace(MESSAGE_FINISHER,"").split(MESSAGE_SEPERATOR);
        return strs;
    }
}
