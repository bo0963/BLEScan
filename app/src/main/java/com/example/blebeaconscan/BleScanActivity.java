package com.example.blebeaconscan;

import android.Manifest;
import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.RequiresApi;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.PermissionChecker;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.blebeaconscan.SQL.BleBeaconDbHelper;
import com.example.blebeaconscan.SQL.SqlContrack;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static android.R.attr.targetSdkVersion;

public class BleScanActivity extends AppCompatActivity implements View.OnClickListener {

    //UI
    private BluetoothAdapter mBluetoothAdapter;
    private ScanResult mScanResult;
    private boolean hasBlePermission;
    private String deviceMessage;
    private TextView mTimerTextView, mDeviceNameTextView, mExperimentTextView , mTimeTextView;
    private final String TAG = getClass().getSimpleName();
    private final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 11110;
    private boolean mScanning;
    private LeDeviceListAdapter mleDeviceListAdapter;
    private Handler mHandler;
    private static final long SCAN_PERIOD = 2000; // 掃描週期
    private ListView listView;
    private BleBeaconDbHelper mbleBeaconDbHelper;
    private static final int PERMISSION_REQUEST_CODE_WRITE_STORAGE = 2;
    private SimpleDateFormat mSimpleDateFormat = new SimpleDateFormat("yyyyMMddHHmm", Locale.getDefault());
    private SharedPreferences mSharedPreferences;
    private Button mDbSelectButton, mDbOpenButton, mStarScanButton, mDoneButton;
    private BleBroadcastReceiver mBleBroadcastReceiver = new BleBroadcastReceiver();

//    private boolean mIsSampling;
//    private enum ScanningState {
//        STATE_IDLE,
//        STATE_STARTED,
//        STATE__STOP
//    }
//
//    private ScanningState mScanningState;

