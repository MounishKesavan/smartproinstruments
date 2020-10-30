package com.smartpro.smartcer.Fragment;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.content.pm.PackageInfo;
import com.smartpro.smartcer.Control.LockableViewPager;
import com.smartpro.smartcer.Activity.MainActivity;
import com.smartpro.smartcer.R;
import com.smartpro.smartcer.Control.ResizableImageView;
import java.io.File;

/**
 * Created by developer@gmail.com on 12/26/15 AD.
 */
public class HomeIconGroupsFragment extends Fragment {
    protected MainActivity activity;
    private View rootView;
    private LockableViewPager viewPager;
    protected File file;

    public static HomeIconGroupsFragment newInstance(Activity activity) {
        HomeIconGroupsFragment fragment = new HomeIconGroupsFragment();
        fragment.activity = (MainActivity) activity;
        return fragment;
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_home_icon_group, container, false);
        viewPager = (LockableViewPager) container;
        setIconButton();
        setAppVersion();
        return rootView;
    }

    private void setIconButton() {
        ResizableImageView diamondIcon = (ResizableImageView) rootView.findViewById(R.id.home_diamond_icon);
        diamondIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                viewPager.goToDiamondTestsView();
            }
        });

        ResizableImageView gemStoneIcon = (ResizableImageView) rootView.findViewById(R.id.home_gemstone_icon);
        gemStoneIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                viewPager.goToGemStoneTestsView();
            }
        });

        ResizableImageView gaugeIcon = (ResizableImageView) rootView.findViewById(R.id.home_gauge_icon);
        gaugeIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                viewPager.goToGaugeTestsView();
            }
        });

        ResizableImageView cerIcon = (ResizableImageView) rootView.findViewById(R.id.home_cer_icon);
        cerIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                viewPager.goToCertificationView();
            }
        });

        ResizableImageView screenIcon = (ResizableImageView) rootView.findViewById(R.id.home_screen_icon);
        screenIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                viewPager.goToScreenTestsView();
            }
        });
    }

    private void setAppVersion() {
        TextView txtVersion = (TextView) rootView.findViewById(R.id.app_version);
        try {
            PackageInfo pInfo = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0);
            txtVersion.setText(pInfo.versionName);
        }
        catch (PackageManager.NameNotFoundException ex){
        }
    }
}
