package com.example.blebeaconscan.SQL;

import android.provider.BaseColumns;

public class SqlContrack {

    //便免有人實體化這個Contract，將建構子設成private
    private SqlContrack(){
    }

    public static class BleBeaconEntry implements BaseColumns {
        public static final String TABLE_NAME_BLE ="blescan";
        public static final String COLUME_NAME_TRACK ="track"; //第幾次實驗
        public static final String COLUMN_NAME_DEVICE_NAME ="name"; //裝置名稱
        public static final String COLUMN_NAME_ADDRESS="address"; //BLE device address(like MAC)
        public static final String COLUMN_NAME_DEVISE_RSSI ="RSSI"; //Received Signal Strength Indicator (已知發射功率)
        public static final String COLUMN_NAME_UUID = "uuid"; //Universally Unique Identifier (唯一通用識別碼)
        public static final String COLUMN_NAME_TIMESTAMP = "timestamp"; //時間戳記
        public static final String COLUMN_NAME_IS_DONE = "is_done";
    }

//    public static class BleBeaconPreferencesEntry implements BaseColumns{
//        public static final String TABLE_NAME_BLE_PREFERENCES ="blescan_preferences";
//        public static final String COLUMN_NAME_DEVICENAME_PREFER ="name_prefer";
//        public static final String COLUMN_NAME_ADDRESS_PREFER="sddress_prefer";
//        public static final String COLUMN_NAME_DEVISE_RSSI_PREFER ="RSSI_prefer";
//        public static final String COLUMN_NAME_UUID_PREFER = "uuid_prefer";
//    }

}
