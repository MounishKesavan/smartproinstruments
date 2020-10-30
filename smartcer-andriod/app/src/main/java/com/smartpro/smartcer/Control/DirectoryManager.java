package com.smartpro.smartcer.Control;

import android.content.Context;
import android.os.Environment;
import android.content.ContextWrapper;

import com.smartpro.smartcer.Activity.MainActivity;
import com.smartpro.smartcer.Model.DiamondCerModel;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

/**
 * Created by developer@gmail.com on 1/23/16 AD.
 */
public class DirectoryManager {
    public static final String DATE_FORMAT = "yyyyMMdd_HHmmss";
    public static final String IMAGE_PATH = "SmartPro";
    public static final String IMAGE_NAME = "SPI_";
    public static final String DIAMOND_IMAGE_FORMAT = ".jpg";
    public static final String CERTIFICATION_IMAGE_FORMAT = ".png";
    public MainActivity activity;

    public DirectoryManager(MainActivity activity) {
        this.activity = activity;
    }

    public File getImageFolder() {
        File imgFolder = new File(android.os.Environment.getExternalStorageDirectory(), IMAGE_PATH);
        if (!imgFolder.exists()) {
            imgFolder.mkdirs();
        }
        return imgFolder;
    }

    public File getTempCropedImageFile() {
        File tmpFile = null;

        if (Environment.isExternalStorageEmulated()) {
            File tmpDirectory = new File(Environment.getExternalStorageDirectory() + "/SmartPro/");
            if (!tmpDirectory.exists()) {
                tmpDirectory.mkdir();
            }
            tmpFile = new File(tmpDirectory, "tmp_" +  getCurrentDateTime() + ".jpg");
        } else {
            try {
                tmpFile = File.createTempFile("tmp", ".jpg", activity.getFilesDir());
            } catch (IOException e) {
            }
        }

        if(tmpFile.exists()){
            tmpFile.delete();
            try {
                tmpFile.createNewFile();
            } catch (IOException e) {
            }
        }
        return tmpFile;
    }

    public File getDiamondImageFile() {
        Random generator = new Random();
        int n = 10000;
        n = generator.nextInt(n);
        String fileName = IMAGE_NAME + getCurrentDateTime() + n + DIAMOND_IMAGE_FORMAT;
        File file = new File(getImageFolder(), fileName);
        return file;
    }

    public File getCertificationImageFile(DiamondCerModel model) {
        /*Random generator = new Random();
        int n = 10000;
        n = generator.nextInt(n);
        String fileName = IMAGE_NAME + getCurrentDateTime() + n + CERTIFICATION_IMAGE_FORMAT;
        */

        //Use spi
        String fileName = IMAGE_NAME + model.getSPINo() + CERTIFICATION_IMAGE_FORMAT;

        File file = new File(getImageFolder(), fileName);
        return file;
    }

    public String getCurrentDateTime() {
        SimpleDateFormat sdfPic = new SimpleDateFormat(DATE_FORMAT);
        return sdfPic.format(new Date()).replace(" ", "");
    }
}
