package com.viewsonic.vsotaupdate;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Handler;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class OtaVerify {

    private final static String TAG = "vsotaupdate/OtaVerify";
    private static final String MD5TAG = "MD5";
    private static final String mMD5SumEntry = "md5.sum";
    private static final String mUpdateEntry = "update.zip";
    private static final String UPDATEFILE = "systemupdate.zip";
    private static boolean LOCAL_CACHE = false;
    /*
    private static final String mCacheMd5SumFile = "/cache/md5sum";
    private static final String mCacheTmpOTAZipFile = "/cache/update.zip";
    */
    private static final int BATTERY_LEVEL = 50;
    private static final int BUFSIZE = 1024;
    private static final int PKGBUFSIZE = 1024 * 1024;
    private static final int MD5MASK = 0xff;
    private static final int MD5BUFSIZE = 64;

    public static final int RESULT_BATTERY = 101;
    public static final int RESULT_NO_AC = 102;
    public static final int RESULT_MD5_ERROR = 103;
    public static final int RESULT_MD5_OK = 104;
    public static final int RESULT_UNZIP_ERROR = 105;
    public static final int RESULT_FILE_NOT_FOUND = 106;
    public static final int RESULT_CONFIRM_UPDATE = 107;
    public static final int RESULT_CANCEL_UPDATE = 108;

    private static final IntentFilter mIntentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
    private Context mContext;
    private Handler mHandler;

    public OtaVerify(Context context, Handler handler) {
        mContext = context;
        mHandler = handler;
    }

    public boolean checkBatteryLevel() {

        Intent intent = mContext.registerReceiver(null, mIntentFilter);
        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);

        Log.e(TAG, "get battery level is " + level);
        if (level < BATTERY_LEVEL) {
            return false;
        }

        return true;
    }

    public boolean checkBatteryCharge() {

        Intent intent = mContext.registerReceiver(null, mIntentFilter);
        int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, 0);

        Log.d(TAG, "get battery status is " + status);
        if (status == BatteryManager.BATTERY_STATUS_CHARGING) {
            Log.d(TAG, "charging!");
            return true;
        }

        return false;
    }

    private void clearCache(boolean local) {
        File fileMD5;
        File fileOta;
        if (local) {
            fileMD5 = new File(mContext.getCacheDir(), mMD5SumEntry);
            fileOta = new File(mContext.getCacheDir(), mUpdateEntry);

        } else {
            fileMD5 = new File("/cache/" + mMD5SumEntry);
            fileOta = new File("/cache/" + mUpdateEntry);
        }
        if (fileMD5 != null && fileMD5.exists()) {
            fileMD5.delete();
        }
        if (fileOta != null && fileOta.exists()) {
            fileOta.delete();
        }
    }

    public boolean isFileExist(String filePath, boolean delet) {
        if (filePath != null) {
            Log.d(TAG, "Local file dir: " + mContext.getFilesDir());
            File file = new File(mContext.getFilesDir(), filePath);

            if (file != null && file.exists()) {
                if (delet) {
                    file.delete();
                }
                return true;
            }
        }

        return false;
    }

    public void VerifySystemUpdate_Thread() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                VerifySystemUpdate();
            }
        }).start();
    }

    public int VerifySystemUpdate() {
        if (isFileExist(UPDATEFILE, false)) {
            Log.d(TAG, "OTA file " + UPDATEFILE + " exist");
        } else {
            Log.d(TAG, "OTA file " + UPDATEFILE + " don't exist");
            mHandler.sendMessage(mHandler.obtainMessage(OtaVerify.RESULT_FILE_NOT_FOUND));
            return RESULT_FILE_NOT_FOUND;
        }
        // mark it because of baytrail m/d CRB don't have battery
        /*
        if (!checkBatteryLevel()) {
            mHandler.sendMessage(mHandler.obtainMessage(OtaVerify.RESULT_BATTERY));
            return RESULT_BATTERY;
        }
        if (!checkBatteryCharge()) {
            mHandler.sendMessage(mHandler.obtainMessage(OtaVerify.RESULT_NO_AC));
            return RESULT_NO_AC;
        }
        */
        try {
            File file = new File(mContext.getFilesDir(), UPDATEFILE);
            ZipFile zipFile = new ZipFile(file);

            if (unzipFileElement(zipFile, mMD5SumEntry, mMD5SumEntry, LOCAL_CACHE) == -1) {
                Log.e(TAG, "Unzip " + mMD5SumEntry + " Failed!");
                mHandler.sendMessage(mHandler.obtainMessage(OtaVerify.RESULT_UNZIP_ERROR));
                return RESULT_UNZIP_ERROR;
            }
            if (unzipFileElement(zipFile, mUpdateEntry, mUpdateEntry, LOCAL_CACHE) == -1) {
                Log.e(TAG, "Unzip " + mUpdateEntry + " Failed!");
                clearCache(LOCAL_CACHE);
                mHandler.sendMessage(mHandler.obtainMessage(OtaVerify.RESULT_UNZIP_ERROR));
                return RESULT_UNZIP_ERROR;
            }
        } catch (IOException e) {
            Log.e(TAG, "An exception occurs and Clean temp file");
            e.printStackTrace();
            clearCache(LOCAL_CACHE);
            mHandler.sendMessage(mHandler.obtainMessage(OtaVerify.RESULT_UNZIP_ERROR));
            return RESULT_UNZIP_ERROR;
        }
        String fileMD5 = getFileMD5(mUpdateEntry, LOCAL_CACHE);
        String comp = null;
        if (fileMD5 != null) {
            comp = fileMD5.substring(0, 31);
        }
        String md5Sum = getMD5sum(mMD5SumEntry, LOCAL_CACHE).substring(0, 31);


        if (comp != null && comp.equals(md5Sum)) {
            Log.d(TAG, mUpdateEntry + " md5 check ok.");
            mHandler.sendMessage(mHandler.obtainMessage(OtaVerify.RESULT_MD5_OK));

            return RESULT_MD5_OK;
        } else {
            mHandler.sendMessage(mHandler.obtainMessage(OtaVerify.RESULT_MD5_ERROR));
            return RESULT_MD5_ERROR;
        }
    }

    private int unzipFileElement(ZipFile zipFile, String entry, String desFile, boolean local) {

        ZipEntry zipEntry = zipFile.getEntry(entry);
        File desfile;

        if (zipEntry == null) {
            Log.e(TAG, "Can't find " + entry);
            return -1;
        }

        try {
            InputStream in = zipFile.getInputStream(zipEntry);

            if (in == null) {
                Log.e(TAG, "Can't create input stream for " + entry);
                return -1;
            }
            if (local) {
                desfile = new File(mContext.getCacheDir(), desFile);
            } else {
                desfile = new File("/cache/" + desFile);
            }
            if (desfile == null) {
                Log.e(TAG, "Can't create des file " + desFile);
                return -1;
            }

            if (desfile.exists()) {
                desfile.delete();
            }

            desfile.createNewFile();
            OutputStream out = new FileOutputStream(desfile);
            if (out == null) {
                Log.e(TAG, "Can't create output stream for " + desfile);
                return -1;

            }
            int realLength = 0;
            byte[] buffer = new byte[BUFSIZE];
            while ((realLength = in.read(buffer)) > 0) {
                out.write(buffer, 0, realLength);
            }

            out.close();
            out = null;
            in.close();
            in = null;
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }

        return 1;
    }

    // calculate md5 of file
    private String getFileMD5(String file, boolean local) {
        FileInputStream fis;
        try {
            MessageDigest md = MessageDigest.getInstance(MD5TAG);
            if (local)
                fis = new FileInputStream(new File(mContext.getCacheDir(), file));
            else
                fis = new FileInputStream("/cache/" + file);

            int length = -1;
            byte[] buffer = new byte[PKGBUFSIZE];

            if (fis == null || md == null) {
                return null;
            }

            while ((length = fis.read(buffer)) != -1) {
                md.update(buffer, 0, length);
            }

            if (fis != null) {
                fis.close();
            }

            byte[] bytes = md.digest();
            if (bytes == null) {
                return null;
            }
            StringBuffer buf = new StringBuffer();
            for (int i = 0; i < bytes.length; i++) {
                String md5s = Integer.toHexString(bytes[i] & MD5MASK);
                if (md5s == null || buf == null) {
                    return null;
                }
                if (md5s.length() == 1) {
                    buf.append("0");
                }
                buf.append(md5s);
            }

            return buf.toString();
        } catch (NoSuchAlgorithmException ex) {

            ex.printStackTrace();
            return null;
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    private String getMD5sum(String file, boolean local) {
        FileInputStream fis;
        try {
            if (local)
                fis = new FileInputStream(new File(mContext.getCacheDir(), file));
            else
                fis = new FileInputStream("/cache/" + file);
            if (fis == null) {
                return null;
            }

            byte[] buffer = new byte[MD5BUFSIZE];

            int length = fis.read(buffer);
            if (fis != null) {
                fis.close();
            }

            return new String(buffer, 0, length);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }


}