package com.diemme.temperaturereader;

import android.*;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import static com.facebook.FacebookSdk.getApplicationContext;

/**
 * Created by XP011224 on 07/11/2017.
 */

public class AlarmReceiver extends BroadcastReceiver {
    private TempReaderDbHelper mTempDbHelper;
    private final String LOG_TAG="AlarmReceiver DEBUG";
    private FirebaseDatabase mFirebaseDatabase;
    final int REQUEST_ENABLE_BT = 1001;
    final int RC_SIGN_IN=10002;
    private static final int INTERVAL=8000;
    private static final int INTERVAL_BLE_SCAN=7200*1000; //one hour
    private static final int TEMPERATURE=200;
    private static final int BTDEVICE=201;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 2001;
    private static final int SCAN_PERIOD = 10000; //10sec
    private static final String TEMP_SENSOR_TAG="0c166e3531";
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapater;
    private BluetoothLeScanner mBLEScanner;
    private BleScannerAdapter mBleScannerAdapter=null;
    private ScannerAdapter mScannerAdapter=null;
    private ScanSettings settings;
    private List<ScanFilter> filters;
    private List<TemperatureReading> listTemperatures=new ArrayList<>();
    private boolean mScanning = false;
    private Handler mHandler = new Handler();
    private String deviceAddress;
    private Integer deviceTemperature;
    private Context context;
    private Query mTempQuery;
    private DatabaseReference mTempDBReference;

    @Override
    public void onReceive(Context context, Intent intent) {
        this.context=context;
        listTemperatures=MainActivity.listTemperatures;
        mTempDbHelper=new TempReaderDbHelper(context);
        mFirebaseDatabase = FirebaseDatabase.getInstance();

        if (mTempDBReference==null){
            mTempDBReference=mFirebaseDatabase.getReference().child("tempdb");
        }

        listTemperatures.clear();
        Log.d(LOG_TAG,"Triggered");
        Log.d(LOG_TAG,"tempDBReference:"+mTempDBReference.child("tempdb").getKey());
        Log.d(LOG_TAG,"StartBLEScan");
        startBLEScan();
       // Toast.makeText(context,"Hello this is OLD!",Toast.LENGTH_LONG).show();

    }


    public void startBLEScan(){


        // check if Bluetooth is enabled: if not ask to enable
        mBluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapater = mBluetoothManager.getAdapter();


        mBLEScanner = mBluetoothAdapater.getBluetoothLeScanner();
        settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();
        filters = new ArrayList<ScanFilter>();
        scanLeDevice(true);
    }

