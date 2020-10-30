package com.smartpro.smartcer.Fragment;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.smartpro.smartcer.Control.LockableViewPager;
import com.smartpro.smartcer.Activity.MainActivity;
import com.smartpro.smartcer.R;
import com.smartpro.smartcer.Control.ResizableImageView;

/**
 * Created by developer@gmail.com on 2/22/16 AD.
 */
public class CertificationFragment extends Fragment {
    public MainActivity activity;
    private View rootView;
    private LockableViewPager viewPager;

    public static CertificationFragment newInstance(Activity activity) {
        CertificationFragment fragment = new CertificationFragment();
        fragment.activity = (MainActivity) activity;
        return fragment;
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_certification, container, false);
        viewPager = (LockableViewPager) container;

        setCertificationAction();
        setActionButton();

        return rootView;
    }

    public void setCertificationAction() {
        Button btnDiamondCert = (Button) rootView.findViewById(R.id.btn_cert_diamond_cert);
        btnDiamondCert.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                viewPager.goToDiamondProperty();
            }
        });
    }

    public void setActionButton() {
        ResizableImageView actionDiamond = (ResizableImageView) rootView.findViewById(R.id.action_reader_tests);
        actionDiamond.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                viewPager.goToDiamondTestsView();
            }
        });
        ResizableImageView actionScreen = (ResizableImageView) rootView.findViewById(R.id.action_screen_tests);
        actionScreen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                viewPager.goToScreenTestsView();
            }
        });
        ResizableImageView actionGemStone = (ResizableImageView) rootView.findViewById(R.id.action_gemstone_tests);
        actionGemStone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                viewPager.goToGemStoneTestsView();
            }
        });
        ResizableImageView actionGauge = (ResizableImageView) rootView.findViewById(R.id.action_gague_tests);
        actionGauge.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                viewPager.goToGaugeTestsView();
            }
        });
        ResizableImageView actionScale = (ResizableImageView) rootView.findViewById(R.id.action_scale_tests);
        actionScale.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                viewPager.goToScaleTestsView();
            }
        });
    }
}
