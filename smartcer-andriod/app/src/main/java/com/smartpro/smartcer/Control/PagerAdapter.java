package com.smartpro.smartcer.Control;

import android.app.Activity;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

import com.smartpro.smartcer.Fragment.CertificationFragment;
import com.smartpro.smartcer.Fragment.DiamondPropertiesFragment;
import com.smartpro.smartcer.Fragment.ReaderTestsFragment;
import com.smartpro.smartcer.Fragment.GaugeTestsFragment;
import com.smartpro.smartcer.Fragment.GemEyeTestsFragment;
import com.smartpro.smartcer.Fragment.HomeIconGroupsFragment;
import com.smartpro.smartcer.Fragment.ScaleTestsFragment;
import com.smartpro.smartcer.Fragment.ScreenTestsFragment;

/**
 * Created by developer@gmail.com on 12/26/15 AD.
 */
public class PagerAdapter extends FragmentPagerAdapter {
    private Activity activity;

    public static final int PAGE_HOME = 0;
    public static final int PAGE_SCREEN_TESTS = 1;
    public static final int PAGE_READER_TESTS = 2;
    public static final int PAGE_GEMEYE_TESTS = 3;
    public static final int PAGE_GAUGE_TESTS = 4;
    //public static final int PAGE_SCALE_TESTS = 5;
    public static final int PAGE_SCALE_TESTS_DUMMY = 99;
    public static final int PAGE_CERTIFICATION = 5;
    public static final int PAGE_DIAMOND_PROPERTY = 6;
    public static final int PAGE_NUM = 7;

    public PagerAdapter(FragmentManager fm, Activity activity) {
        super(fm);
        this.activity = activity;
    }

    public int getCount() {
        return PAGE_NUM;
    }

    public Fragment getItem(int position) {
        if(position == PAGE_HOME)
            return HomeIconGroupsFragment.newInstance(activity);
        else if(position == PAGE_SCREEN_TESTS)
            return ScreenTestsFragment.newInstance(activity);
        else if(position == PAGE_READER_TESTS)
            return ReaderTestsFragment.newInstance(activity);
        else if(position == PAGE_GEMEYE_TESTS)
            return GemEyeTestsFragment.newInstance(activity);
        else if(position == PAGE_GAUGE_TESTS)
            return GaugeTestsFragment.newInstance(activity);
        //else if(position == PAGE_SCALE_TESTS)
        //    return ScaleTestsFragment.newInstance(activity);
        else if(position == PAGE_CERTIFICATION)
            return CertificationFragment.newInstance(activity);
        else if(position == PAGE_DIAMOND_PROPERTY) {
            return DiamondPropertiesFragment.newInstance(activity);
        }

        return null;
    }
}
