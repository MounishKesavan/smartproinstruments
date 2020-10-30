package com.smartpro.smartcer.Model;

import android.graphics.Bitmap;

/**
 * Created by developer@gmail.com on 1/7/16 AD.
 */
public class DiamondPropertiesModel {
    public Bitmap photo;
    public String shape;
    public int shapeResourceId;
    public String color;
    public String clarity;
    public String mounted;
    public String description;

    public DiamondPropertiesModel() {
        this.photo = null;
        this.shape = "";
        this.shapeResourceId = 0;
        this.color = "";
        this.clarity = "";
        this.mounted = "";
        this.description = "";
    }
}
