package com.example.blebeaconscan;

import android.database.Cursor;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.example.blebeaconscan.SQL.BleBeaconDbHelper;

import java.nio.channels.Selector;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;

public class DatabaseViewerActivity extends AppCompatActivity {
    public static final String EXTRA_DB_NAME = "extra: database name";

    private SQLiteOpenHelper mSQLiteOpenHelper;

    private RecyclerView mRecyclerView;
    private List<Data> mDataSet = new ArrayList<>();
    private int mBackState = 1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_database_viewer);
        String dbName = getIntent().getStringExtra(EXTRA_DB_NAME);
        if (dbName == null){
            finish();
        }

        setTitle("資料庫" + dbName);

        //SQLOpenHelper
        mSQLiteOpenHelper = new BleBeaconDbHelper(this, dbName);

        //RecycleView
        mRecyclerView = findViewById(R.id.recyclerView);
        GridLayoutManager layoutManager = new GridLayoutManager(this, 1);
        class MySpanSizeLookUp extends GridLayoutManager.SpanSizeLookup{
            private GridLayoutManager mGridLayoutManager;

            private  MySpanSizeLookUp(GridLayoutManager gridLayoutManager){
                this.mGridLayoutManager = gridLayoutManager;
            }

            @Override
            public int getSpanSize(int position) {
             int spanSize = 1;
             switch (mDataSet.get(position).mType){
                 case Data.TYPE_TABLE_NAME:
                     spanSize = mGridLayoutManager.getSpanCount();
                     break;
                 case Data.TYPE_DATA:
                     spanSize = 1;
                     break;
             }
                return spanSize;
            }
        }

        layoutManager.setSpanSizeLookup(new MySpanSizeLookUp(layoutManager));
        mRecyclerView.setLayoutManager(layoutManager);
        MyRecyclerAdapter myRecyclerAdapter = new MyRecyclerAdapter(mDataSet);
        mRecyclerView.setAdapter(myRecyclerAdapter);
        mRecyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
    }

    @Override
    protected void onStart() {
        super.onStart();

        //更新RecyclerView(table list)
        Cursor c = mSQLiteOpenHelper.getReadableDatabase().rawQuery("SELECT name FROM sqlite_master WHERE type='table'",null );
        if (c.moveToFirst()){
            mDataSet.clear();
            while (!c.isAfterLast()){
                mDataSet.add(new Data(Data.TYPE_DATA , c.getString(0)));
                c.moveToNext();
            }
            mRecyclerView.getAdapter().notifyDataSetChanged();
        }
        c.close();
    }

    @Override
    protected void onDestroy() {
        if (mSQLiteOpenHelper != null){
            mSQLiteOpenHelper.close();
            mSQLiteOpenHelper = null;
        }
        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && mBackState == 2){
            collapseTable();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    //摺疊表
    private void collapseTable(){
        //清除所有的data
        List<Data> tempDataList = new ArrayList<>();
        for (Data data : mDataSet){
            if (data.mType == Data.TYPE_TABLE_NAME){
                tempDataList.add(data);
            }
        }
        mDataSet.clear();
        mDataSet.addAll(tempDataList);
        mRecyclerView.getAdapter().notifyDataSetChanged();
        mBackState = 1;
    }

    private void expandClickedTable(String tableName){
        collapseTable();

        //取得Data
        Cursor cursor = mSQLiteOpenHelper.getReadableDatabase().query(tableName, null, null, null,null , null,null );
        int spanCount = cursor.getColumnCount();

        //設定GridLayoutManager的spanCount
        ((GridLayoutManager)mRecyclerView.getLayoutManager()).setSpanCount(spanCount);

        //建立表頭
        for (int i= 0 ;i < spanCount;i++){
            mDataSet.add(new Data(Data.TYPE_DATA,cursor.getColumnName(i)));
        }

        //建立data表
        if (cursor.moveToFirst()){
            while (!cursor.isAfterLast()){
                for (int i =0;i<cursor.getColumnCount();i++){
                    String data = "";
                    data = data + cursor.getColumnName(i) + ":";
                    switch (cursor.getType(i)){
                        case Cursor.FIELD_TYPE_INTEGER:
                            data = String.valueOf(cursor.getLong(i));
                            break;
                        case Cursor.FIELD_TYPE_FLOAT:
                            data = String.valueOf(cursor.getDouble(i));
                            break;
                        case Cursor.FIELD_TYPE_STRING:
                            data = cursor.getString(i);
                            break;
                    }
                    mDataSet.add(new Data(Data.TYPE_DATA,data));
                }
                cursor.moveToNext();
            }
        }
        cursor.close();
        mRecyclerView.getAdapter().notifyDataSetChanged();

        mBackState = 2;
    }

    //-----------------------------------------------------------------------------------------------------------

    private class MyRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{
        private List<Data> mDataSet;
        public MyRecyclerAdapter(List<Data> dataSet) {
            this.mDataSet = dataSet;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            RecyclerView.ViewHolder viewHolder;
            View view;
            switch (viewType){
                case Data.TYPE_TABLE_NAME:
                    view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_table_name, parent, false);
                    viewHolder = new TableNameHolder(view);
                    break;
                case Data.TYPE_DATA:
                    view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_data, parent, false);
                    viewHolder = new DataHolder(view);
                    break;
                 default:
                     throw new RuntimeException("RecyclerView找不到對應的Type");
            }
            return viewHolder;
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            Data data = mDataSet.get(holder.getAdapterPosition());

            if (holder instanceof TableNameHolder){
                final TableNameHolder tableNameHolder = (TableNameHolder) holder;
                final Data finalData = data;
                class  TableOnClickListener implements View.OnClickListener{
                    @Override
                    public void onClick(View view) {
                        expandClickedTable(finalData.mContentString);
                    }
                }
                View.OnClickListener tableOnClickListener = new TableOnClickListener();
                tableNameHolder.mTableNameButton.setText(finalData.mContentString);
                tableNameHolder.mTableNameButton.setOnClickListener(tableOnClickListener);
            }else if (holder instanceof DataHolder){
                DataHolder dataHolder = (DataHolder) holder;
                dataHolder.mTextView.setText(data.mContentString);
            }
        }

        @Override
        public int getItemCount() {
            return 0;
        }

        private class TableNameHolder extends RecyclerView.ViewHolder {
            public Button mTableNameButton;

            public TableNameHolder(View itemView) {
                super(itemView);
                mTableNameButton = itemView.findViewById(R.id.tableNameButton);
            }
        }

        private class DataHolder extends RecyclerView.ViewHolder {
            private TextView mTextView;

            private DataHolder(View itemView){
                super(itemView);
                mTextView = itemView.findViewById(R.id.dataTextView);
            }
        }
    }

    //------------------------------------------------------------------------------------------------

    private static class Data{
        private static final int TYPE_TABLE_NAME = 1;
        private static final int TYPE_DATA =2;

        private int mType;
        private String mContentString = "";

        private Data(int type,String content){
            if (type != TYPE_TABLE_NAME && type != TYPE_DATA){
                throw new RuntimeException("type 錯誤");
            }
            this.mType = type;
            this.mContentString = content;
        }

    }

}
