package com.smartpro.smartcer.BluetoothMessage;

import android.util.Log;

/**
 * Created by developer@gmail.com on 1/18/16 AD.
 */
public class ScaleIMessage extends SmartProMessage {
    public static final int VALUE_MESSAGE_INDEX = 1;
    public static final int UNIT_MESSAGE_INDEX = 2;
    public static final int STABLE_MESSAGE_INDEX = 3;
    public static final String STABLE_CODE = "S";


    public ScaleIMessage() {
    }

    @Override
    public String getTesterCategory() {
        return SCALE_I_TESTER_CATEGORY;
    }

    public Message getMessage(String btMessage) {
        //if (isValidMessage(btMessage)) {

        try {
            String[] strs = getStringsMessage(btMessage);

            if (strs[CATETORY_MESSAGE_INDEX].equals(SCALE_I_TESTER_CATEGORY)) {
                if (strs[VALUE_MESSAGE_INDEX].equals(LOW_BATTERRY_MESSAGE)) {
                    return new BatteryMessage(strs[2]);
                } else {
                    Float value = Float.parseFloat(strs[VALUE_MESSAGE_INDEX]);
                    String unit = strs[UNIT_MESSAGE_INDEX];

                    if (strs.length == 3) {
                        return new TestResultsMessage(value, unit, false);
                    } else {
                        return new TestResultsMessage(value, unit, strs[STABLE_MESSAGE_INDEX].equals(STABLE_CODE));
                    }
                }
            }
        } catch (Exception ex) {
            Log.i("getMessage Err: ", btMessage);
        }

        return null;
        //}
    }

    public class Message {
    }

    public class BatteryMessage extends Message {
        public String value;

        public BatteryMessage (String value) {
            this.value = value;
        }
    }

    public class TestResultsMessage extends Message {
        public float value;
        public String unit;
        public Boolean isStable;

        public TestResultsMessage (float value, String unit, boolean isStable) {
            this.value = value;
            this.unit = unit;
            this.isStable = isStable;
        }
    }
}
