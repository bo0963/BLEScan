package com.example.blebeaconscan.SQL;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanResult;

public class BeaconData {
    private BluetoothDevice mDevice;
    private ScanResult scanResult;

    public BeaconData(BluetoothDevice mDevice,ScanResult scanResult){
        this.mDevice = mDevice;
        this.scanResult = scanResult;
    }

    public BluetoothDevice getmDevice(){
        return mDevice;
    }

    public  ScanResult getScanResult(){
        return scanResult;
    }
}
