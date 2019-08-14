package com.diemme.temperaturereader;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import static java.security.AccessController.getContext;

/**
 * Created by XP011224 on 01/11/2017.
 */

public class BleScannerAdapter extends BaseAdapter {
    private final String BLESCAN_DEBUG ="BLESCANNERADAPTER";
    private ArrayList<BluetoothDevice> mDevices;
    private ArrayList<byte[]> mRecords;
    private ArrayList<Integer> mRSSIs;
    private ArrayList<Integer> mTemperatures;
    private LayoutInflater mInflater;

    public BleScannerAdapter() {
        super();
        mDevices  = new ArrayList<BluetoothDevice>();
        mRecords = new ArrayList<byte[]>();
        mRSSIs = new ArrayList<Integer>();
        mTemperatures = new ArrayList<>();
        //mInflater = par.getLayoutInflater();
    }

    public void addDevice(BluetoothDevice device, int rssi, byte[] scanRecord, int temperature) {
        Log.d(BLESCAN_DEBUG,"Add Device");
        showdevices();
        if(mDevices.contains(device) == false) {
            Log.d(BLESCAN_DEBUG,"New Device detected...add to list...");
            mDevices.add(device);
            mRSSIs.add(rssi);
            mRecords.add(scanRecord);
            mTemperatures.add(temperature);
        }
    }

    public BluetoothDevice getDevice(int index) {
        return mDevices.get(index);
    }

    public int getRssi(int index) {
        return mRSSIs.get(index);
    }

    public int getTemperature(int index){ return mTemperatures.get(index);}

    public void clearList() {
        mDevices.clear();
        mRSSIs.clear();
        mRecords.clear();
    }

    @Override
    public int getCount() {
        return mDevices.size();
    }

    @Override
    public Object getItem(int position) {
        return getDevice(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // get already available view or create new if necessary
        FieldReferences fields;
        if (convertView == null) {
            Log.d(BLESCAN_DEBUG,"Inflating");
            //Activity ma = ((Activity)  getContext());
            //mInflater=ma.getLayoutInflater();
            convertView = mInflater.inflate(R.layout.temperature_scanner, null);
            fields = new FieldReferences();
            fields.deviceAddress = (TextView)convertView.findViewById(R.id.DeviceAddress_TextView);
            fields.deviceName    = (TextView)convertView.findViewById(R.id.DeviceName_TextView);
            fields.deviceRssi    = (TextView)convertView.findViewById(R.id.Rssi_TextView);
            fields.deviceTemperature = (TextView) convertView.findViewById(R.id.Temp_TextView);
            convertView.setTag(fields);
        } else {
            fields = (FieldReferences) convertView.getTag();
        }

        // set proper values into the view
        BluetoothDevice device = mDevices.get(position);
        int rssi = mRSSIs.get(position);
        String rssiString = (rssi == 0) ? "N/A" : rssi + " db";
        String name = device.getName();
        String address = device.getAddress();
        if(name == null || name.length() <= 0) name = "Unknown Device";

        int temperature= mTemperatures.get(position);
        String temperatureString = temperature + " C";

        fields.deviceName.setText(name);
        fields.deviceAddress.setText(address);
        fields.deviceRssi.setText(rssiString);
        fields.deviceTemperature.setText(temperatureString);
        Log.d(BLESCAN_DEBUG,"Setup fields position:"+position+" "+ name + " " + address+ " " + temperature);
        return convertView;
    }

    private class FieldReferences {
        TextView deviceName;
        TextView deviceAddress;
        TextView deviceRssi;
        TextView deviceTemperature;
    }

    private void showdevices(){
        int i=0;
        while (i<mDevices.size()){
            Log.d(BLESCAN_DEBUG,"mDevices: "+i+ " "+ mDevices.get(i).getName());
            i++;
        }
    }


}

