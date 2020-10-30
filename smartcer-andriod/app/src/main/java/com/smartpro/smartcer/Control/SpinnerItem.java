package com.smartpro.smartcer.Control;

/**
 * Created by developer on 9/10/2016 AD.
 */
public class SpinnerItem {
    String text;
    Integer imageId;

    public SpinnerItem(String text, Integer imageId){
        this.text = text;
        this.imageId = imageId;
    }

    public String getText(){
        return text;
    }

    public Integer getImageId(){
        return imageId;
    }
}
