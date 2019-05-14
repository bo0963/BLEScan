package com.example.blebeaconscan;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.channels.FileChannel;

//負責執行檔案匯出的Class
public class FileManager {
    public static long exportDb(File database) throws IOException{
        //判斷外部空間考否寫入
        String state = Environment.getExternalStorageState();
        if (!state.equals(Environment.MEDIA_MOUNTED)){
            throw new IOException("此裝置無法寫入外部儲存空間" + state);
        }

        //創建資料夾
        File targetDirectory = new File(Environment.getExternalStorageDirectory(),"BLE室內定位資料庫");
        if (!targetDirectory.isDirectory()){
            Log.i(FileManager.class.getName(), targetDirectory.mkdir() ? targetDirectory.getPath()+"資料夾建立成功": targetDirectory.getPath()+"資料夾建立失敗");
        }

        //指定檔案路徑
        File target = new File(targetDirectory,database.getName());

        //將database檔案匯出到指定資料夾
        FileChannel databaseChannel;
        FileChannel targetChannel;
        try {
            databaseChannel = new FileInputStream(database).getChannel();
            targetChannel = new FileInputStream(target).getChannel();
        }catch (FileNotFoundException e){
            e.printStackTrace();
            throw new IOException("資料通道創建失敗");
        }

        try{
            targetChannel.transferFrom(databaseChannel, 0, databaseChannel.size());
        }catch (IOException e){
            e.printStackTrace();
            throw new IOException("檔案匯出失敗");
        }

        long targetSize;
        try {
            targetSize = targetChannel.size();
            databaseChannel.close();
            targetChannel.close();
        }catch (IOException e){
            e.printStackTrace();
            throw new IOException("關閉資料通道失敗");
        }

        return targetSize;
    }
}
