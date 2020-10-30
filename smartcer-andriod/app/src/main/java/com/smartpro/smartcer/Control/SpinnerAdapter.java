package com.smartpro.smartcer.Control;

/**
 * Created by developer on 9/10/2016 AD.
 */
import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.smartpro.smartcer.R;

import java.util.ArrayList;

public class SpinnerAdapter extends ArrayAdapter<SpinnerItem> {
    protected int groupid;
    protected Activity context;
    protected ArrayList<SpinnerItem> list;
    protected LayoutInflater inflater;

    public SpinnerAdapter(Activity context, int groupid, int id, ArrayList<SpinnerItem> list){
        super(context, id, list);
        this.list = list;
        inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.groupid = groupid;
    }

    public View getView(int position, View convertView, ViewGroup parent ){
        View itemView = inflater.inflate(groupid,parent,false);
        ImageView imageView = (ImageView)itemView.findViewById(R.id.spinner_item_image);
        imageView.setImageResource(list.get(position).getImageId());
        imageView.setVisibility(View.GONE);

        TextView textView = (TextView)itemView.findViewById(R.id.spinner_item_text);
        textView.setText(list.get(position).getText());
        return itemView;
    }

    public View getDropDownView(int position, View convertView, ViewGroup parent){
        View itemView = inflater.inflate(groupid,parent,false);
        ImageView imageView = (ImageView)itemView.findViewById(R.id.spinner_item_image);
        imageView.setImageResource(list.get(position).getImageId());
        TextView textView = (TextView)itemView.findViewById(R.id.spinner_item_text);
        textView.setText(list.get(position).getText());
        return itemView;
    }
}
