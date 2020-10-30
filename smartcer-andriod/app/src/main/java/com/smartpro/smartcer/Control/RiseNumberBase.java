package com.smartpro.smartcer.Control;

/**
 * Created by developer@gmail.com on 1/5/16 AD.
 */
public interface RiseNumberBase {
    void start();
    RiseNumberTextView withNumber(float number);
    RiseNumberTextView withNumber(int number);
    RiseNumberTextView setDuration(long duration);
    void setOnEnd(RiseNumberTextView.EndListener callback);
}