package com.smartpro.smartcer.Fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Toast;

import com.smartpro.smartcer.Control.GMailSender;
import com.smartpro.smartcer.Control.Preferences;
import com.smartpro.smartcer.Model.DiamondCerImageV2;
import com.smartpro.smartcer.Control.DirectoryManager;
import com.smartpro.smartcer.Control.LockableViewPager;
import com.smartpro.smartcer.Activity.MainActivity;
import com.smartpro.smartcer.Model.DiamondCerModel;
import com.smartpro.smartcer.Model.DiamondPropertiesModel;
import com.smartpro.smartcer.Model.GaugeTestsModel;
import com.smartpro.smartcer.Model.ScaleTestsModel;
import com.smartpro.smartcer.Model.DiamondTestsModel;
import com.smartpro.smartcer.R;
import com.smartpro.smartcer.Control.ResizableImageView;
import com.smartpro.smartcer.Control.SpinnerAdapter;
import com.smartpro.smartcer.Control.SpinnerItem;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by developer@gmail.com on 12/30/15 AD.
 */
public class DiamondPropertiesFragment extends Fragment {
    protected MainActivity activity;
    protected View rootView;
    protected LockableViewPager viewPager;
    protected ProgressDialog progressDialog;

    public final int CAMERA_REQUEST = 1888;
    public final int PIC_CROP = 1889;
    public final int CHOOSE_PHOTO = 1890;

    protected File photoFile;
    protected File certificationFile;
    protected Uri croppedImageUri;
    protected Bitmap croppedImage;
    protected boolean isHaveAPhoto;
    protected ResizableImageView cerImage;

    protected AlertDialog dialog;
    protected Dialog measurementDialog;
    protected Dialog weightDialog;
    protected Preferences preferences;
    protected DirectoryManager directoryManager;

    // New shape, shapes.put("8","Rectangular");
    // Remove Tapered Baguette

    String[] shapeItems = { "", "Asscher", "Baguette", "Cushion", "Emerald", "Heart", "Marquise","Oval", "Pear",
            "Rectangular",
            "Round", "Princess", "Radiant", "Trillion" };

    String[] shapeImgs = { "", "dshape_asscher_tv", "dshape_baguette_tv", "dshape_cushion_tv", "dshape_emerald_tv",
            "dshape_heart_tv", "dshape_marquise_tv", "dshape_oval_tv", "dshape_pear_tv",
            "dshape_rectangular_tv",
            "dshape_round_tv", "dshape_princess_tv", "dshape_radiant_tv", "dshape_trillion_tv" };

    String[] colorItems = { "","D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z" };
    String[] clarityItems = { "", "FL","IF","VVS1","VVS2","VS1","VS2","SI1","SI2","I1","I2","I3" };
    String[] clarityImgs = { "", "clarity_fl","clarity_if","clarity_vvs1","clarity_vvs2","clarity_vs1","clarity_vs2","clarity_si1","clarity_si2","clarity_i1","clarity_i2","clarity_i3" };

    public static DiamondPropertiesFragment newInstance(Activity activity) {
        DiamondPropertiesFragment fragment = new DiamondPropertiesFragment();
        fragment.activity = (MainActivity) activity;
        return fragment;
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_diamond_properties, container, false);
        viewPager = (LockableViewPager) container;
        activity.setDiamondPropertiesFragment(this);
        preferences = new Preferences(activity);
        directoryManager = new DirectoryManager(activity);

        setPhotoAction();
        setSpinner();
        setActionButton();
        setOK();
        setNewTest();

        setDialog();
        loadDiamondCerModel();

