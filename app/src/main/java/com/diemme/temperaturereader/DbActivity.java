package com.diemme.temperaturereader;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;


public class DbActivity extends AppCompatActivity {
    private List<TemperatureReading> listTemperatures;
    private ScannerAdapter mScannerAdapter;

    private static final String LOG_TAG= "DB Activity - Debug";
    private static final int BTDEVICE=1001;
    private static final int TEMPERATURE=1002;
    private EditText addressEditText;
    private EditText roomEditText;
    private TempReaderDbHelper mTempDbHelper = new TempReaderDbHelper(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_db);
        addressEditText=(EditText) findViewById(R.id.text1) ;
        roomEditText= (EditText) findViewById(R.id.text2) ;

        Button buttonRead= (Button) findViewById(R.id.buttonRead);
        Button buttonTemp= (Button) findViewById(R.id.buttonTemp);
        Button buttonWrite = (Button) findViewById(R.id.buttonWrite);
        Button buttonScan = (Button) findViewById(R.id.buttonScan);

        buttonRead.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                read_db_room(v);
            }
        });
        buttonTemp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                read_db_temp(v);
            }
        });
        buttonWrite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                write_db(v);
            }
        });

        listTemperatures=new ArrayList<>();
        //mBleScannerAdapter=new BleScannerAdapter();
        mScannerAdapter=new ScannerAdapter(this,R.layout.item_temperature,listTemperatures);
        ListView mListView = (ListView) findViewById(R.id.listViewTemperature);
        mListView.setAdapter(mScannerAdapter);
    }

    private void read_db_room(View v){
        SQLiteDatabase db = mTempDbHelper.getReadableDatabase();
        String[] projectionDevice={
                TemperatureContract.DeviceEntry._ID,
                TemperatureContract.DeviceEntry.COLUMN_NAME,
                TemperatureContract.DeviceEntry.COLUMN_ADDRESS,
                TemperatureContract.DeviceEntry.COLUMN_ROOM
        };

        Cursor cursorDevice= db.query(
                TemperatureContract.DeviceEntry.TABLE_NAME,
                projectionDevice,
                null,null,null,null,null
        );

        Log.d(LOG_TAG,"Device DB");
        cursorDumper(cursorDevice, BTDEVICE);

        String[] projectionTemperature={
                TemperatureContract.TemperatureEntry._ID,
                TemperatureContract.TemperatureEntry.COLUMN_ROOM,
                TemperatureContract.TemperatureEntry.COLUMN_TIMESTAMP,
                TemperatureContract.TemperatureEntry.COLUMN_TEMPERATURE
        };
        Log.d(LOG_TAG,"Temperature DB");
        Cursor cursorTemperature=db.query(
                TemperatureContract.TemperatureEntry.TABLE_NAME,
                projectionTemperature,
                null,null,null,null,null
        );

        cursorDumper(cursorTemperature, TEMPERATURE);


    }

    private void read_db_temp(View v) {
        SQLiteDatabase db = mTempDbHelper.getReadableDatabase();
        String[] projectionTemp = {
                TemperatureContract.TemperatureEntry._ID,
                TemperatureContract.TemperatureEntry.COLUMN_ROOM,
                TemperatureContract.TemperatureEntry.COLUMN_TEMPERATURE,
                TemperatureContract.TemperatureEntry.COLUMN_TIMESTAMP
        };

        Cursor cursorDevice = db.query(
                TemperatureContract.TemperatureEntry.TABLE_NAME,
                projectionTemp,
                null, null, null, null, null
        );
    }

    private boolean write_db(View v){
        SQLiteDatabase db = mTempDbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(TemperatureContract.DeviceEntry.COLUMN_NAME,"Device x");
        cv.put(TemperatureContract.DeviceEntry.COLUMN_ADDRESS,addressEditText.getText().toString());
        cv.put(TemperatureContract.DeviceEntry.COLUMN_ROOM,roomEditText.getText().toString());
        long row_id=db.insert(TemperatureContract.DeviceEntry.TABLE_NAME,null,cv);
        if (row_id>0){
            Log.d(LOG_TAG,"Values Inserted in DB");
            db.close();
            return true;
        }
        else{
            Log.d(LOG_TAG,"Fail to insert in DB");
            db.close();
            return false;
        }
    }


    private void cursorDumper(Cursor c, int type_of_cursor){

        if (c.moveToFirst()){
            while (!c.isAfterLast()){
                if (type_of_cursor==BTDEVICE){
                    Log.d(LOG_TAG,"cursor "+c.getString(c.getColumnIndexOrThrow(TemperatureContract.DeviceEntry.COLUMN_NAME)) + " " +
                            c.getString(c.getColumnIndexOrThrow(TemperatureContract.DeviceEntry.COLUMN_ADDRESS)) + " " +
                            c.getString(c.getColumnIndexOrThrow(TemperatureContract.DeviceEntry.COLUMN_ROOM)));
                }
                else if (type_of_cursor==TEMPERATURE){
                    Log.d(LOG_TAG,"cursor "+c.getString(c.getColumnIndexOrThrow(TemperatureContract.TemperatureEntry.COLUMN_ROOM)) + " " +
                            c.getLong(c.getColumnIndexOrThrow(TemperatureContract.TemperatureEntry.COLUMN_TIMESTAMP)) + " " +
                            c.getInt(c.getColumnIndexOrThrow(TemperatureContract.TemperatureEntry.COLUMN_TEMPERATURE)));
                }

                c.moveToNext();
            }
        }
    }
}
