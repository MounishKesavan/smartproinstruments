package com.smartpro.smartcer.Model;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Typeface;
import android.net.Uri;

import com.smartpro.smartcer.Activity.MainActivity;
import com.smartpro.smartcer.Control.DirectoryManager;
import com.smartpro.smartcer.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * Created by developer@gmail.com on 1/23/16 AD.
 */
public class DiamondCerImageV2 {
    protected Context context;
    protected int descValuePositionX = 1150;
    protected int descPositionX = 750;
    protected int descPositionY = 540;
    public DirectoryManager directoryManager;

    public DiamondCerImageV2(Context context) {
        this.context = context;
        directoryManager = new DirectoryManager((MainActivity) context);
    }

    public Bitmap getTemplate() {
        Bitmap bmp = BitmapFactory.decodeResource(context.getResources(), R.drawable.diamond_certification);
        if (bmp.getWidth() == 3512 && bmp.getHeight() == 2482) {
            return bmp;
        }
        return Bitmap.createScaledBitmap(bmp, 3512, 2482, true);
    }

    public File saveImage(DiamondCerModel model) {
        Bitmap templateBitMap = getTemplate();
        Bitmap mutableBitmap = templateBitMap.copy(Bitmap.Config.ARGB_8888, true);
        templateBitMap.recycle();
        Canvas canvas = new Canvas(mutableBitmap);
        Paint paint = getDescriptionFontPaint();
        canvas.drawBitmap(mutableBitmap, 0, 0, paint);

        //SPI & Date
        canvas.drawText("SPI " + model.getSPINo(), descPositionX, 320, paint);
        canvas.drawText(model.getIssueDate(), descPositionX, 400, paint);
        canvas.drawText("Seller name: " + model.sellerName, descPositionX, 480, paint);

        //Shape Gallery
        if (model.diamondPropertiesModel.shapeResourceId != 0) {
            InputStream is = context.getResources().openRawResource(model.diamondPropertiesModel.shapeResourceId);
            Bitmap bmp = BitmapFactory.decodeStream(is);
            int shapeWidth = (int) (bmp.getWidth() * 2.2);
            int shapeHeight = (int) (bmp.getHeight() * 2.2);
            bmp = Bitmap.createScaledBitmap(bmp, shapeWidth, shapeHeight, true);
            canvas.drawBitmap(bmp, 2800 - (shapeWidth/2), 340, getBitmapPaint());
        }

        //Photo
        if (model.diamondPropertiesModel.photo != null) {
            canvas.drawBitmap(Bitmap.createScaledBitmap(model.diamondPropertiesModel.photo, 960, 960, true), 2320, 1250, getBitmapPaint());
        }

        //Property Section
        descPositionY = 580;
        printPropertyValue(canvas, paint, model.diamondTestsModel.testResult + " Grading Report");
        /*if (model.gaugeTestsModel != null && model.gaugeTestsModel.cutStyle != null
                && !model.gaugeTestsModel.cutStyle.equals("")) {
            printPropertyValue(canvas, paint, model.gaugeTestsModel.cutStyle + ", " + model.diamondPropertiesModel.shape);
        } else {
            printPropertyValue(canvas, paint, model.diamondPropertiesModel.shape);
        }*/
        printPropertyValue(canvas, paint, model.diamondPropertiesModel.shape);
        printPropertyValue(canvas, paint, getMeasurementText(model.gaugeTestsModel));

        descPositionY = 1060;
        printPropertyValue(canvas, paint, getWeightText(model.scaleTestsModel));
        printPropertyValue(canvas, paint, model.diamondPropertiesModel.color);
        printPropertyValue(canvas, paint, model.diamondPropertiesModel.clarity);
        printPropertyValue(canvas, paint, model.diamondPropertiesModel.mounted);

        descPositionY = 1580;
        printDescriptionLine(canvas, paint, model.diamondPropertiesModel.description);

        // Device Section
        descPositionX = 1165;
        descPositionY = 2050;
        Paint devicePaint = getDeviceFontPaint();
        printDeviceLine(canvas, devicePaint, getTestedByDevice(model));

        // Out of scope Section
        descPositionY = 2090;
        printDeviceLine(canvas, devicePaint, "Color, Clarity grade are not tested by SmartPro app.");

        File file = directoryManager.getCertificationImageFile(model);
        try {
            FileOutputStream out = new FileOutputStream(file);
            mutableBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();
            mutableBitmap.recycle();
        } catch (Exception e) {
            e.printStackTrace();
        }

        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        Uri contentUri = Uri.fromFile(file);
        mediaScanIntent.setData(contentUri);
        context.sendBroadcast(mediaScanIntent);
        return file;
    }

    protected void printPropertyValue(Canvas canvas, Paint paint, String string) {
        canvas.drawText(string, descValuePositionX, descPositionY, paint);
        descPositionY = descPositionY + 100;
    }

