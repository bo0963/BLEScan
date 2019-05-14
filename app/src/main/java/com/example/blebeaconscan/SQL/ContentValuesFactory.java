package com.example.blebeaconscan.SQL;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanResult;
import android.content.ContentValues;
import android.os.Build;
import android.support.annotation.RequiresApi;

public class ContentValuesFactory {

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static ContentValues creatBleContentValues(BluetoothDevice device, ScanResult scanResult){
        ContentValues  contentValues = new ContentValues();
        contentValues.put(SqlContrack.BleBeaconEntry.COLUMN_NAME_DEVICE_NAME,device.getName() );
        contentValues.put(SqlContrack.BleBeaconEntry.COLUMN_NAME_ADDRESS,device.getAddress() );
        contentValues.put(SqlContrack.BleBeaconEntry.COLUMN_NAME_DEVISE_RSSI,scanResult.getRssi() );
        contentValues.put(SqlContrack.BleBeaconEntry.COLUMN_NAME_TIMESTAMP,scanResult.getTimestampNanos() );

        return contentValues;
    }

}