    //@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blescan);
        setTitle(R.string.title_devices);
        mHandler = new Handler();
        listView = findViewById(R.id.devicesListView);
        mSharedPreferences = getSharedPreferences(Constants.SHARED_PREFERENCES_KEY_DEVICE_NAME, Constants.SHARED_PREFERENCES_MODE);
        initializeUI();

//        mScanningState = ScanningState.STATE_IDLE;
//        refreshUI(ScanningState.STATE_IDLE);

//        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to 初始化Bluetooth adapter
//        // BluetoothAdapter through BluetoothManager.
//        final BluetoothManager bluetoothManager =
//                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
//        mBluetoothAdapter = bluetoothManager.getAdapter();
//
//        // Checks if Bluetooth is supported on the device. 檢查裝置是否支援藍芽
//        if (mBluetoothAdapter == null) {
//            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
//            finish();
//            return;
//        }
//        final BluetoothManager bluetoothManager1 =
//                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
//        mBluetoothAdapter = bluetoothManager.getAdapter();
//
//        if (mBluetoothAdapter == null) {
//            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
//            finish();
//            return;
//        }
    }

    private void initializeUI() {

        mTimerTextView = findViewById(R.id.timerTextView);
        mDeviceNameTextView = findViewById(R.id.deviceNameTextView);
        mExperimentTextView = findViewById(R.id.experimentTextView);
        mTimeTextView = findViewById(R.id.timeTextView);
        mDbSelectButton = findViewById(R.id.dbSelectButton);
        mDbOpenButton = findViewById(R.id.dbOpenButton);
        mStarScanButton = findViewById(R.id.startScanButton);
        mDoneButton = findViewById(R.id.doneButton);

        class TimerThread extends Thread {
            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
            @Override
            public void run() {
                final long initialTimeMillis = System.currentTimeMillis();
                {
                    while (!isDestroyed()) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                long currentTimeMillis = System.currentTimeMillis() - initialTimeMillis;
                                mTimerTextView.setText(String.format(Locale.getDefault(), "%02d:%02d:%02d"
                                        , currentTimeMillis / 3600000, (currentTimeMillis % 3600000) / 60000
                                        , (currentTimeMillis % 3600000 % 60000) / 1000));
                            }
                        });
                        try {
                            sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
        new TimerThread().start();

        mExperimentTextView.setText(Constants.EXPERIMENT);
        mDeviceNameTextView.setText(mSharedPreferences.getString(Constants.SHARED_PREFERENCES_KEY_DEVICE_NAME, "裝置名稱"));

        mDbSelectButton.setOnClickListener(this);
        mDbOpenButton.setOnClickListener(this);
        mStarScanButton.setOnClickListener(this);
        mDoneButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.dbSelectButton:
                onDbSelectionButtonClick();
                break;
            case R.id.dbOpenButton:
                onDbOpButtonClick();
                break;
            case R.id.startScanButton:
                onStartScanButtonClick();
                break;
            case R.id.doneButton:
                onDoneButtonClick();
                break;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    protected void onStart() {
        super.onStart();
        //初始化BluetoothAdapter
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        //確認裝置是否支援藍芽
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "裝置不支援藍芽", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        //確認藍芽權限
        checkBlePermission();
    }

    //ActionBar上的Scan and Stop 控制APP掃描裝置
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.actionbar_scan_stop, menu);
        if (!mScanning) {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);
            menu.findItem(R.id.menu_refresh).setActionView(null);
        } else {
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(R.layout.activity_blescan_actionbar);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan:
                mleDeviceListAdapter.clear();
                //創建資料庫
//                mbleBeaconDbHelper = new BleBeaconDbHelper(BleScanActivity.this, mExperimerntTextView.getText().toString()+"-"+mDeviceNameTextView.getText().toString());
                break;
            case R.id.menu_stop:
                break;
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        //初始化list view adapter
        mleDeviceListAdapter = new LeDeviceListAdapter();
        listView.setAdapter(mleDeviceListAdapter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        scanLeDevice(false);
        mleDeviceListAdapter.clear();

    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mbleBeaconDbHelper != null) {
            mbleBeaconDbHelper.close();
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void scanLeDevice(final boolean enable) {
        if (enable) {
            //在達到預定的掃描週期後停止Scan
            mHandler.postDelayed(new Runnable() {
                @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    invalidateOptionsMenu();
                }
            }, SCAN_PERIOD);
            mScanning = true; //若mScanning等於true , 啟動startLeScan , 並
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
        invalidateOptionsMenu();
    }


    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> mLeDevices;
        private LayoutInflater mInflater; //加載Layout

        // 加入rssi檢視功能 https://blog.csdn.net/love_xsq/article/details/77677034
        private ArrayList<Integer> mRSSI;    // RSSI(Received Signal Strength Indicator)已知訊號強度
        private ArrayList<byte[]> mRecord;

        //利用ListAdapter控制掃到的device生成count,item,itemID...等
        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<BluetoothDevice>();
            mInflater = BleScanActivity.this.getLayoutInflater();
            mRSSI = new ArrayList<Integer>();
            mRecord = new ArrayList<byte[]>();
        }

        public void addDevices(BluetoothDevice device, int rssi, byte[] scanrecord) {
            if (!mLeDevices.contains(device)) {
                mLeDevices.add(device);
                mRSSI.add(rssi);
                mRecord.add(scanrecord);
            }
        }

        public BluetoothDevice getDevice(int position) {
            return mLeDevices.get(position);
        }

        public void clear() {
            mLeDevices.clear();
        }

        //取得Item的數量 通常用於取得資料集合的大小或數量
        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        //回傳Item的資料
        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        //回傳Item的ID
        @Override
        public long getItemId(int i) {
            return i;
        }

        //回傳處理後的ListItem畫面\ 必須謹慎處理否則容易發生錯誤
        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            Log.d(TAG, "get View");
            if (view == null) {
                view = mInflater.inflate(R.layout.activity_blescan_listview_item, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_Adress);
                viewHolder.deviceRSSI = (TextView) view.findViewById(R.id.device_Rssi);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_Name);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }
            BluetoothDevice device = mLeDevices.get(i);
            final String deviceName = device.getName();
            int rssi = mRSSI.get(i);
            viewHolder.deviceAddress.setText(device.getAddress());
            viewHolder.deviceRSSI.setText("RSSI:" + String.valueOf(rssi));
            return view;
        }
    }

    //宣告會動到的Item object
    static class ViewHolder {
        TextView deviceAddress;
        TextView deviceRSSI;
        TextView deviceName;
    }

    //Device scan callback
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mleDeviceListAdapter.addDevices(device, rssi, scanRecord);
                    mleDeviceListAdapter.notifyDataSetChanged(); //若device的數據有更改 則刷新View
                }
            });
        }
    };

    //--------------------------------------------------------------------------Database Method-------------------------------------------------------------------------------------
    private void onDbSelectionButtonClick() {
        //過濾dbNames
        final List<String> dbNameList = new ArrayList<>();
        for (String dbName : databaseList()) {
            String[] tokens = dbName.split("-");
            if (!dbName.contains("-journal")) {
                dbNameList.add(dbName);
            }
        }
        dbNameList.add("新實驗");
        //show出dbNames
        new AlertDialog.Builder(this).setTitle("選擇資料庫").setItems(dbNameList.toArray(new String[0]), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == dbNameList.size() - 1) {
                    //新實驗
                    //輸入裝置名稱
                    final EditText editText = new EditText(BleScanActivity.this);
                    editText.setText(mSharedPreferences.getString(Constants.SHARED_PREFERENCES_KEY_DEVICE_NAME, ""));

                    new AlertDialog.Builder(BleScanActivity.this).setTitle("裝置名稱").setView(editText).setNegativeButton("確認", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (editText.getText().toString().isEmpty()) {
                                snackBar("裝置名稱不能空白");
                                return;
                            }
//                            mTimeTextView.setText(mSimpleDateFormat.format(new Date()));  // *注意* 此為實驗時間非實驗"持續"時間 因為layout未設此object所以不使用
                            mDeviceNameTextView.setText(editText.getText().toString());
                            mTimeTextView.setText(mSimpleDateFormat.format(new Date()));
                            mSharedPreferences.edit().putString(Constants.SHARED_PREFERENCES_KEY_DEVICE_NAME, editText.getText().toString()).apply();
                        }
                    }).show();

                    //更新頁面
                    mTimeTextView.setText(mSimpleDateFormat.format(new Date()));  // *注意* 此為實驗時間非實驗"持續"時間 因為layout未設此object所以不使用
