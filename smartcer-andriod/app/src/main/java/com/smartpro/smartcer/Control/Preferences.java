package com.smartpro.smartcer.Control;

import android.content.Context;
import android.content.SharedPreferences;
import com.smartpro.smartcer.Activity.MainActivity;

/**
 * Created by developer on 5/15/2017 AD.
 */

public class Preferences {
    protected MainActivity activity;

    public Preferences(MainActivity activity) {
        this.activity = activity;
    }

    public String GetSellerName() {
        SharedPreferences sharedPref = activity.getPreferences(Context.MODE_PRIVATE);
        return sharedPref.getString("SELLER_NAME", "");
    }

    public void SetSellerName(String name) {
        SharedPreferences sharedPref = activity.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("SELLER_NAME", name);
        editor.commit();
    }

    public int GetCertRunningNo() {
        SharedPreferences sharedPref = activity.getPreferences(Context.MODE_PRIVATE);
        return sharedPref.getInt("CERT_RUNNING_NO", 0);
    }

    public void SetCertRunningNo(int runningNo) {
        SharedPreferences sharedPref = activity.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt("CERT_RUNNING_NO", runningNo);
        editor.commit();
    }
}
