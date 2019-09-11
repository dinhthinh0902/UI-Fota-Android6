package com.viewsonic.vsotaupdate;

import android.content.Context;
import android.content.SharedPreferences;
import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import android.util.Log;

public class Util {
    public static final String PREFS_STATE = "system_update_service_state";
    private static final String BUILD_PROP = "/system/build.prop";
    private static final String INCREMENTAL_VERSION = "ro.build.version.incremental";
    private static final String TAG = "vsota/Util";
    public static class Action {
        public static final String ACTION_BOOT_COMPLETE = "com.viewsonic.vsotaupdate.ACTION_BOOT_COMPLETE";
        public static final String ACTION_SHOW_OTA = "com.viewsonic.vsotaupdate.ACTION_SHOW_OTA";
        public static final String ACTION_CANCEL_DOWNLOAD = "com.viewsonic.vsotaupdate.ACTION_CANCEL_DOWNLOAD";
        public static final String ACTION_PERIODIC_CHECK = "com.viewsonic.vsotaupdate.ACTION_PERIODIC_CHECK";
        public static final String ACTION_NORMAL = "com.viewsonic.vsotaupdate.ACTION_NORMAL";
    }
    public static class VSOta {
        public static final String VS_OTA_Version = "http://test.zien.vn:8080/version.txt";
        //public static final String VS_OTA_Package = "http://ota.viewsoniceurope.com/VSD224/systemupdate_test.zip";
        public static final String VS_OTA_Package = "http://test.zien.vn:8080/systemupdate.zip";
        public static final String VS_OTA_LocalPackage = "systemupdate.zip";

    }
    static String getUpdateServiceState(Context context, String key) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_STATE, 0);
        return settings.getString(key,"init");
    }
    static void setUpdateServiceState(Context context, String key, String state) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_STATE, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(key, state);
        editor.apply();
    }
    static long CheckLocalUpdateFileSize(Context context) {
        File file = new File(context.getFilesDir(), VSOta.VS_OTA_LocalPackage);
        if (file != null) {
            return file.length();
        }
        else
            return 0;

    }
    static long GetDeviceVersion(Context context) {
        //File file = new File(BUILD_PROP);
        String line;
        String version = "";
        int i =0,j=0, idx;
        BufferedReader buf;
        boolean found = false;
        Log.d(TAG,"[GetDeviceVersion]");
        try {
            buf = new BufferedReader(new FileReader(BUILD_PROP));
            while ((line = buf.readLine()) != null) {
               if (line.contains(INCREMENTAL_VERSION)) {
                    i = line.lastIndexOf("-");
                    j = line.lastIndexOf("=");
                    idx = (i>j) ? i:j;
                    Log.d(TAG,line);
                    version = line.substring(idx+1, line.length());
                    Log.d(TAG,"Device version: " + version);
                   //dump by Min
                   version = "201406250131";
                   Log.d(TAG,"Device version: " + version);
                    found = true;
                    break;
               }

            }
            buf.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        if (found)
            if (version.length() == 8)
                return Long.parseLong(version) * 10000;
            else
                return Long.parseLong(version);
        else
            return 99999999;
    }

}
