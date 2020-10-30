package com.smartpro.smartcer.Control;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * Created by developer@gmail.com on 12/27/15 AD.
 */
public class LockableViewPager extends ViewPager {

    private boolean swipeable = true;

    public LockableViewPager(Context context) {
        super(context);
    }

    public LockableViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (this.swipeable) {
            return super.onTouchEvent(event);
        }

        return false;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (this.swipeable) {
            return super.onInterceptTouchEvent(event);
        }

        return false;
    }

    public void setSwipeable(boolean swipeable) {
        this.swipeable = swipeable;
    }

    public void goToNextView()
    {
        int idx = getCurrentItem();
        setCurrentItem(idx + 1);
    }

    public void goToPreviousView()
    {
        int idx = getCurrentItem();
        setCurrentItem(idx - 1);
    }

    public void goToDiamondTestsView() {
        setCurrentItem(PagerAdapter.PAGE_READER_TESTS);
    }

    public void goToScreenTestsView() {
        setCurrentItem(PagerAdapter.PAGE_SCREEN_TESTS);
    }

    public void goToHomeIconView() {
        setCurrentItem(PagerAdapter.PAGE_HOME);
    }

    public void goToGemStoneTestsView() {
        setCurrentItem(PagerAdapter.PAGE_GEMEYE_TESTS);
    }

    public void goToGaugeTestsView() {
        setCurrentItem(PagerAdapter.PAGE_GAUGE_TESTS);
    }

    public void goToScaleTestsView() {
        //setCurrentItem(PagerAdapter.PAGE_SCALE_TESTS);
    }

    public void goToCertificationView() {
        setCurrentItem(PagerAdapter.PAGE_CERTIFICATION);
    }

    public void goToDiamondProperty() {
        setCurrentItem(PagerAdapter.PAGE_DIAMOND_PROPERTY);
    }

    public void goToWorkingView(int viewId) {
        if (getCurrentItem() != viewId) {
            setCurrentItem(viewId);
        }
    }
}
