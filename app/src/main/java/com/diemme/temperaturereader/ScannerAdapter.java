package com.diemme.temperaturereader;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Created by XP011224 on 03/11/2017.
 */

public class ScannerAdapter extends ArrayAdapter<TemperatureReading> {
    public ScannerAdapter(Context context, int resource, List<TemperatureReading> listTemp){
        super(context,resource, listTemp);
    }
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = ((Activity) getContext()).getLayoutInflater().inflate(R.layout.item_temperature, parent, false);
        }

        //TextView roomTextView = (TextView)convertView.findViewById(R.id.idRoomLabelTextView);
        //TextView temperatureTextView    = (TextView)convertView.findViewById(R.id.idTemLabelTextView);
        //TextView timeTextView = (TextView) convertView.findViewById(R.id.idTimeLabelTextView);
        TextView roomTextView = (TextView)convertView.findViewById(R.id.roomTextView);
        TextView temperatureTextView    = (TextView)convertView.findViewById(R.id.temperatureTextView);
        TextView timeTextView = (TextView) convertView.findViewById(R.id.timeTextView);
        TextView rssiTextView =(TextView) convertView.findViewById(R.id.rssiTextView) ;

        TemperatureReading mTR = getItem(position);
        Date date = new Date(mTR.getTimestamp()) ;;
        SimpleDateFormat dateFormat = new SimpleDateFormat("E dd.MM HH.mm");
        String dateString= dateFormat.format(date);

        //idTextView.setText(device.getId().toString());
        roomTextView.setText(mTR.getRoom());
        temperatureTextView.setText(mTR.getTemperature()+"C   ");
        timeTextView.setText(dateString);
        Log.d("ScannerAdapter","rssi:"+mTR.getRssi());
        rssiTextView.setText(mTR.getRssi()+"dbm");


        return convertView;
}
}
