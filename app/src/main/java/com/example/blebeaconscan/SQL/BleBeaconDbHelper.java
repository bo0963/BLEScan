package com.example.blebeaconscan.SQL;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.design.widget.Snackbar;
import android.widget.Toast;


import com.example.blebeaconscan.SQL.SqlContrack.BleBeaconEntry;

import java.util.concurrent.BlockingDeque;

import static com.example.blebeaconscan.SQL.SqlContrack.*;

public class BleBeaconDbHelper extends SQLiteOpenHelper {
    private static final String INTEGER_TYPE = " INTEGER";
    private static final String TEXT_TYPE = " TEXT";
    private static final String COMMA_SEP = " ,";

    //建立表格時，定義表格外觀字串
    private static final String SQL_CREATE_ENTRIES_BLESCAN =
            " CREATE TABLE " + SqlContrack.BleBeaconEntry.TABLE_NAME_BLE +
                    " (" +
                    BleBeaconEntry._ID+" INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL"+
                    BleBeaconEntry.COLUME_NAME_TRACK+INTEGER_TYPE+COMMA_SEP+
                    BleBeaconEntry.COLUMN_NAME_DEVICE_NAME+TEXT_TYPE+COMMA_SEP+
                    BleBeaconEntry.COLUMN_NAME_ADDRESS+TEXT_TYPE+COMMA_SEP+
                    BleBeaconEntry.COLUMN_NAME_DEVISE_RSSI+INTEGER_TYPE+COMMA_SEP+
                    BleBeaconEntry.COLUMN_NAME_UUID+INTEGER_TYPE+COMMA_SEP+
                    BleBeaconEntry.COLUMN_NAME_TIMESTAMP+INTEGER_TYPE+ " )";

    private static final int DATEBASE_VERSION =1; //資料庫版本

    public BleBeaconDbHelper(Context context,String databaseName){
        super(context,databaseName,null,DATEBASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        //當資料庫不存在，執行此method建立
        db.execSQL(SQL_CREATE_ENTRIES_BLESCAN);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //資料庫需要更新時，執行此method
    }

    /**
     *
     * @param bleDataBlockingQueue 存放Data的queue
     * @param sqLiteOpenHelper 存放
     * @param tableName 存放資料的table(原始)
     * @return  int[0]:放入table的數量;
     */

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public int[] transFerQueueToDatabase(@NonNull BlockingDeque<BeaconData> bleDataBlockingQueue,
                                         @NonNull SQLiteOpenHelper sqLiteOpenHelper, @NonNull String tableName){
        int[] count = new int[]{0}; //int originalCount =0
        SQLiteDatabase sqLiteDatabase = sqLiteOpenHelper.getReadableDatabase();
        sqLiteDatabase.beginTransaction();
        while (!bleDataBlockingQueue.isEmpty()){
            BeaconData beaconData = bleDataBlockingQueue.poll();
            ContentValues contentValues = ContentValuesFactory.creatBleContentValues(beaconData.getmDevice(),beaconData.getScanResult() );
            sqLiteDatabase.insert(tableName,null ,contentValues ); //將Data放入原始的(所有ScanResult)table
            count[0]++;

            //原本目的為從另一個過濾過的table取得相同的時間資料，但因為目前只有一個table所以不執行
//            Cursor cursor = sqLiteDatabase.query(tableName, null, BleBeaconEntry.COLUMN_NAME_TIMESTAMP + "=?" ,
//                    new String[]{String.valueOf(beaconData.getScanResult().getTimestampNanos())} ,null , null,null );
//
//            boolean isBeaconDataUnique = true;
//            if (cursor.getCount() >0){
//                cursor.moveToFirst();
//                while (!cursor.isAfterLast()){
//                    String NAME = cursor.getString(cursor.getColumnIndex(BleBeaconEntry.COLUMN_NAME_DEVICE_NAME));
//                    if (NAME.equals(beaconData.getScanResult().getDevice())){
//                        isBeaconDataUnique = false;
//                        break;
//                    }
//                    cursor.moveToNext();
//                }
//            }
//            cursor.close();
        }
        sqLiteDatabase.setTransactionSuccessful();
        sqLiteDatabase.endTransaction();
        return count;

//        snackBar("已經將所有資料放入database, 全部" + originCount + "筆");
//        因為design.widget.Snackbar需要build tool 28.0.0怕打亂整個架構故不使用
//        https://stackoverflow.com/questions/34263418/cant-find-android-support-design-widget-snackbar-in-support-design-library
//        詳細參見https://developer.android.com/reference/android/support/design/widget/Snackbar.html#nested-classes
    }

//    因為要使用Snackbar的話Activity必須繼承APPCompatActivity,但這個class已經繼承SQL所以不實作
//    private void snackBar(final String message){
//        Snackbar.make(findViewById(R.id.layout), message, Snackbar.LENGTH_SHORT).show();
//    }

}
