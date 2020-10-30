package com.smartpro.smartcer.BluetoothMessage;

import android.util.Log;

/**
 * Created by developer@gmail.com on 1/18/16 AD.
 */
public class GaugeIMessage extends SmartProMessage {
    public static final int VALUE_MESSAGE_INDEX = 1;
    public static final int UNIT_MESSAGE_INDEX = 2;

    public GaugeIMessage() {
    }

    @Override
    public String getTesterCategory() {
        return GAUGE_I_TESTER_CATEGORY;
    }

    public Message getMessage(String btMessage) {
        //if (isValidMessage(btMessage)) {

        try {
            String[] strs = getStringsMessage(btMessage);

            if (strs[CATETORY_MESSAGE_INDEX].equals(GAUGE_I_TESTER_CATEGORY)) {
                switch (strs[VALUE_MESSAGE_INDEX])
                {
                    case "LN":
                        return new LengthMessage(Float.parseFloat(strs[2]),
                                strs[3]);
                    case "WD":
                        return new WidthMessage(Float.parseFloat(strs[2]),
                                strs[3]);
                    case "DP":
                        return new DepthMessage(Float.parseFloat(strs[2]),
                                strs[3]);
                    case "WG":
                        return new WeightMessage(Float.parseFloat(strs[2]),
                                strs[3]);
                    case "WC":
                        return new WeightDetailsMessage(
                                strs[2],
                                strs[3],
                                Float.parseFloat(strs[4]),
                                strs[5],
                                Float.parseFloat(strs[6]),
                                strs[7],
                                Float.parseFloat(strs[8]),
                                strs[9],
                                Float.parseFloat(strs[10]),
                                strs[11],
                                strs[12]);
                    case "ID":
                        return new InnerDetailsMessage(
                                Float.parseFloat(strs[2]),
                                strs[3]);
                    case "OD":
                        return new OuterDiameterMessage(
                                Float.parseFloat(strs[2]),
                                strs[3]);
                    case "OC":
                        return new OuterWeightMessage(
                                Float.parseFloat(strs[2]),
                                strs[3]);
                    case "SG":
                        return new SpecificGravityMessage(
                                strs[2]);
                    case "OT":
                        return new OuterDetailsMessage(
                                Float.parseFloat(strs[2]),
                                strs[3],
                                Float.parseFloat(strs[4]),
                                strs[5]);
                    case LOW_BATTERRY_MESSAGE:
                        Log.i("getMessage LB: ", strs[2]);
                        return new BatteryMessage(strs[2]);
                    case "MO":
                        Log.i("getMessage Mode: ", strs[2]);
                        return new ModeMessage(strs[2]);
                    case "TY":
                        Log.i("getMessage Gemtype: ", strs[2]);
                        return new MaterialMessage(strs[2]);
                    case "SH":
                        Log.i("getMessage Cut style: ", strs[2]);
                        return new CutStyleMessage(strs[2]);
                    case "H":
                        return new HoldMessage();
                    case "U":
                        return new UnholdMessage();
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

    public class HoldMessage extends Message {
        public HoldMessage () {
        }
    }

    public class UnholdMessage extends Message {
        public UnholdMessage () {
        }
    }

    public class ModeMessage extends Message {
        public String value;

        public ModeMessage (String value) {
            this.value = value;
        }
    }

    public class MaterialMessage extends Message {
        public String value;

        public MaterialMessage(String value) {
            this.value = value;
        }
    }

    public class CutStyleMessage extends Message {
        public String value;

        public CutStyleMessage (String value) {
            this.value = value;
        }
    }

    public class SpecificGravityMessage extends Message {
        public String value;

        public SpecificGravityMessage (String value) {
            this.value = value;
        }
    }

    public class LengthMessage extends Message {
        public float value;
        public String unit;

        public LengthMessage (float value, String unit) {
            this.value = value;
            this.unit = unit;
        }
    }

    public class WidthMessage extends Message {
        public float value;
        public String unit;

        public WidthMessage (float value, String unit) {
            this.value = value;
            this.unit = unit;
        }
    }

    public class DepthMessage extends Message {
        public float value;
        public String unit;

        public DepthMessage (float value, String unit) {
            this.value = value;
            this.unit = unit;
        }
    }

    public class WeightMessage extends Message {
        public float value;
        public String unit;

        public WeightMessage (float value, String unit) {
            this.value = value;
            this.unit = unit;
        }
    }

    public class WeightDetailsMessage extends Message {
        public String gemstoneType;
        public String shape;

        public float lengthValue;
        public String lengthUnit;
        public float widthValue;
        public String widthUnit;
        public float depthValue;
        public String depthUnit;

        public float weightValue;
        public String weightUnit;
        public String specificGravity;

        public WeightDetailsMessage (String gemstoneType,
                String shape,
                float lengthValue,
                String lengthUnit,
                float widthValue,
                String widthUnit,
                float depthValue,
                String depthUnit,
                float weightValue,
                String weightUnit,
                String specificGravity) {
            this.gemstoneType = gemstoneType;
            this.shape = shape;
            this.lengthValue = lengthValue;
            this.lengthUnit = lengthUnit;
            this.widthValue = widthValue;
            this.widthUnit = widthUnit;
            this.depthValue = depthValue;
            this.depthUnit = depthUnit;
            this.weightValue = weightValue;
            this.weightUnit = weightUnit;
            this.specificGravity = specificGravity;
        }
    }

    public class InnerDetailsMessage extends Message {
        public float value;
        public String unit;

        public InnerDetailsMessage (float value, String unit) {
            this.value = value;
            this.unit = unit;
        }
    }

    public class OuterDiameterMessage extends Message {
        public float value;
        public String unit;

        public OuterDiameterMessage(float value, String unit) {
            this.value = value;
            this.unit = unit;
        }
    }

    public class OuterWeightMessage extends Message {
        public float value;
        public String unit;

        public OuterWeightMessage(float value, String unit) {
            this.value = value;
            this.unit = unit;
        }
    }

    public class OuterDetailsMessage extends Message {
        public float diameterValue;
        public String diameterUnit;
        public float weightValue;
        public String weightUnit;

        public OuterDetailsMessage(float diameterValue, String diameterUnit,
                                   float weightValue, String weightUnit) {
            this.diameterValue = diameterValue;
            this.diameterUnit = diameterUnit;
            this.weightValue = weightValue;
            this.weightUnit = weightUnit;
        }
    }
}