//                    mScanningState = ScanningState.STATE_IDLE;
//                    refreshUI(mScanningState);
                } else {
                    //撈出舊資料 原本是要撈出 另外撰寫的Preferences的Table 因為沒有撰寫所以先使用原始資料庫 *注意*不確定能成功撈取
                    mbleBeaconDbHelper = new BleBeaconDbHelper(BleScanActivity.this, dbNameList.get(which));
                    Cursor cursor = mbleBeaconDbHelper.getReadableDatabase().query(SqlContrack.BleBeaconEntry.TABLE_NAME_BLE, null, null, null, null, null, null);
                    if (cursor.getCount() <= 0) {
                        return;
                    }
                    cursor.moveToLast();
                    String deviceName = cursor.getString(cursor.getColumnIndex(SqlContrack.BleBeaconEntry.COLUMN_NAME_DEVICE_NAME));
                    String experimentTime = cursor.getString(cursor.getColumnIndex(SqlContrack.BleBeaconEntry.COLUMN_NAME_TIMESTAMP));
                    cursor.close();

                    //資料庫名稱
                    mTimeTextView.setText(experimentTime);  // *注意* 此為實驗時間非實驗"持續"時間 因為layout未設此object所以不使用
                    mDeviceNameTextView.setText(deviceName);
                }
            }
        }).show();
    }

    private void onDbOpButtonClick() {
        String[] dbNameArray = databaseList();
        if (mbleBeaconDbHelper == null){
            snackBar("mBleDbHelper == null");
            return;
        }

        String dbName = mbleBeaconDbHelper.getDatabaseName();
        boolean isDbExist = false;
        for (String existDbName : dbNameArray) {
            if (existDbName.equals(dbName)) {
                isDbExist = true;
                break;
            }
        }

        if (isDbExist) {
            class MyOnClickListener implements DialogInterface.OnClickListener {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                        case 0:
                            Intent intent = new Intent(BleScanActivity.this, DatabaseViewerActivity.class);
                            intent.putExtra(DatabaseViewerActivity.EXTRA_DB_NAME, mbleBeaconDbHelper.getDatabaseName());
                            startActivity(intent);
                            break;
                        case 1:
                            //匯出
                            if (ActivityCompat.checkSelfPermission(BleScanActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                                ActivityCompat.requestPermissions(BleScanActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE_WRITE_STORAGE);
                                break;
                            }

                            try {
                                long targetSize = FileManager.exportDb(getDatabasePath(mbleBeaconDbHelper.getDatabaseName()));
                                snackBar("檔案已匯出 共" + targetSize + "bytes");
                            } catch (IOException e) {
                                e.printStackTrace();
                                snackBar(e.getMessage());
                            }
                            break;
                        case 2:
                            //刪除資料庫
                            showDBDeletionAlert();
                            break;
                    }
                }
            }
            DialogInterface.OnClickListener onClickListener = new MyOnClickListener();
            new AlertDialog.Builder(this)
                    .setTitle(dbName)
                    .setItems(new String[]{"查看", "匯出", "刪除"}, onClickListener)
                    .setNegativeButton("返回", null).show();
        } else {
            snackBar("資料庫不存在");
        }

    }

    private void onStartScanButtonClick() {
        scanBleOnce();

        //建立資料庫
        mbleBeaconDbHelper = new BleBeaconDbHelper(BleScanActivity.this, mExperimentTextView.getText().toString()
                + "-" + mTimeTextView.getText().toString()
                + "-" + mDeviceNameTextView.getText().toString());

//        ContentValues contentValues = new ContentValues();
//        contentValues.put(SqlContrack.BleBeaconEntry.COLUMN_NAME_DEVICE_NAME, mleDeviceListAdapter.mLeDevices.toString());
//        contentValues.put(SqlContrack.BleBeaconEntry.COLUMN_NAME_ADDRESS,);


    }

    private void onDoneButtonClick(){
        scanLeDevice(false);
    }

    /**
     * 顯示刪除資料庫的警告，並再按下按鈕時刪除
     */
    private void showDBDeletionAlert() {
        class MyOnClickerListener implements DialogInterface.OnClickListener {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        //刪除資料庫
                        deleteDatabase(mbleBeaconDbHelper.getDatabaseName());
                        mbleBeaconDbHelper = null;

                        mTimeTextView.setText(mSimpleDateFormat.format(new Date()));
                        break;
                }
            }
        }

        new AlertDialog.Builder(this).setTitle("刪除資料庫\"" + mbleBeaconDbHelper.getDatabaseName() + "\"")
                .setMessage("刪除之後便無法復原")
                .setPositiveButton("刪除", new MyOnClickerListener())
                .setNegativeButton("返回", null).show();
    }


    //-----------------------------------------------------------------Check Permission------------------------------------------------------------------------------------------------------
    public void checkBlePermission() {
        if (!selfPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION)) {
            Log.d(TAG, "已經有Permission，請檢查位置選項是否有開啟");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
            Log.d(TAG, "檢查執行順序");
        } else {
            //已有permission，檢查位置選項是否有打開
            Log.d(TAG, "位置選項已開啟，檢查手機有無支援BLE");
            if (areLocationServiceEnabled()) {
                // 位置選項已經打開了，檢查手機有沒有支援BLE
                Log.d(TAG, "位置選項已開啟，檢查手機有無支援BLE");
                if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
                    Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
                    hasBlePermission = false;
                }
                hasBlePermission = true;

            } else {
                // 位置選項沒打開
                Log.d(TAG, "位置選項未開啟");
                deviceMessage = "請打開位置設定以使用BLE";
                Toast.makeText(this, deviceMessage, Toast.LENGTH_LONG).show();
                hasBlePermission = false;
            }
        }
    }

    public boolean selfPermissionGranted(String permission) {
        boolean result = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (targetSdkVersion >= Build.VERSION_CODES.M) {
                result = this.checkSelfPermission(permission)
                        == PackageManager.PERMISSION_GRANTED;
            } else {
                result = PermissionChecker.checkSelfPermission(this, permission)
                        == PermissionChecker.PERMISSION_GRANTED;
            }
        }
        return result;
    }

    public boolean areLocationServiceEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Log.d(TAG, "areLocationServicesEnabled()");

        try {
            return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private class BleBroadcastReceiver extends BroadcastReceiver {
        private boolean isRegistered = false;
        private IntentFilter mIntentFilter = new IntentFilter(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);

        @Override
        public void onReceive(Context context, Intent intent) {
//            mIsSampling =false;
//            refreshUI(mScanningState);
//
//            if (mScanningState == ScanningState.STATE_STARTED) {
//                scanBleOnce();
//            } else {
//                if (isRegistered) {
//                    unRegister();
//                }
//            }
        }



        public void register() {
            registerReceiver(this, mIntentFilter);
            isRegistered = true;
        }

        public void unRegister() {
            unregisterReceiver(this);
            isRegistered = false;
        }
    }


    private void scanBleOnce() {
        if (!mBleBroadcastReceiver.isRegistered) {
            mBleBroadcastReceiver.register();
        }
        scanLeDevice(true);
        snackBar("掃描已經開始，正在等待WIFI資料回傳");
    }


//    private void refreshUI(ScanningState scanningState) {
//        switch (scanningState) {
//            case STATE_IDLE:
//                mStarScanButton.setEnabled(true);
//                mStopScanButton.setEnabled(false);
//                mDbOpenButton.setEnabled(false);
//                mDbSelectButton.setEnabled(false);
//            case STATE_STARTED:
//                mStopScanButton.setEnabled(true);
//                mStarScanButton.setEnabled(false);
//                mDbSelectButton.setEnabled(false);
//                mDbOpenButton.setEnabled(false);
//                break;
//            case STATE__STOP:
//                if (mIsSampling){
//                    mDbSelectButton.setEnabled(false);
//                    mDbOpenButton.setEnabled(false);
//                    mStarScanButton.setEnabled(false);
//                    mStopScanButton.setEnabled(false);
//                }else {
//                    mStopScanButton.setEnabled(false);
//                    mStarScanButton.setEnabled(false);
//                    mDbSelectButton.setEnabled(true);
//                    mDbOpenButton.setEnabled(true);
//                }
//                break;
//        }
//    }

    //    -----------------------------------------------------------Snackbar method--------------------------------------------------------------------
    private void snackBar(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Snackbar.make(findViewById(R.id.activity_bleScan), message, Snackbar.LENGTH_SHORT).show();
            }
        });
    }

}
