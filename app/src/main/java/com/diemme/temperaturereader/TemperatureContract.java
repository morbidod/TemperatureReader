package com.diemme.temperaturereader;

import android.provider.BaseColumns;

/**
 * Created by XP011224 on 30/10/2017.
 */

public class TemperatureContract {


    public static class TemperatureEntry implements BaseColumns{
        public static final String TABLE_NAME="temperature";
        public static final String COLUMN_ADDRESS="address";
        public static final String COLUMN_TEMPERATURE="temperature";
        public static final String COLUMN_TIMESTAMP="timestamp";
        public static final String COLUMN_ROOM="room";
    }
    public static class DeviceEntry implements BaseColumns {
        public static final String TABLE_NAME="devices";
        public static final String COLUMN_ADDRESS="address";
        public static final String COLUMN_ROOM="room";
        public static final String COLUMN_NAME="name";
    }
}

