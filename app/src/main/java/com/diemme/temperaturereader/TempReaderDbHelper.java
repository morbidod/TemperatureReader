package com.diemme.temperaturereader;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created by XP011224 on 30/10/2017.
 */

public class TempReaderDbHelper extends SQLiteOpenHelper {
    private final String LOG_TAG="DBHelper DEBUG";
    public static final int DATABASE_VERSION = 2;
    public static final String DATABASE_NAME = "Temp.db";
    public static final String CREATE_TABLE_TEMPERATURE="CREATE TABLE "+TemperatureContract.TemperatureEntry.TABLE_NAME + "(" +
            TemperatureContract.TemperatureEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
            TemperatureContract.TemperatureEntry.COLUMN_ADDRESS + " TEXT NOT NULL ," +
            TemperatureContract.TemperatureEntry.COLUMN_ROOM + " TEXT NOT NULL," +
            TemperatureContract.TemperatureEntry.COLUMN_TEMPERATURE + " INTEGER NOT NULL," +
            TemperatureContract.TemperatureEntry.COLUMN_TIMESTAMP + " INTEGER NOT NULL" + ")";

    public static final String CREATE_TABLE_DEVICE="CREATE TABLE "+ TemperatureContract.DeviceEntry.TABLE_NAME + "(" +
            TemperatureContract.DeviceEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
            TemperatureContract.DeviceEntry.COLUMN_ADDRESS + " TEXT NOT NULL ," +
            TemperatureContract.DeviceEntry.COLUMN_NAME + " TEXT NOT NULL ," +
            TemperatureContract.DeviceEntry.COLUMN_ROOM + " TEXT NOT NULL" + ")";



    public TempReaderDbHelper(Context context){
        super(context,DATABASE_NAME,null,DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(LOG_TAG,CREATE_TABLE_DEVICE);
        db.execSQL(CREATE_TABLE_DEVICE);
        db.execSQL(CREATE_TABLE_TEMPERATURE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TemperatureContract.TemperatureEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + TemperatureContract.DeviceEntry.TABLE_NAME);
        onCreate(db);
    }
}