    protected void printDescriptionLine(Canvas canvas, Paint paint, String str) {
        canvas.drawText(str, descPositionX, descPositionY, paint);
        descPositionY = descPositionY + 100;

        /*String[] lines = str.split("\n");
        float txtSize = -paint.ascent() + paint.descent();

        if (paint.getStyle() == Paint.Style.FILL_AND_STROKE || paint.getStyle() == Paint.Style.STROKE){
            txtSize += paint.getStrokeWidth(); //add stroke width to the text size
        }
        float lineSpace = txtSize * 0.2f;  //default line spacing

        for (int i = 0; i < lines.length; ++i) {
            canvas.drawText(lines[i], descPositionX, descPositionY + (txtSize + lineSpace) * i, paint);
        }*/
    }

    protected void printDeviceLine(Canvas canvas, Paint paint, String string) {
        canvas.drawText(string, descPositionX, descPositionY, paint);
    }

    protected String getMeasurementText(GaugeTestsModel measurementsModel) {
        if (measurementsModel == null) {
            return "";
        }

        String measurementsText = "";
        String dimensionText = "";
        if (measurementsModel.width > 0f || measurementsModel.length > 0f || measurementsModel.depth > 0f) {
            if (measurementsModel.width > 0.0f) {
                measurementsText = "" + measurementsModel.width;
                dimensionText = "w";
            }
            if (measurementsModel.length > 0.0f) {
                if (measurementsText == "") {
                    measurementsText = "" + measurementsModel.length;
                    dimensionText = "l";
                } else {
                    measurementsText = measurementsText + " * " + measurementsModel.length;
                    dimensionText = dimensionText + "*l";
                }
            }
            if (measurementsModel.depth > 0.0f) {
                if (measurementsText == "") {
                    measurementsText = "" + measurementsModel.depth;
                } else {
                    measurementsText = measurementsText + " * " + measurementsModel.depth;
                    dimensionText = dimensionText + "*d";
                }
            }
            if (measurementsText != "") {
                measurementsText = measurementsText + "(" + dimensionText + " " + measurementsModel.diameterUnit + ")";
            }
        }
        return measurementsText;
    }

    protected String getWeightText(ScaleTestsModel scaleTestsModel) {
        if (scaleTestsModel == null) {
            return "";
        }

        String weightValue = "";
        if (scaleTestsModel.value > 0.0f) {
            weightValue = "" + scaleTestsModel.value + " " + scaleTestsModel.getUnitText();
        }
        return weightValue;
    }

    protected String getTestedByDevice (DiamondCerModel diamondCerModel) {
        String testedBy = "Tested by ";
        String smartProDevice = "";
        String gaugeDeviceName = "";
        if (diamondCerModel.diamondTestsModel.isTestedBySmartProDevice) {
            smartProDevice = diamondCerModel.diamondTestsModel.smartProDeviceName;
        }
        if (diamondCerModel.gaugeTestsModel != null && diamondCerModel.gaugeTestsModel.isTestedBySmartProDevice) {
            gaugeDeviceName = diamondCerModel.gaugeTestsModel.smartProDeviceName;
            if (smartProDevice == "") {
                smartProDevice = diamondCerModel.gaugeTestsModel.smartProDeviceName;
            } else {
                smartProDevice = smartProDevice + ", ";
                smartProDevice = smartProDevice + diamondCerModel.gaugeTestsModel.smartProDeviceName;
            }
        }
        if (diamondCerModel.scaleTestsModel != null && diamondCerModel.scaleTestsModel.isTestedBySmartProDevice
                && diamondCerModel.scaleTestsModel.smartProDeviceName != gaugeDeviceName) {
            if (smartProDevice == "") {
                smartProDevice = diamondCerModel.scaleTestsModel.smartProDeviceName;
            } else {
                smartProDevice = smartProDevice + ", ";
                smartProDevice = smartProDevice + diamondCerModel.scaleTestsModel.smartProDeviceName;
            }
        }
        testedBy = testedBy + smartProDevice;
        return testedBy;
    }

    public Paint getBitmapPaint() {
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);
        paint.setDither(true);
        return paint;
    }

    public Paint getDescriptionFontPaint() {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Typeface fontFace = Typeface.createFromAsset(context.getAssets(), "fonts/OratorStd.otf");
        paint.setTypeface(fontFace);
        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(11); // Text Size
        paint.setTextSize(50f);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)); // Text Overlapping Pattern
        return paint;
    }

    public Paint getDeviceFontPaint() {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Typeface fontFace = Typeface.createFromAsset(context.getAssets(), "fonts/OratorStd.otf");
        paint.setTypeface(fontFace);
        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(11); // Text Size
        paint.setTextSize(32f);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)); // Text Overlapping Pattern
        return paint;
    }
}