    private void scanLeDevice(boolean enable) {
        if (enable) {
            // we need to start the scanning
            mScanning = true;
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {

                    mBLEScanner.stopScan(mScanCallback);
                    mScanning = false;
                    Log.d(LOG_TAG, "Runnable is in execution, Scan is terminated...Uploading Results");
                    uploadListTemperatures();
                //  for the moment we don't upload
                    //    uploadListTemperatures();
                }
            }, SCAN_PERIOD);
            Log.d(LOG_TAG, "Start Scanning for BLE devices");
            listTemperatures.clear();
            mBLEScanner.startScan(filters, settings, mScanCallback);
        } else { // enable is false
            mBLEScanner.stopScan(mScanCallback);
            mScanning = false;
        }
    }

    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            TemperatureReading mTR ;
            super.onScanResult(callbackType, result);
            //Log.d(LOG_TAG, "Found Device onScanResult is called");
            //Log.d("BLE CallBack","Type:"+callbackType + " Result:"+result.toString());
            byte[] scanrec_bytes= result.getScanRecord().getBytes();
            String scanRecord = byteArrayToHexString(result.getScanRecord().getBytes());
            // Log.d("BLE CallBack","Scan Record:"+scanRecord);
            String tempSensorString = scanRecord.substring(60,70);
            int temperature_device=0;
            int battery_level=0;

            if (tempSensorString.equals((String) TEMP_SENSOR_TAG)) {

                //Log.d("BLE Scan CallBack", "Yes it's a chinese temp sensor!!!");
                String accelerometer = scanRecord.substring(71,77);
                //byte[] tempbyte= {scanrec_bytes[156],scanrec_bytes[157],scanrec_bytes[158],scanrec_bytes[159],scanrec_bytes[160],scanrec_bytes[161],scanrec_bytes[162],scanrec_bytes[163]};
                //Log.d("ScanCallBack","Temp byte array:"+tempbyte.toString());
                String temperature = scanRecord.substring(76,80);
                String temperature_hex="0x" + temperature;
                String battery= scanRecord.substring(80,82);
                deviceTemperature=Integer.decode(temperature_hex);
                deviceAddress=result.getDevice().getAddress();
                battery_level = Integer.parseInt(battery,16);


                //Log.d("ScanCallBack","SensorData from Device:"+deviceAddress + " temperature:"+temperature + " temperature_hex:"+temperature_hex + " which is:"+ deviceTemperature+"degrees C Battery:"+battery);
                //Log.d(LOG_TAG,mTR.getReadable());
                // mScannerAdapter.add().addDevice(result.getDevice(),result.getRssi(),scanrec_bytes,temperature_device);
                // mBleScannerAdapter.notifyDataSetChanged();
                //check if the address has already been read
                // if (!addressAlreadyFound(deviceAddress,listTemperatures)){
                String room="MyRoom";
                mTR= new TemperatureReading(deviceTemperature,deviceAddress,System.currentTimeMillis(),room);
                //  if (!listTemperatures.contains(mTR)){
                if (!addressAlreadyFound(deviceAddress)){
                    //mTR= new TemperatureReading(deviceTemperature,deviceAddress,System.currentTimeMillis(),getRoomFromAddress(deviceAddress));
                    //mTR= new TemperatureReading(deviceTemperature,deviceAddress,System.currentTimeMillis(),"Room");
                    room=getRoomFromAddress(deviceAddress);
                    if (room!=null){
                        mTR.setRoom(room);
                    }
                    listTemperatures.add(mTR);
                    // mScannerAdapter.add(mTR);
                   // mScannerAdapter.notifyDataSetChanged();

                }
                else {
                    Log.d(LOG_TAG,"Already present in db");
                }



           /*     updateButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(getApplicationContext(), "Update Button Pressed", Toast.LENGTH_LONG).show();


                        // we must start the scanning process
                    }
                });
                */
            }
        }
    };



    public static String byteArrayToHexString(byte[] array) {
        StringBuffer hexString = new StringBuffer();
        for (byte b : array) {
            int intVal = b & 0xff;
            if (intVal < 0x10)
                hexString.append("0");
            hexString.append(Integer.toHexString(intVal));
        }
        return hexString.toString();
    }

    private Boolean addressAlreadyFound(String addressDetected){
        Log.d(LOG_TAG,"ALF - Looking for Address:"+addressDetected);
        boolean addressAlreadyPresent=false;
        int i=0;
        while (i<listTemperatures.size()){
            Log.d(LOG_TAG,"listTemperatures "+i+" "+listTemperatures.get(i).getAddress());
            if (listTemperatures.get(i).getAddress().equals(addressDetected)){
                addressAlreadyPresent=true;
            }
            i++;
        }
        Log.d(LOG_TAG,"ALF Returning:"+addressAlreadyPresent);
        return addressAlreadyPresent;
    }

    private String getRoomFromAddress(String address){
        String deviceAddress=address;
        String room=null;
        SQLiteDatabase db = mTempDbHelper.getReadableDatabase();
        String[] projectionRoom={
                TemperatureContract.DeviceEntry.COLUMN_ROOM
        };
        String selection= TemperatureContract.DeviceEntry.COLUMN_ADDRESS + "= ?";
        String[] selectionArgs = { deviceAddress };

        Cursor cursorDevice= db.query(
                TemperatureContract.DeviceEntry.TABLE_NAME,
                projectionRoom,
                selection,
                selectionArgs,
                null,null,null
        );
        Log.d(LOG_TAG,"getRoomFromAddress address:"+ address +" room:" +room);
        if (cursorDevice.moveToNext()){
            room=cursorDevice.getString(cursorDevice.getColumnIndexOrThrow(TemperatureContract.DeviceEntry.COLUMN_ROOM));
        }
        Log.d(LOG_TAG,"getRoomFromAddress address:"+address +" room:" + room);
        return room;
    }

    private void uploadListTemperatures(){
        SQLiteDatabase db = mTempDbHelper.getWritableDatabase();
        int i=0;
        ContentValues values=new ContentValues();

        while (i<listTemperatures.size()){
            final TemperatureReading mTR=new TemperatureReading(listTemperatures.get(i).getTemperature(),listTemperatures.get(i).getAddress(),listTemperatures.get(i).getTimestamp(),listTemperatures.get(i).getRoom());
            Log.d(LOG_TAG,"listTemperatures "+i+" "+listTemperatures.get(i).getAddress() + " " +listTemperatures.get(i).getRoom()+
                    " T:"+ listTemperatures.get(i).getTemperature()+ " Time: "+ listTemperatures.get(i).getTimestamp());
            values.put(TemperatureContract.TemperatureEntry.COLUMN_ADDRESS,listTemperatures.get(i).getAddress());
            values.put(TemperatureContract.TemperatureEntry.COLUMN_ROOM,listTemperatures.get(i).getRoom());
            values.put(TemperatureContract.TemperatureEntry.COLUMN_TIMESTAMP,listTemperatures.get(i).getTimestamp());
            values.put(TemperatureContract.TemperatureEntry.COLUMN_TEMPERATURE,listTemperatures.get(i).getTemperature());
            long rowid=db.insert(TemperatureContract.TemperatureEntry.TABLE_NAME,null,values);
            //update the Cloud Temp DB

                Log.d(LOG_TAG,"Uploading to Server");
                mTempQuery=mTempDBReference.orderByChild("address").
                        equalTo(listTemperatures.get(i).getAddress());
                mTempQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        for (DataSnapshot itemSnapshot: dataSnapshot.getChildren()) {
                            itemSnapshot.getRef().setValue(mTR);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                    }
                });
            i++;
        }

    }







}