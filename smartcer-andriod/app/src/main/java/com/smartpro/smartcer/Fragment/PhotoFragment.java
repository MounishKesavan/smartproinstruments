package com.smartpro.smartcer.Fragment;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Toast;

import com.smartpro.smartcer.Control.LockableViewPager;
import com.smartpro.smartcer.Activity.MainActivity;
import com.smartpro.smartcer.R;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by developer@gmail.com on 12/27/15 AD.
 */
public class PhotoFragment extends Fragment {

    public MainActivity activity;
    private View rootView;
    private LockableViewPager viewPager;

    private GridView gidView;
    private ImageAdapter imageAdapter;

    ArrayList<HashMap<String, Object>> arrList = new ArrayList<HashMap<String, Object>>();
    private Cursor cur;
    private int ImgColInx;

    public static PhotoFragment newInstance(String str, Activity activity) {
        PhotoFragment fragment = new PhotoFragment();
        fragment.activity = (MainActivity) activity;

        return fragment;
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_photo, container, false);
        viewPager = (LockableViewPager) container;

        //setPhotoGrid();

        return rootView;
    }

    public void setPhotoGrid() {
        // gridView1 and imageAdapter
        gidView = (GridView) rootView.findViewById(R.id.gridView);
        gidView.setClipToPadding(false);
        imageAdapter = new ImageAdapter(activity.getApplicationContext());
        gidView.setAdapter(imageAdapter);

        // OnClick
        gidView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v,
                                    int position, long id) {

                String[] imgData = {MediaStore.Images.Media.DATA};
                cur = activity.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        imgData,null,null,null);
                ImgColInx = cur.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                cur.moveToPosition(position);

                // Get Image Path
                String imagePath = cur.getString(ImgColInx);
                Toast.makeText(activity.getApplicationContext(), "Your selected : " + imagePath, Toast.LENGTH_SHORT).show();
                activity.setSelectedPhoto(imagePath);

                viewPager.goToNextView();
            }
        });

        new LoadContentFromServer().execute();
    }

    public class LoadContentFromServer extends AsyncTask<String, String, String> {

        private int imgCount;
        private int imgLimit;
        private int imgCurrent;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

        }

        @Override
        protected String doInBackground(String... params) {

            String[] Thumb = { MediaStore.Images.Media._ID,
                    MediaStore.Images.Media.DATA,
                    MediaStore.Images.Media.ORIENTATION };

            Cursor cur = activity.getContentResolver().query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    Thumb,
                    null,
                    null,
                    MediaStore.Images.Media._ID + " DESC");

            int imgIdIdx = cur.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
            int imgFullPathIdx = cur.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            int orientationIdx = cur.getColumnIndexOrThrow(MediaStore.Images.Media.ORIENTATION);

            imgCount = cur.getCount();

            if (imgCount > 50) {
                imgCount = 50;
            }

            HashMap<String, Object> map;

            int imgID = 0;
            String imgFullPath = "";
            int orientation = 1;
            Display display = activity.getWindowManager().getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            int width = size.x;
            int newWidth = (int) ((width / 100.0) * 32);

            for (int i = 0; i < imgCount; i++) {
                imgCurrent = i;
                cur.moveToPosition(i);
                imgID = cur.getInt(imgIdIdx);
                imgFullPath = cur.getString(imgFullPathIdx);
                orientation = cur.getInt(orientationIdx);
                if (orientation == 0) {
                    try {
                        ExifInterface exif = new ExifInterface(imgFullPath);
                        orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                    } catch (Exception e) {
                        e.getStackTrace();
                    }
                }

                Uri uri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "" + imgID);
                try {
                    Bitmap bitmap = BitmapFactory.decodeStream(activity.getContentResolver().openInputStream(uri));
                    Bitmap newBitmap;
                    Matrix matrix = new Matrix();
                    matrix.setRotate(orientation);

                    if (bitmap != null) {
                        if (bitmap.getHeight() >= bitmap.getWidth()) {
                            newBitmap = Bitmap.createBitmap(
                                    bitmap,
                                    0,
                                    bitmap.getHeight()/2 - bitmap.getWidth()/2,
                                    bitmap.getWidth(),
                                    bitmap.getWidth(),
                                    matrix,
                                    true
                            );
                        } else {
                            newBitmap = Bitmap.createBitmap(
                                    bitmap,
                                    bitmap.getWidth()/2 - bitmap.getHeight()/2,
                                    0,
                                    bitmap.getHeight(),
                                    bitmap.getHeight(),
                                    matrix,
                                    true
                            );
                        }
                        Bitmap scaledNewBitmap = Bitmap.createScaledBitmap(newBitmap, newWidth, newWidth, false);
                        newBitmap.recycle();
                        bitmap.recycle();

                        if (scaledNewBitmap != null) {
                            map = new HashMap<String, Object>();
                            map.put("ImageID", String.valueOf(imgID));
                            map.put("ImageBitmap", scaledNewBitmap);
                            arrList.add(map);

                            publishProgress(String.valueOf((int)((i / (float) imgCount) * 100)));
                        }
                    }
                } catch (IOException e) {
                    e.getStackTrace();
                }
            }
            cur.close();
            return null;
        }

        @Override
        public void onProgressUpdate(String... progress) {
            imageAdapter.notifyDataSetChanged();
        }

        @Override
        protected void onPostExecute(String unused) {

        }
    }



    class ImageAdapter extends BaseAdapter {

        private Context mContext;

        public ImageAdapter(Context context) {
            mContext = context;
        }

        public int getCount() {
            return arrList.size();
        }

        public Object getItem(int position) {
            return arrList.get(position);
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView imageView;
            if (convertView == null) {
                imageView = new ImageView(mContext);
            } else {
                imageView = (ImageView) convertView;
            }
            imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            imageView.setPadding(8, 8, 8, 8);
            imageView.setImageBitmap((Bitmap)arrList.get(position).get("ImageBitmap"));

            return imageView;
        }

    }

}
