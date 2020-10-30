package com.smartpro.smartcer.Bluetooth;

import java.util.HashMap;

/**
 * Created by developer@gmail.com on 5/5/16 AD.
 */
public class BluetoothLeGattAttributes {
    private static HashMap<String, String> attributes = new HashMap();
    public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";
    public static String DEVICE_INFO_SERVICE = "0000180a-0000-1000-8000-00805f9b34fb";
    public static String GENERIC_ACCESS_SERVICE = "00001800-0000-1000-8000-00805f9b34fb";
    public static String GENERIC_ATTRIBUTE_SERVICE = "00001801-0000-1000-8000-00805f9b34fb";
    public static String SMARTPRO_SERVICE = "0000ffe0-0000-1000-8000-00805f9b34fb";
    public static String SMARTPRO_RX_TX_CHARACTERISTIC = "0000ffe1-0000-1000-8000-00805f9b34fb";

    static {
        // Services.
        attributes.put(DEVICE_INFO_SERVICE, "Device Information Service");
        attributes.put(GENERIC_ACCESS_SERVICE, "Generic Access");
        attributes.put(GENERIC_ATTRIBUTE_SERVICE, "Generic Attribute");
        attributes.put(SMARTPRO_SERVICE, "SmartPro Service");

        // Characteristics.
        attributes.put(SMARTPRO_RX_TX_CHARACTERISTIC,"RX/TX data");
    }

    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }
}