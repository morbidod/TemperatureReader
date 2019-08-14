package com.diemme.temperaturereader;

import android.Manifest;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
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
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {
    private static final long SCANTIMER = 300*1000; // 300 seconds 5 min
    public TempReaderDbHelper mTempDbHelper = new TempReaderDbHelper(this);
    private SQLiteDatabase mSqlDatabase;
    //private SQLiteDatabase db=mTempDbHelper.getWritableDatabase();
    private final String LOG_TAG="LOG DEBUG";
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
    public static List<TemperatureReading> listTemperatures;
    private boolean mScanning = false;
    private Handler mHandler = new Handler();
    private Handler timerHandler = new Handler();
    private boolean alarmStarted=false;

    //Firebase
    private String mUsername;
    public static FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mDatabaseReference;
    private DatabaseReference mUserDBReference;
    private DatabaseReference mTempDBReference;
    private Query mDatabaseQuery;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;

    private FirebaseUser mFirebaseUser=null;
    private User mUser;
    private Query mTempQuery;

    private ChildEventListener mChildEventListener;
    private ChildEventListener mTempChildEventListener;
    private ValueEventListener mValueEventListener;

    public Boolean userHasWriteAccess=false;
    private Boolean timerActivityStarted=false;

   private AlarmReceiver mAlarmReceiver;
    private PendingIntent pendingIntent;

    private Button buttonTimer;
    private String deviceAddress;
    private Integer deviceTemperature;
    private Integer numAlarms=0;
    private Boolean startedTimer= Boolean.FALSE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(LOG_TAG,"OnCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkpermission();

        // Firebase initialization - get references to btdevices, users and tempdb
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mDatabaseReference = mFirebaseDatabase.getReference().child("btdevices");
        mUserDBReference=mFirebaseDatabase.getReference().child("users");
        mTempDBReference=mFirebaseDatabase.getReference().child("tempdb");

        //Firebase Auth initialization
        mFirebaseAuth=FirebaseAuth.getInstance();

        mAlarmReceiver=new AlarmReceiver();
        Intent alarmIntent = new Intent(MainActivity.this, AlarmReceiver.class);
        pendingIntent = PendingIntent.getBroadcast(MainActivity.this, 0, alarmIntent, 0);

        //Initialize the button Scan -> When clicked a single BLE scan will be performed
        Button buttonScan = (Button) findViewById(R.id.buttonScan);
        buttonScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //replaced by startBLEScan
                startBLEScan();

            }
        });
       //Button Timer: if Timer is not running start the repetive timing scanner, otherwise stop the timer
        buttonTimer = (Button) findViewById(R.id.buttonTimer);
        buttonTimer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startTimingTask();
            }
        });


       //initialize the list of temperatures
        listTemperatures=new ArrayList<>();

        //set the ScannerAdapter to diplay hte results in the listView
        mScannerAdapter=new ScannerAdapter(this,R.layout.item_temperature,listTemperatures);
        ListView mListView = (ListView) findViewById(R.id.listViewTemperature);
        mListView.setAdapter(mScannerAdapter);

        //start the listener of Auth State
        // only authenticated users with WriteAccess can perform the Reading Task
        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                final FirebaseUser user = firebaseAuth.getCurrentUser();
                Log.d(LOG_TAG,"OnAuthStateChanged");
                //mFirebaseUser = firebaseAuth.getCurrentUser();
                if (user == null){
                    //Need to Sign in
                    Log.d(LOG_TAG,"OnAuth Detected user is null");
                    onSignOutCleanUp();
                    Toast.makeText(getApplicationContext(),"Need to Sign in",Toast.LENGTH_LONG).show();
                    Intent intentSignIn= AuthUI.getInstance().createSignInIntentBuilder().
                            setProviders(AuthUI.EMAIL_PROVIDER,AuthUI.GOOGLE_PROVIDER).build();
                    Log.d(LOG_TAG,"startActivityForResult "+RC_SIGN_IN);
                    startActivityForResult(intentSignIn,RC_SIGN_IN);

                }
                if (user !=null){
                    // user is authenticated - need to iniitialize
                    Toast.makeText(getApplicationContext(),"Welcome "+user.getDisplayName(),Toast.LENGTH_SHORT).show();
                    Log.d(LOG_TAG,"User is not null!!!!");
                    userHasWriteAccess=false;

                    //get the user database with key ID to see if the user has writeAccess
                    mUserDBReference.child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            userHasWriteAccess=dataSnapshot.getValue(User.class).getIsWriter();
                            Log.d(LOG_TAG,"userDB onDataChange exit userhaswriteaccess:"+userHasWriteAccess);
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });


                    Log.d(LOG_TAG,"user:"+user.getDisplayName()+ " has WriteAccess:"+userHasWriteAccess);
                    onSignInInitialize(user.getDisplayName());


                }

            }
        };
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(LOG_TAG,"onActivityResult RequestCode:"+requestCode+" ResultCode:"+resultCode);
        if (requestCode == RC_SIGN_IN) {
            // it was a SIGN IN activity
            if (resultCode == RESULT_OK) {
                Toast.makeText(getApplicationContext(), "Sign In success", Toast.LENGTH_LONG).show();
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(getApplicationContext(), "Sign In was not successfull", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.sign_out_menu:
                AuthUI.getInstance().signOut(this);
            case R.id.database:
                Intent i = new Intent(this,DbActivity.class);
                startActivity(i);

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume(){
        super.onResume();
        mFirebaseAuth.addAuthStateListener(mAuthStateListener);
        Log.d(LOG_TAG,"OnResume");

    }

    @Override
    public void onPause(){
        super.onPause();
        mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
        detachDatabaseListener();
        mScannerAdapter.clear();
        listTemperatures.clear();
    }



   public void startBLEScan(){
       if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
           Toast.makeText(getApplicationContext(), "Sorry BLE is not supported", Toast.LENGTH_SHORT).show();
           finish();
       }
       Log.d(LOG_TAG,"Start BLEScan");
       // check if Bluetooth is enabled: if not ask to enable
       mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
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
            Log.d(LOG_TAG,"ScanLeDevice: start handler");
            mScanning = true;
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {

                    mBLEScanner.stopScan(mScanCallback);
                    mScanning = false;
                    Log.d(LOG_TAG, "Runnable is in execution, Scan is terminated...Uploading Results");
                    uploadListTemperatures();
                }
            }, SCAN_PERIOD);
            //Log.d(LOG_TAG, "Start Scanning for BLE devices");
            listTemperatures.clear();
            mBLEScanner.startScan(filters, settings, mScanCallback);
        } else { //
            // enable is false
            mBLEScanner.stopScan(mScanCallback);
            mScanning = false;
        }
    }

    //ScanCallBack receives the result of the BLE scanning and analyze the data received
    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            TemperatureReading mTR ;
            super.onScanResult(callbackType, result);
            //Log.d(LOG_TAG, "Found Device onScanResult is called");
            Log.d("BLE CallBack","Type:"+callbackType + " Result:"+result.toString());
            int rssi=result.getRssi();
            byte[] scanrec_bytes= result.getScanRecord().getBytes();
            String scanRecord = byteArrayToHexString(result.getScanRecord().getBytes());
           // Log.d("BLE CallBack","Scan Record:"+scanRecord);
            String tempSensorString = scanRecord.substring(60,70);
            int temperature_device=0;
            int battery_level=0;

            if (tempSensorString.equals((String) TEMP_SENSOR_TAG)) {

                Log.d("BLE Scan CallBack", "Yes it's a chinese temp sensor!!!");
                String accelerometer = scanRecord.substring(71,77);
                //byte[] tempbyte= {scanrec_bytes[156],scanrec_bytes[157],scanrec_bytes[158],scanrec_bytes[159],scanrec_bytes[160],scanrec_bytes[161],scanrec_bytes[162],scanrec_bytes[163]};
                //Log.d("ScanCallBack","Temp byte array:"+tempbyte.toString());
                String temperature = scanRecord.substring(76,80);
                String temperature_hex="0x" + temperature;
                String battery= scanRecord.substring(80,82);
                deviceTemperature=Integer.decode(temperature_hex);
                deviceAddress=result.getDevice().getAddress();
                battery_level = Integer.parseInt(battery,16);


                Log.d("ScanCallBack","SensorData from Device:"+deviceAddress + " temperature:"+temperature + " temperature_hex:"+temperature_hex + " which is:"+ deviceTemperature+"degrees C RSSI:"+rssi+" Battery:"+battery);
                //Log.d(LOG_TAG,mTR.getReadable());
                // mScannerAdapter.add().addDevice(result.getDevice(),result.getRssi(),scanrec_bytes,temperature_device);
               // mBleScannerAdapter.notifyDataSetChanged();
                //check if the address has already been read
               // if (!addressAlreadyFound(deviceAddress,listTemperatures)){
                String room="MyRoom";
                mTR= new TemperatureReading(deviceTemperature,deviceAddress,System.currentTimeMillis(),room,rssi);
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
                    mScannerAdapter.notifyDataSetChanged();

                }
                else {
                    Log.d(LOG_TAG,"Already present in db");
                }
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


    private void startTimingTask(){
        Log.d(LOG_TAG,"startTiming Task Timer:"+startedTimer);

     //  timerTask executed through handler
       /* final Handler mTaskHandler = new Handler();
        Runnable r = new Runnable() {
            @Override
            public void run() {
                Log.d(LOG_TAG,"Runnable execution");
                startBLEScan();
                mTaskHandler.postDelayed(this,SCANTIMER);
            }
        };
        if (!startedTimer){
            startedTimer=true;
            buttonTimer.setText("Stop Timer");
            mTaskHandler.post(r);
        }
        else {
            startedTimer=false;
            buttonTimer.setText("Start Timer");
            mTaskHandler.removeCallbacks(r);

        }
       */

     // timerTask execute through AlarmManager

        AlarmManager manager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        int interval = 10*60*1000; //10 minutes
        if(!alarmStarted){
            //start repeating Task
            alarmStarted=true;
            numAlarms++;
            Log.d("Alarm fired","alarmStarted:"+alarmStarted + " Alarm N:"+numAlarms);

            manager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime(), AlarmManager.INTERVAL_HALF_HOUR, pendingIntent);
            //manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), AlarmManager.INTERVAL_HALF_HOUR, pendingIntent);

            //Toast.makeText(this, "Alarm Set", Toast.LENGTH_SHORT).show();

            buttonTimer.setText("Stop Timer");
                 }
        else {
            alarmStarted=false;

            Log.d("Alarm cancelled","alarmStarted:"+alarmStarted);
            manager.cancel(pendingIntent);
            buttonTimer.setText("Start Timer");
        }
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
        //Log.d(LOG_TAG,"getRoomFromAddress address:"+address +" room:" + room);
        return room;
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

  private void uploadListTemperatures(){
        Log.d(LOG_TAG,"uploadListTemperatures");
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
          if (userHasWriteAccess){
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
          }


          i++;
      }
  }





    Runnable mRunnable=new Runnable() {
        @Override
        public void run() {
            Log.d(LOG_TAG,"mRunnable in ex");
            startBLEScan();
            timerHandler.postDelayed(mRunnable,60000);
        }
    };

    private void startRepeatingTask(){
        Log.d(LOG_TAG,"startRepeatingTask");
        mRunnable.run();
    }

    private void stopReatingTask(){
        Log.d(LOG_TAG,"stopRepeatingTask");
        timerHandler.removeCallbacks(mRunnable);
    }



    private void onSignInInitialize(String username){
        mUsername=username;
        loadDeviceDatabase();
        attachTempDatabaseListener();
    }

    //LoadDeviceDatabase
    // read the device database from Firebase and if not present store it locally
    private void loadDeviceDatabase() {
        final SQLiteDatabase dbread= mTempDbHelper.getReadableDatabase();
        final SQLiteDatabase db = mTempDbHelper.getWritableDatabase();
        final ContentValues values=new ContentValues();
        mDatabaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot devicesnaphot: dataSnapshot.getChildren()){
                    Device mDevice=new Device();
                    String key=devicesnaphot.getKey();
                    Log.d(LOG_TAG,"device key:"+key);

                    //mDevice.setId(devicesnaphot.child("id").getValue(int.class));
                    //mDevice.setAddress(devicesnaphot.child("address").getValue(String.class));
                    //mDevice.setName(devicesnaphot.child("name").getValue(String.class));
                    //mDevice.setRoom(devicesnaphot.child("room").getValue(String.class));
                    //mDevice.show();

                    Log.d(LOG_TAG,"Updating local db");
                    String DeviceName=devicesnaphot.child("name").getValue(String.class);
                    String[] projection={TemperatureContract.DeviceEntry.COLUMN_NAME};
                    String selection=TemperatureContract.DeviceEntry.COLUMN_NAME+"=?";
                    String[] selectionArgs={DeviceName};
                    Cursor cursor= dbread.query(TemperatureContract.DeviceEntry.TABLE_NAME,projection,selection,selectionArgs,null,null,null);
                    if (!cursor.moveToFirst()){
                        Log.d(LOG_TAG,"Device not present..will be inserted in the local DB");
                        values.put(TemperatureContract.DeviceEntry._ID,DeviceName);
                        values.put(TemperatureContract.DeviceEntry.COLUMN_ADDRESS,devicesnaphot.child("address").getValue(String.class));
                        values.put(TemperatureContract.DeviceEntry.COLUMN_NAME,devicesnaphot.child("name").getValue(String.class));
                        values.put(TemperatureContract.DeviceEntry.COLUMN_ROOM,devicesnaphot.child("room").getValue(String.class));
                        long row_id=db.insert(TemperatureContract.DeviceEntry.TABLE_NAME,null,values);
                        if (row_id>0){
                            Log.d(LOG_TAG,"Values Inserted in DB");
                        }
                        else{
                            Log.d(LOG_TAG,"Fail to insert in DB");
                        }
                    }
                    else {
                        // device name is already present
                        Log.d(LOG_TAG,"Device already present in Local DB");
                    }

                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
        //db.close();
    }

    private void onSignOutCleanUp(){
        //On Sign Out: clear the username, clear the list and detach the db listener
        //Toast.makeText(getApplicationContext(),"Good Bye",Toast.LENGTH_SHORT).show();
        Log.d(LOG_TAG,"OnSignOutCleanUp");
        mUsername = "ANONYMOUS";
        mScannerAdapter.clear();
        listTemperatures.clear();
    }

    private void attachTempDatabaseListener(){
        if (mTempChildEventListener == null){
            mTempChildEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    TemperatureReading mTR = dataSnapshot.getValue(TemperatureReading.class);
                    //add to the list of temperatures
                    // check if the timestamp
                    mScannerAdapter.add(mTR);
                    mScannerAdapter.notifyDataSetChanged();
                    // listTemperatures.add(mTR);

                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                }

                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) {
                }

                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String s) {
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                }
            };
            mTempDBReference.addChildEventListener(mTempChildEventListener);
        }

    }

    private void detachDatabaseListener(){

        if (mTempChildEventListener != null){
            mTempDBReference.removeEventListener(mTempChildEventListener);
            mTempChildEventListener=null;
        }
    }

    private void Logging (User user){
        Log.d(LOG_TAG,"User:"+user.getName()+ " "+user.getUid()+ " isWriter:"+user.getIsWriter());
    }





    private void checkpermission() {
        boolean permission_granted = false;
        // Android M Permission check 
        if (Build.VERSION.SDK_INT >= 23) {
            if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[],
                                           int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(LOG_TAG, "coarse location permission granted");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to scan ble");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }
                    });
                    builder.show();
                }
                return;
            }
        }
    }


}