        return rootView;
    }

    public void loadDiamondCerModel() {
        EditText sellerName = (EditText) rootView.findViewById(R.id.sellername);
        sellerName.setText(preferences.GetSellerName());

        DiamondCerModel diamondCerModel = activity.getDiamondCerModel();
        if (diamondCerModel.diamondTestsModel != null) {
            setTestedBy(diamondCerModel.diamondTestsModel);
        }
        if (diamondCerModel.gaugeTestsModel != null) {
            setMeasurement(diamondCerModel.gaugeTestsModel);
        }
        if (diamondCerModel.scaleTestsModel != null) {
            setWeight(diamondCerModel.scaleTestsModel);
        }

        setShape(diamondCerModel.getPropertiesModel().shape);

        //For initial purpose
        Spinner spinnerColor = (Spinner) rootView.findViewById(R.id.color);
        spinnerColor.setSelection(1);
        Spinner spinnerClarity = (Spinner) rootView.findViewById(R.id.clarity);
        spinnerClarity.setSelection(1);
        Spinner spinner = (Spinner) rootView.findViewById(R.id.mounted);
        spinner.setSelection(1);
        EditText desc = (EditText) rootView.findViewById(R.id.description);
        desc.setText("");
    }

    public void setTestedBy(DiamondTestsModel model) {
        Button spinnerTestedBy = (Button) rootView.findViewById(R.id.testedby);
        if (model.isTestedBySmartProDevice) {
            spinnerTestedBy.setText(model.testResult + " (By "+ model.smartProDeviceName  + ")");
        } else if (model.testResult == "Diamond" || model.testResult == "CVD/HPHT" || model.testResult == "Moissanite") {
            spinnerTestedBy.setText(model.testResult + " (By user)");
        } else {
            spinnerTestedBy.setText("");
        }
    }

    public void setMeasurement(GaugeTestsModel model) {
        Button spinnerMeasurements = (Button) rootView.findViewById(R.id.measurements);
        if (model.width > 0.0f || model.length > 0.0f || model.depth > 0.0f) {
            String value = "";
            if (model.width > 0.0f) {
                value = "" + model.width;
            }
            if (model.length > 0.0f) {
                if (value == "") {
                    value = "" + model.length;
                } else {
                    value = value + " * " + model.length;
                }
            }
            if (model.depth > 0.0f) {
                if (value == "") {
                    value = "" + model.depth;
                } else {
                    value = value + " * " + model.depth;
                }
            }
            if (model.isTestedBySmartProDevice) {
                spinnerMeasurements.setText(value + " " + model.diameterUnit + " (By " + model.smartProDeviceName + ")");

                if (activity.getDiamondCerModel().IsCvdHphtGradingReport()) {
                    DiamondTestsModel m = new DiamondTestsModel(false,"","");
                    activity.getDiamondCerModel().diamondTestsModel = m;
                    setTestedBy(m);
                }
            } else {
                spinnerMeasurements.setText(value + " " + model.diameterUnit);
            }
        } else {
            spinnerMeasurements.setText("");
        }
    }

    public void setShape(String shape) {
        Spinner spinnerShape = (Spinner) rootView.findViewById(R.id.shape);
        if (shape != null && !shape.equals("")) {
            spinnerShape.setSelection(Arrays.asList(shapeItems).indexOf(shape));
        } else {
            spinnerShape.setSelection(1);
        }
    }

    public void setWeight(ScaleTestsModel model) {
        Button spinnerWeight = (Button) rootView.findViewById(R.id.weight);

        if (model.value > 0.0f) {
            if (model.isTestedBySmartProDevice) {
                spinnerWeight.setText("" + model.value + " " + model.getUnitText() + " (By " + model.smartProDeviceName + ")");
            } else {
                spinnerWeight.setText("" + model.value + " " + model.getUnitText());
            }
        } else {
            spinnerWeight.setText("");
        }
    }

    public void setPhotoAction() {
        ResizableImageView diamondPhoto = (ResizableImageView) rootView.findViewById(R.id.diamond_photo);
        diamondPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                displayMenuDialog();
            }
        });
        LinearLayout editDiamondPhotoPanel = (LinearLayout) rootView.findViewById(R.id.edit_diamond_photo_panel);
        editDiamondPhotoPanel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                displayMenuDialog();
            }
        });
    }

    public void displayMenuDialog() {
        String[] options = new String[]{"Take a photo", "Choose from Gallery", "Delete"};
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(activity, android.R.layout.select_dialog_item, options);
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) {
                    TakeAPhotoAction();
                } else if (which == 1) {
                    choosePhotoFromGallery();
                } else {
                    DeletePhotoAction();
                }
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public void TakeAPhotoAction() {
        if (!hasCamera()) {
            Toast.makeText(activity, "Camera is not available", Toast.LENGTH_SHORT).show();
            return;
        }
        photoFile = directoryManager.getDiamondImageFile();
        Uri outputFileUri = Uri.fromFile(photoFile);

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
        startActivityForResult(intent, CAMERA_REQUEST);
    }

    private boolean hasCamera(){
        return activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_REQUEST && resultCode == Activity.RESULT_OK) {
            Uri picUri = Uri.fromFile(photoFile);
            cropPhoto(picUri);
        } else if (requestCode == PIC_CROP) {
            croppedImage = null;
            if (data != null) {
                if (data.getData() != null) {
                    try {
                        croppedImage = MediaStore.Images.Media.getBitmap(activity.getContentResolver(), data.getData());
                    } catch (IOException ex) {
                        Log.e("io", ex.getMessage());
                    }
                }

                if (croppedImage == null) {
                    Bundle extras = data.getExtras();
                    if (extras != null) {
                        croppedImage = extras.getParcelable("data");
                    }
                }
            }

            if (croppedImage != null) {
                setPhoto(croppedImage);
                //scanFile();
            }
        } else if (requestCode == CHOOSE_PHOTO && resultCode == Activity.RESULT_OK) {
            Uri picUri = data.getData();
            cropPhoto(picUri);
        }
    }

    protected void cropPhoto(Uri picUri) {
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setDataAndType(picUri, "image/*");
        intent.putExtra("crop", "true");
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        intent.putExtra("scale", true);
        intent.putExtra("outputX", 960);
        intent.putExtra("outputY", 960);
        intent.putExtra("return-data", false);
        File f = directoryManager.getTempCropedImageFile();
        try {
            f.createNewFile();
        } catch (IOException ex) {
        }
        croppedImageUri = Uri.fromFile(f);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, croppedImageUri);
        intent.putExtra("outputFormat",Bitmap.CompressFormat.JPEG.toString());
        startActivityForResult(intent, PIC_CROP);

        /*Intent photoPickerIntent = new Intent(
                Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        photoPickerIntent.setType("image/*");
        photoPickerIntent.putExtra("scaleType", "centerCrop");
        photoPickerIntent.putExtra("crop", "true");
        photoPickerIntent.putExtra("aspectX", 1);
        photoPickerIntent.putExtra("aspectY", 1);
        photoPickerIntent.putExtra("outputX", 960);
        photoPickerIntent.putExtra("outputY", 960);
        photoPickerIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(tmpFile));
        photoPickerIntent.putExtra("outputFormat",Bitmap.CompressFormat.JPEG.toString());
        startActivityForResult(photoPickerIntent, PIC_CROP);*/
    }

    /*
    protected void scanFile() {
        if (photoFile != null) {
            Intent mediaScanIntent = new Intent("android.intent.action.MEDIA_SCANNER_SCAN_FILE");
            Uri contentUri = Uri.fromFile(photoFile);
            mediaScanIntent.setData(contentUri);
            activity.sendBroadcast(mediaScanIntent);
        }
    }*/

    protected void setPhoto(Bitmap bitmap) {
        ResizableImageView diamondPhoto = (ResizableImageView) rootView.findViewById(R.id.diamond_photo);
        diamondPhoto.setImageBitmap(bitmap);
        croppedImage = bitmap;
        isHaveAPhoto = true;
        activity.log("setPhoto sz","Width "+bitmap.getWidth() + ", Height "+bitmap.getHeight(), false);
    }

    protected void choosePhotoFromGallery() {
        photoFile = null;
        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("image/*");
        startActivityForResult(photoPickerIntent, CHOOSE_PHOTO);
    }

    protected void DeletePhotoAction() {
        isHaveAPhoto = false;
        croppedImage = null;
        ResizableImageView diamondPhoto = (ResizableImageView) rootView.findViewById(R.id.diamond_photo);
        diamondPhoto.setImageResource(R.drawable.diamond_blank_photo);
    }

    public void setActionButton() {
        ResizableImageView actionDaimond = (ResizableImageView) rootView.findViewById(R.id.action_reader_tests);
        actionDaimond.setOnClickListener(new View.OnClickListener() {
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
        ResizableImageView actionGemstone = (ResizableImageView) rootView.findViewById(R.id.action_gemstone_tests);
        actionGemstone.setOnClickListener(new View.OnClickListener() {
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

    private void setNewTest() {
        Button btnNewTest = (Button) rootView.findViewById(R.id.btnNewTest);
        btnNewTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                AlertDialog.Builder alert = new AlertDialog.Builder(activity);
                alert.setTitle("Certificate");
                alert.setMessage("Do you want to reset all properties?");
                alert.setNegativeButton("Cancel", null);
                alert.setPositiveButton("Ok", new AlertDialog.OnClickListener() {
                    public void onClick(DialogInterface dialog, int arg1) {
                        resetAllProperties();
                    }
                });
                dialog = alert.create();
                dialog.show();
            }
        });
    }

    private void resetAllProperties() {
        DeletePhotoAction();
        activity.setNewDiamondCerModel();
        loadDiamondCerModel();
    }

    private void setOK() {
        Button btnOK = (Button) rootView.findViewById(R.id.btnOK);
        btnOK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                try {
                    EditText sellerNameText = (EditText) rootView.findViewById(R.id.sellername);
                    preferences.SetSellerName(sellerNameText.getText().toString());
                    activity.setSellerName(sellerNameText.getText().toString());

                    if (activity.getDiamondCerModel().diamondTestsModel == null) {
                        AlertDialog.Builder alert = new AlertDialog.Builder(activity);
                        alert.setTitle("Certificate");
                        alert.setMessage("Grading Report is required.");
                        alert.setPositiveButton("OK", new AlertDialog.OnClickListener() {
                            public void onClick(DialogInterface dialog, int arg1) {
                            }
                        });
                        alert.show();
                    } else {

                        if (isValidToGenerate()) {

                            if (isMaterialConflict()) {
                                AlertDialog.Builder alert = new AlertDialog.Builder(activity);
                                alert.setTitle("Certificate");
                                alert.setMessage("Found material conflicts between Grading Report and Primo-I measurement");
                                alert.setPositiveButton("OK", new AlertDialog.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int arg1) {
                                    }
                                });
                                alert.show();
                                return;
                            }

                            progressDialog = ProgressDialog.show(activity, "Get certificate", "Generating certificate ...", true);
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        Spinner spinnerShape = (Spinner) rootView.findViewById(R.id.shape);
                                        String shape = ((SpinnerItem) spinnerShape.getSelectedItem()).getText();
                                        Spinner spinnerColor = (Spinner) rootView.findViewById(R.id.color);
                                        String color = ((SpinnerItem) spinnerColor.getSelectedItem()).getText();
                                        Spinner spinnerClarity = (Spinner) rootView.findViewById(R.id.clarity);
                                        String clarity = ((SpinnerItem) spinnerClarity.getSelectedItem()).getText();
                                        Spinner spinnerMounted = (Spinner) rootView.findViewById(R.id.mounted);
                                        String mounted = ((SpinnerItem) spinnerMounted.getSelectedItem()).getText();
                                        EditText editTextDesc = (EditText) rootView.findViewById(R.id.description);
                                        String desc = editTextDesc.getText().toString();

                                        int runningNo = preferences.GetCertRunningNo() + 1;
                                        activity.setRunningNo(String.format("%03d", runningNo));

                                        DiamondPropertiesModel model = new DiamondPropertiesModel();
                                        model.photo = croppedImage;
                                        model.shape = shape;
                                        model.shapeResourceId = getResouceId("c" + shapeImgs[spinnerShape.getSelectedItemPosition()]);
                                        model.color = color;
                                        model.clarity = clarity;
                                        model.mounted = mounted;
                                        model.description = desc;
                                        activity.setDiamondPropertiesModel(model);
                                        DiamondCerImageV2 cer = new DiamondCerImageV2(getActivity());
                                        certificationFile = cer.saveImage(activity.getDiamondCerModel());
                                        preferences.SetCertRunningNo(runningNo);

                                        if (activity.isInternetAvailable()) {
                                            new Thread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    try {
                                                        GMailSender sender = new GMailSender();
                                                        sender.addAttachment(certificationFile.getAbsolutePath());
                                                        sender.sendMail(activity.getDiamondCerModel());
                                                    } catch (Exception e) {
                                                        activity.log("email", e.getMessage(), false);
                                                    }
                                                }
                                            }).start();
                                        }

                                        activity.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                progressDialog.dismiss();
                                                Toast.makeText(activity, "Generated certificate successfully ", Toast.LENGTH_SHORT).show();

                                                setShare();
                                                setRestartProperty();
                                                setPrint();
                                                nextToShare();
                                            }
                                        });
                                    } catch (Exception ex) {
                                        AlertDialog.Builder alert = new AlertDialog.Builder(activity);
                                        alert.setTitle("Certificate");
                                        alert.setMessage("Have got technical difficulties");
                                        alert.setPositiveButton("OK", new AlertDialog.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int arg1) {
                                            }
                                        });
                                        alert.show();
                                    }
                                }
                            }).start();
                        } else {
                            AlertDialog.Builder alert = new AlertDialog.Builder(activity);
                            alert.setTitle("Certificate");
                            alert.setMessage("would need to be tested by SmartPro Bluetooth device.");
                            alert.setPositiveButton("OK", new AlertDialog.OnClickListener() {
                                public void onClick(DialogInterface dialog, int arg1) {
                                }
                            });
                            alert.show();
                        }
                    }
                } catch (Exception ex) {
                    AlertDialog.Builder alert = new AlertDialog.Builder(activity);
                    alert.setTitle("Certificate");
                    alert.setMessage("Have got technical difficulties");
                    alert.setPositiveButton("OK", new AlertDialog.OnClickListener() {
                        public void onClick(DialogInterface dialog, int arg1) {
                        }
                    });
                    alert.show();
                }
            }
        });
    }

    private boolean isMaterialConflict() {
        String GradingReportMaterial = activity.getDiamondCerModel().diamondTestsModel.testResult;
        return activity.getDiamondCerModel().gaugeTestsModel.isTestedBySmartProDevice &&
                !activity.getDiamondCerModel().gaugeTestsModel.gemStoneType.toLowerCase().equals(GradingReportMaterial.toLowerCase());

    }

    private boolean isValidToGenerate() {
        DiamondCerModel model = activity.getDiamondCerModel();
        return (model.diamondTestsModel != null && model.diamondTestsModel.isTestedBySmartProDevice) ||
                (model.gaugeTestsModel != null && model.gaugeTestsModel.isTestedBySmartProDevice) ||
                (model.scaleTestsModel != null && model.scaleTestsModel.isTestedBySmartProDevice);
    }

    private void setDialog() {
        measurementDialog = new Dialog(activity);
        measurementDialog.setTitle("Measurements in mm.");
        measurementDialog.setContentView(R.layout.dialog_diamond_measurements);

        final EditText widthInput = (EditText) measurementDialog.findViewById(R.id.dialog_measurements_width_input);
        final EditText lengthInput = (EditText) measurementDialog.findViewById(R.id.dialog_measurements_lenght_input);
        final EditText depthInput = (EditText) measurementDialog.findViewById(R.id.dialog_measurements_depth_input);
        Button btnMeasurementsOK = (Button) measurementDialog.findViewById(R.id.dialog_measurements_ok);
        Button btnMeasurementsCancel = (Button) measurementDialog.findViewById(R.id.dialog_measurements_cancel);

        btnMeasurementsOK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                float width = 0.0f;
                float length = 0.0f;
                float depth = 0.0f;
                try {
                    width = Float.parseFloat(widthInput.getText().toString());
                } catch (Exception e){}
                try {
                    length = Float.parseFloat(lengthInput.getText().toString());
                } catch (Exception e){}
                try {
                    depth = Float.parseFloat(depthInput.getText().toString());
                } catch (Exception e){}

                activity.setGaugeTestsModel(new GaugeTestsModel(false,"", width, length, depth, "mm"));
                measurementDialog.dismiss();
            }
        });

        btnMeasurementsCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                measurementDialog.dismiss();
            }
        });


        weightDialog = new Dialog(activity);
        weightDialog.setTitle("Weight in carat");
        weightDialog.setContentView(R.layout.dialog_diamond_weight);

        final EditText weightInput = (EditText) weightDialog.findViewById(R.id.dialog_weight_width_input);
        Button btnWeightOK = (Button) weightDialog.findViewById(R.id.dialog_weight_ok);
        Button btnWeightCancel = (Button) weightDialog.findViewById(R.id.dialog_weight_cancel);

        btnWeightOK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                float weight = 0.0f;
                try {
                    weight = Float.parseFloat(weightInput.getText().toString());
                } catch (Exception e){}

                activity.setScaleTestsModel(new ScaleTestsModel(false,"", weight, "ct"));
                weightDialog.dismiss();
            }
        });

        btnWeightCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                weightDialog.dismiss();
            }
        });
    }

    public void nextToShare() {
        if (certificationFile != null) {
            try {
                ResizableImageView cerImage = (ResizableImageView) rootView.findViewById(R.id.certification_image);
                Uri cerUrl = Uri.fromFile(certificationFile);
                Bitmap certificationBitmap = BitmapFactory.decodeStream(activity.getContentResolver().openInputStream(cerUrl));
                cerImage.setImageBitmap(certificationBitmap);

                setZoom();
            } catch (IOException ex) {
                ex.getStackTrace();
            }
        }

        LinearLayout propertyLayout = (LinearLayout) rootView.findViewById(R.id.property_layout);
        propertyLayout.setVisibility(View.GONE);
        LinearLayout shareLayout = (LinearLayout) rootView.findViewById(R.id.share_layout);
        shareLayout.setVisibility(View.VISIBLE);

        ScrollView sv = (ScrollView) rootView.findViewById(R.id.view_scrollview);
        sv.scrollTo(0, sv.getTop());
    }

    public void setShare() {
        Button shareBtn = (Button) rootView.findViewById(R.id.share_button);
        shareBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (certificationFile != null) {
                    progressDialog = ProgressDialog.show(activity, "Share", "Share to ...", true);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            Intent share = new Intent(Intent.ACTION_SEND);
                            share.setType("image/jpeg");
                            share.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(certificationFile));
                            startActivity(Intent.createChooser(share, "Share certification to"));

                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    progressDialog.dismiss();
                                }
                            });
                        }
                    }).start();
                }
            }
        });
    }

    public void setRestartProperty() {
        Button btnRestartProperty = (Button) rootView.findViewById(R.id.restart_property_button);
        btnRestartProperty.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                LinearLayout propertyLayout = (LinearLayout) rootView.findViewById(R.id.property_layout);
                propertyLayout.setVisibility(View.VISIBLE);
                LinearLayout shareLayout = (LinearLayout) rootView.findViewById(R.id.share_layout);
                shareLayout.setVisibility(View.GONE);
            }
        });
    }

    public void setPrint() {
        Button printBtn = (Button) rootView.findViewById(R.id.restart_tests_button);
        printBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                viewPager.goToHomeIconView();
            }
        });
    }

    public void setZoom() {
        cerImage = (ResizableImageView) rootView.findViewById(R.id.certification_image);
        cerImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                cerImage.setEnabled(false);
                progressDialog = ProgressDialog.show(activity, "View", "View & Zoom ...", true);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Intent intent = new Intent();
                        intent.setAction(Intent.ACTION_VIEW);
                        intent.setDataAndType(Uri.fromFile(certificationFile), "image/*");
                        startActivity(intent);

                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progressDialog.dismiss();
                                cerImage.setEnabled(true);
                            }
                        });
                    }
                }).start();
            }
        });
    }

    public void setSpinner() {
        Button spinnerTestedBy = (Button) rootView.findViewById(R.id.testedby);
        spinnerTestedBy.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View arg0) {
                String[] choices =
                        {"Go tests by Screen-I"
                                , "Go test by Reader-I"
                                , "I guarantee diamond"
                                , "I guarantee CVD/HPHT"
                                , "I guarantee moissanite"};
                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setItems(choices, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            viewPager.goToScreenTestsView();
                        } else if (which == 1) {
                            viewPager.goToDiamondTestsView();
                        } else if (which == 2) {
                            DiamondTestsModel model = new DiamondTestsModel(false,"","Diamond");
                            activity.getDiamondCerModel().diamondTestsModel = model;
                            setTestedBy(model);
                        } else if (which == 3) {

                            if (activity.getDiamondCerModel().IsPrimoTests()) {

                                AlertDialog.Builder alert = new AlertDialog.Builder(activity);
                                alert.setTitle("Tests by Primo-I");
                                alert.setMessage("Primo-I couldn't issue grading report of CVD/HPHT  which is not standard material.  To continue CVD/HPHT, please input measurement, weight by yourself.");
                                alert.setNegativeButton("Cancel", null);
                                alert.setPositiveButton("Ok", new AlertDialog.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int arg1) {
                                        DiamondTestsModel model = new DiamondTestsModel(false,"","CVD/HPHT");
                                        activity.getDiamondCerModel().diamondTestsModel = model;
                                        setTestedBy(model);

                                        activity.setGaugeTestsModel(new GaugeTestsModel(false,"", 0, 0, 0, "mm"));
                                        activity.setScaleTestsModel(new ScaleTestsModel(false,"", 0, "ct"));
                                    }
                                });
                                alert.show();
                            } else {
                                DiamondTestsModel model = new DiamondTestsModel(false,"","CVD/HPHT");
                                activity.getDiamondCerModel().diamondTestsModel = model;
                                setTestedBy(model);
                            }


                        } else if (which == 4) {
                            DiamondTestsModel model = new DiamondTestsModel(false,"","Moissanite");
                            activity.getDiamondCerModel().diamondTestsModel = model;
                            setTestedBy(model);
                        }
                    }
                });
                builder.setNegativeButton("Cancel", null);
                builder.create();
                builder.show();
            }
        });

        Button spinnerMeasurements = (Button) rootView.findViewById(R.id.measurements);
        spinnerMeasurements.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View arg0) {
                String[] choices =
                        { "Go tests by Primo-I"
                                , "I'll input by myself"
                                , "Clear" };
                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setItems(choices, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            if (activity.getDiamondCerModel().IsCvdHphtGradingReport()) {

                                AlertDialog.Builder alert = new AlertDialog.Builder(activity);
                                alert.setTitle("Tests by Primo-I");
                                alert.setMessage("Primo-I couldn't issue grading report of CVD/HPHT  which is not standard material.  To continue Primo-I tests, please choose another grading report.");
                                alert.setNegativeButton("Cancel", null);
                                alert.setPositiveButton("Ok", new AlertDialog.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int arg1) {
                                        DiamondTestsModel model = new DiamondTestsModel(false,"","");
                                        activity.getDiamondCerModel().diamondTestsModel = model;
                                        setTestedBy(model);
                                        viewPager.goToGaugeTestsView();
                                    }
                                });
                                alert.show();
                            } else {
                                viewPager.goToGaugeTestsView();
                            }
                        } else if (which == 1) {
                            displayGaugeInput();
                        } else if (which == 2) {
                            activity.setGaugeTestsModel(new GaugeTestsModel(false,"", 0, 0, 0, "mm"));
                        }
                    }
                });
                builder.setNegativeButton("Cancel", null);
                builder.create();
                builder.show();
            }
        });

        Button spinnerWeight = (Button) rootView.findViewById(R.id.weight);
        spinnerWeight.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View arg0) {
                String[] choices =
                        { "Go tests by Primo-I"
                                , "I'll input by myself"
                                , "Clear" };
                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setItems(choices, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            if (activity.getDiamondCerModel().IsCvdHphtGradingReport()) {

                                AlertDialog.Builder alert = new AlertDialog.Builder(activity);
                                alert.setTitle("Tests by Primo-I");
                                alert.setMessage("Primo-I couldn't issue grading report of CVD/HPHT  which is not standard material.  To continue Primo-I tests, please choose another grading report.");
                                alert.setNegativeButton("Cancel", null);
                                alert.setPositiveButton("Ok", new AlertDialog.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int arg1) {
                                        DiamondTestsModel model = new DiamondTestsModel(false,"","");
                                        activity.getDiamondCerModel().diamondTestsModel = model;
                                        setTestedBy(model);
                                        viewPager.goToGaugeTestsView();
                                    }
                                });
                                alert.show();
                            } else {
                                viewPager.goToGaugeTestsView();
                            }
                        } else if (which == 1) {
                            displayScaleInput();
                        } else if (which == 2) {
                            activity.setScaleTestsModel(new ScaleTestsModel(false,"",0, "ct"));
                        }
                    }
                });
                builder.setNegativeButton("Cancel", null);
                builder.create();
                builder.show();
            }
        });

        Spinner spinnerShape = (Spinner) rootView.findViewById(R.id.shape);
        SpinnerAdapter adapterShape = new SpinnerAdapter(activity, R.layout.spinner_image_text_item,
                R.id.spinner_item_text, getShapeItems());
        spinnerShape.setAdapter(adapterShape);

        spinnerShape.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                Spinner spinnerShape = (Spinner) rootView.findViewById(R.id.shape);
                String selectedShape = ((SpinnerItem) spinnerShape.getItemAtPosition(arg2)).getText();

                Spinner spinnerColor = (Spinner) rootView.findViewById(R.id.color);
                int selectedColor = spinnerColor.getSelectedItemPosition();
                SpinnerAdapter adapterColor = new SpinnerAdapter(activity, R.layout.spinner_image_text_item, R.id.spinner_item_text, getColorItems(selectedShape == ""? "Round" : selectedShape));
                spinnerColor.setAdapter(adapterColor);
                spinnerColor.setSelection(selectedColor);
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });

        Spinner spinnerColor = (Spinner) rootView.findViewById(R.id.color);
        SpinnerAdapter adapterColor = new SpinnerAdapter(activity, R.layout.spinner_image_text_item, R.id.spinner_item_text, getColorItems(""));
        spinnerColor.setAdapter(adapterColor);

        Spinner spinnerClarity = (Spinner) rootView.findViewById(R.id.clarity);
        SpinnerAdapter adapterClarity = new SpinnerAdapter(activity, R.layout.spinner_image_text_item, R.id.spinner_item_text, getClarityItems());
        spinnerClarity.setAdapter(adapterClarity);

        Spinner spinnerMounted = (Spinner) rootView.findViewById(R.id.mounted);
        SpinnerAdapter adapterMounted = new SpinnerAdapter(activity, R.layout.spinner_image_text_item, R.id.spinner_item_text, getMountedItems());
        spinnerMounted.setAdapter(adapterMounted);
    }

    public void displayGaugeInput() {
        measurementDialog.show();
    }

    public void displayScaleInput() {
        weightDialog.show();
    }

    public ArrayList<SpinnerItem> getShapeItems() {
        ArrayList<SpinnerItem> items = new ArrayList<SpinnerItem>();
        for(int i=0; i < shapeItems.length; i++) {
            items.add(new SpinnerItem(shapeItems[i], getResouceId(shapeImgs[i])));
        }
        return items;
    }

    public ArrayList<SpinnerItem> getColorItems(String shape) {
        ArrayList<SpinnerItem> items = new ArrayList<SpinnerItem>();
        for(int i=0; i < colorItems.length; i++) {
            items.add(new SpinnerItem(colorItems[i], getResouceId(getShapeColorImg(shape, colorItems[i].toLowerCase()))));
        }
        return items;
    }

    public ArrayList<SpinnerItem> getClarityItems() {
        ArrayList<SpinnerItem> items = new ArrayList<SpinnerItem>();
        for(int i=0; i < clarityItems.length; i++) {
            items.add(new SpinnerItem(clarityItems[i], getResouceId(clarityImgs[i])));
        }
        return items;
    }

    public ArrayList<SpinnerItem> getMountedItems() {
        String[] mountedItems = { "", "Yes","No" };
        String[] mountedImgs = { "", "mounted","mountloose" };

        ArrayList<SpinnerItem> items = new ArrayList<SpinnerItem>();
        for(int i=0; i < mountedItems.length; i++) {
            items.add(new SpinnerItem(mountedItems[i], getResouceId(mountedImgs[i])));
        }
        return items;
    }

    public String getShapeColorImg(String shapeName, String color) {
        String shape = "";
        if (shapeName == "Asscher") {
            shape = "asscher";
        } else if (shapeName == "Baguette") {
            shape = "baguette";
        } else if (shapeName == "Cushion") {
            shape = "cushion";
        } else if (shapeName == "Emerald") {
            shape = "emerald";
        } else if (shapeName == "Heart") {
            shape = "heart";
        } else if (shapeName == "Marquise") {
            shape = "marquise";
        } else if (shapeName == "Oval") {
            shape = "oval";
        } else if (shapeName == "Pear") {
            shape = "pear";
        } else if (shapeName == "Princess") {
            shape = "princess";
        } else if (shapeName == "Radiant") {
            shape = "radiant";
        } else if (shapeName == "Round") {
            shape = "round";
        } else if (shapeName == "Rectangular") {
            shape = "rectangular";
        } else if (shapeName == "Trillion") {
            shape = "trillion";
        }

        return shape + "_" + color;
    }

    public int getResouceId(String resourceName) {
        return getResources().getIdentifier(resourceName, "drawable", activity.getPackageName());
    }

    @Override
    public void onAttach(final Activity activity) {
        super.onAttach(activity);
        setHasOptionsMenu(true);
    }


}
