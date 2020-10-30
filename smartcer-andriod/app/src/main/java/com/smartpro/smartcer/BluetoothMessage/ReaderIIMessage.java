package com.smartpro.smartcer.BluetoothMessage;

import android.util.Log;

/**
 * Created by developer@gmail.com on 1/18/16 AD.
 */
public class ReaderIIMessage extends SmartProMessage {
    public static final int VALUE_MESSAGE_INDEX = 1;

    public ReaderIIMessage() {
    }

    @Override
    public String getTesterCategory() {
        return READER_II_TESTER_CATEGORY;
    }

    public Message getMessage(String btMessage) {
        //if (isValidMessage(btMessage)) {

        try {
            String[] strs = getStringsMessage(btMessage);
            Log.i("getMessage: ", strs[VALUE_MESSAGE_INDEX]);

            if (strs[CATETORY_MESSAGE_INDEX].equals(READER_II_TESTER_CATEGORY)) {
                String value = strs[VALUE_MESSAGE_INDEX];
                if (value.equals(LOW_BATTERRY_MESSAGE)) {
                    Log.i("getMessage LB: ", strs[2]);
                    return new BatteryMessage(strs[2]);
                } else if (value.equals("Met")) {
                    Log.i("getMessage Metal: ", strs[VALUE_MESSAGE_INDEX]);
                    return new MetalMessage();
                } else if (value.equals("Dia") ||
                        value.equals("Moi") ||
                        value.equals("Sim")) {
                    Log.i("getMessage Rst: ", strs[VALUE_MESSAGE_INDEX]);
                    return new TestResultsMessage(strs[VALUE_MESSAGE_INDEX]);
                } else if (value.equals(READY_MESSAGE)) {
                    Log.i("getMessage Ready: ", strs[VALUE_MESSAGE_INDEX]);
                    return new ReadyMessage();
                }
            }
        } catch (Exception ex) {
            Log.i("getMessage Err: ", btMessage);
        }

        return  null;
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

    public class ReadyMessage extends Message {
        public ReadyMessage () {
        }
    }

    public class MetalMessage extends Message {
        public MetalMessage () {
        }
    }

    public class TestResultsMessage extends Message {
        public String value;

        public TestResultsMessage (String value) {
            this.value = value;
        }
    }
}
