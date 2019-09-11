package com.viewsonic.vsotaupdate;

import android.app.Service;
import android.content.Intent;
import android.app.PendingIntent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Binder;
import android.util.Log;
import android.os.Handler;
import android.os.Message;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import android.net.Uri;
import java.net.HttpURLConnection;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import android.content.Context;
import android.app.AlarmManager;
import java.util.Calendar;
import android.os.SystemClock;
import java.text.SimpleDateFormat;
import java.util.Date;
import com.viewsonic.vsotaupdate.OtaNotification;

public class SystemUpdateService extends Service {

    private static final String TAG = "vsota/UpdateService";

    static final int MSG_NETWORKERROR = 0;
    static final int MSG_NEWVERSIONDETECTED = 1;
    static final int MSG_DOWNLOADPROGRESS = 2;
    static final int MSG_DOWNLOADFAIL = 3;
    static final int MSG_DOWNLOADSUCCESS = 4;
    static final int MSG_NONEWVERSIONDETECTED = 5;
    static final int MSG_CANCELDOWNLOAD = 6;
    static final int MSG_STARTRESUMEDOWNLOAD = 7;
    static final int MSG_UNKNOWERROR = 20;
    static final int URL_CONNECT_TIMEOUT = 120000; // 120 second
    static final int URL_READ_TIMEOUT = 120000; // 120 second
    static final int RETRY_PERIOD = 2;  // 30 min
    static final int HTTP_TRANSFER_BUF = 16 * 1024;
    public DownloadFilesThread mDownloadFWThread=null;
    private Handler mHandler;
    public String mOTAVersion = "";
    private OtaNotification mOtaNotification = null;
    boolean mNewFirmwareFound = false;
    boolean mAppForeground = false;
    long mDeviceVersion;
    private ServiceBinder mBinder = new ServiceBinder();
    private String mServiceState = "";
    private String StateKey = "State";
    private String VersionKey = "Version";
    public static final String FwCheckKey = "FWCheck";

    private PendingIntent pendingIntent;
    boolean bResume = false;
    public class ServiceBinder extends Binder {
        SystemUpdateService getService() {
            return SystemUpdateService.this;
        }
    }


    public SystemUpdateService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        Log.d(TAG, "[onBind] ");
        if ( mOtaNotification == null ) mOtaNotification = new OtaNotification(getApplicationContext());
        return mBinder;
    }

    public int onStartCommand(Intent intent, int flags, int startId) {

        String action = intent.getAction();
        if (action == null) action = Util.Action.ACTION_BOOT_COMPLETE;
        Log.d(TAG, "[onStartCommand], action = " + action);
//        SetNextFWCheckTime(getApplicationContext());
        if ( mOtaNotification == null ) mOtaNotification = new OtaNotification(getApplicationContext());
        mServiceState = Util.getUpdateServiceState(getApplicationContext(), StateKey);
        //mDeviceVersion = Util.GetDeviceVersion(getApplicationContext());
        Log.d(TAG, "[SystemUpdateService], state = " + mServiceState + "version = " + mDeviceVersion);
        switch (mServiceState) {
            case "init":
                if (action.equals(Util.Action.ACTION_BOOT_COMPLETE)) {
                    DelayedQueryNewFirmware();
                }
                else
                    query_new_version();
                break;
            case "newfw":
                mOtaNotification.IssueNewFirmwareNotification(getApplicationContext().getString(R.string.new_fw_found),getApplicationContext().getString(R.string.tap_to_download));
                //mHandler.sendMessage(mHandler.obtainMessage(SystemUpdateService.MSG_NEWVERSIONDETECTED));
                break;
            case "download_complete":
                mOtaNotification.IssueFirmwareCompleteNotification(getApplicationContext().getString(R.string.new_fw_download), getApplicationContext().getString(R.string.tap_to_upgrade));
                break;
            case "downloading":
                ResumeDownload(getApplicationContext());
            default:
                break;
        }
        /*
        if (Util.Action.ACTION_AUTO_QUERY_NEWVERSION.equals(action)) {
            query_new_version();
            if (mHandler != null) {
                mHandler.sendMessage(mHandler.obtainMessage(SystemUpdateService.MSG_NEWVERSIONDETECTED));
                Log.d(TAG, "onQueryNewVersion, Send new version founded message.");
            }
        }
        */
        return START_STICKY;
    }
    @Override
    public void onDestroy() {
        Log.d(TAG, "Service onDestroy");
        mOtaNotification = null;
        super.onDestroy();
    }
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "test ...Set onCreate Intent alarmIntent");
        Intent alarmIntent = new Intent(getApplicationContext(), AlarmReceiver.class);
        pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, alarmIntent, 0);
    }
    public String getUpdateServiceState() {
        mServiceState = Util.getUpdateServiceState(getApplicationContext(), StateKey);
        return mServiceState;
    }
    public void SetAppForeground(boolean fg) {
        if (!fg) mOtaNotification.CancelDownloadNotification();
        mAppForeground = fg;

    }
    public void SetNextFWCheckTime(Context context) {
        Log.d(TAG, "Set Alarm to next time check fw update");
        //CancelAlarm();
        AlarmManager manager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Calendar calendar = Calendar.getInstance();

        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, 12);
        calendar.set(Calendar.MINUTE, 0);
        manager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis() + AlarmManager.INTERVAL_DAY,pendingIntent);
        //manager.setInexactRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pendingIntent);
    }
    private void DelayedQueryNewFirmware() {
        Log.d(TAG, "DelayedQueryNewFirmware");
        CancelAlarm();
        int interval = 1 * 60 * 1000;
        AlarmManager manager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        manager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + interval,pendingIntent);

    }
    private void CancelAlarm() {
        AlarmManager manager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        manager.cancel(pendingIntent);
    }
    public void CancelNewFirmwareNotify() {
        mOtaNotification.CancelNewFirmwareNotification();
    }
    public void CancelDownloadCompleteNotify() {
        mOtaNotification.CancelFirmwareCompleteNotification();
    }
    private void SetFirmwareCheckTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm");
        Date dt=new Date(System.currentTimeMillis());
        String chk_time = sdf.format(dt);
        Log.d(TAG, "SetFirmwareCheckTime : " + chk_time);
        Util.setUpdateServiceState(getApplicationContext(), FwCheckKey, chk_time);
    }
    private void SetPeriodicCheck(Context context) {
        Log.d(TAG, "Set Periodic Check Firmware Download");

        CancelAlarm();
        int interval = RETRY_PERIOD * 60 * 1000;
        AlarmManager manager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

            manager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + interval,pendingIntent);
    }
    // check ota firmware version
    public Boolean CheckNewFirmware(String OTAVersion) {
        long ota_ver;
        mDeviceVersion = Util.GetDeviceVersion(getApplicationContext());
        if (OTAVersion != "")
            ota_ver = Long.parseLong(OTAVersion);
        else
            return false;
        Log.d(TAG, "Local ver: " + mDeviceVersion + " Remote ver: " + ota_ver);
        return ( ota_ver > mDeviceVersion ) ? true : false;
    }
    // handle error situation
    public void HandleOtaVerifyUseCase(int code) {
        switch (code) {
            case OtaVerify.RESULT_MD5_ERROR:
            case OtaVerify.RESULT_UNZIP_ERROR:
            case OtaVerify.RESULT_FILE_NOT_FOUND:
            case OtaVerify.RESULT_CONFIRM_UPDATE:
                Util.setUpdateServiceState(getApplicationContext(), StateKey, "init");
                SetNextFWCheckTime(getApplicationContext());
                break;


        }
    }

    void setHandler(Handler handler) {
        mHandler = handler;
    }

    // Download file thread
    class DownloadFilesThread extends Thread {
        String mDownloadFile;
        long mFileSize;
        Context mContext;

        public DownloadFilesThread(String file, Context context) {
            mDownloadFile = file;
            mContext = context;
        }

        public void run() {
            URL url;
            HttpURLConnection urlConnection = null;
            FileOutputStream outputStream;
            int count=0;
            Integer result;
            long total = 0;
            long LocalFileSize = 0;
            int preProgress = 0, Progress = 0;
            try {
                url = new URL(mDownloadFile);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setConnectTimeout(URL_CONNECT_TIMEOUT);
                urlConnection.setReadTimeout(URL_READ_TIMEOUT);
                mFileSize = (long) urlConnection.getContentLength();

                //InputStream in = new BufferedInputStream(urlConnection.getInputStream(),HTTP_TRANSFER_BUF);
                File LocalFile = new File(getApplicationContext().getFilesDir(), Util.VSOta.VS_OTA_LocalPackage);
                if (LocalFile != null)
                    LocalFileSize =  LocalFile.length();
                else
                    LocalFileSize =  0;
                //outputStream = mContext.openFileOutput(Util.VSOta.VS_OTA_LocalPackage, mContext.MODE_PRIVATE);
                outputStream = new FileOutputStream(LocalFile);
                byte data[] = new byte[HTTP_TRANSFER_BUF];

                Log.d(TAG, "download file size : " + mFileSize );
//                while ((count = in.read(data)) != -1) {
//                    if (isInterrupted()) break;
//                    total += count;
//                    //publishProgress((int)((total*100)/(float) mFileSize));
//                    // writing data to file
//                    outputStream.write(data, 0, count);
//                    //Log.d(TAG, "write data : " + total  + " " + (int)((total*100)/(float) mFileSize));
//                    Progress = (int)((total*100)/(float) mFileSize);
//                    if (Progress > preProgress) {
//                        // publishing the progress....
//                        publishProgress(Progress);
//                        Log.d(TAG, "write data : " + total  + " " + Progress);
//                        preProgress = Progress;
//                    }
//                }

                if (outputStream != null) {
                    outputStream.flush();
                    outputStream.close();
                }
                if ((total != mFileSize) || (isInterrupted())) {
                    Log.d(TAG, "Download is interrupted");
                    if (!bResume)
                        new File(Util.VSOta.VS_OTA_LocalPackage).delete();

                    if (mOtaNotification != null)
                        mOtaNotification.CancelDownloadNotification();

                }
            }
            catch (Exception e) {
                e.printStackTrace();
                if (!bResume)
                    new File(Util.VSOta.VS_OTA_LocalPackage).delete();
                if (mOtaNotification != null)
                    mOtaNotification.CancelDownloadNotification();
                if (mHandler != null) {
                    Message msg = mHandler.obtainMessage(SystemUpdateService.MSG_DOWNLOADFAIL);
                    mHandler.sendMessage(msg);
                }
            } finally {
                if(urlConnection != null)
                    urlConnection.disconnect();
            }
            // successfully download
            if (total == mFileSize) {
                if (mHandler != null) {
                    Message msg = mHandler.obtainMessage(SystemUpdateService.MSG_DOWNLOADSUCCESS);
                    mHandler.sendMessage(msg);
                }
                if (mOtaNotification != null) {
                    mOtaNotification.IssueFirmwareCompleteNotification(mContext.getString(R.string.new_fw_download), mContext.getString(R.string.tap_to_upgrade));
                    mOtaNotification.CancelDownloadNotification();
                }
                Util.setUpdateServiceState(getApplicationContext(), StateKey, "download_complete");
                SetNextFWCheckTime(getApplicationContext());
            }
            else {
                SetPeriodicCheck(getApplicationContext());
            }
            Log.d(TAG, "end thread");
        }
        private void publishProgress(int progress) {
            if (mHandler != null) {
                Bundle bundle = new Bundle();
                bundle.putInt("progress", progress);
                Message msg = mHandler.obtainMessage(SystemUpdateService.MSG_DOWNLOADPROGRESS);
                msg.setData(bundle);
                mHandler.sendMessage(msg);
            }
            // send notification
            if (mOtaNotification != null)
                mOtaNotification.SetDownloadProgress(progress);
        }
    }
    public boolean CheckDownloadThreadAlive() {
        if (mDownloadFWThread != null)  {
            return mDownloadFWThread.isAlive();
        }
        else return false;
    }
    public void ResumeDownload(Context context) {
        Log.d(TAG, "Resume Download");
        if (mDownloadFWThread != null)  {
            if (mDownloadFWThread.isAlive()) {
                Log.d(TAG, "Download thread is alive");
                return;
            }
        }
        Log.d(TAG, "Start Download Thread");
        if (mHandler != null)
            mHandler.sendMessage(mHandler.obtainMessage(SystemUpdateService.MSG_STARTRESUMEDOWNLOAD));
        Util.setUpdateServiceState(getApplicationContext(), StateKey, "downloading");
        mDownloadFWThread = new DownloadFilesThread(Util.VSOta.VS_OTA_Package, context);
        mDownloadFWThread.start();
        mOtaNotification.IssueDownloadNotification(context.getString(R.string.download_dlg_title), context.getString(R.string.download_dlg_message));
        mOtaNotification.CancelNewFirmwareNotification();
    }
    public void DownloadFirmware(Context context) {
        if (mDownloadFWThread != null)  {
            if (mDownloadFWThread.isAlive()) {
                Log.d(TAG, "Download thread is alive");
                return;
            }
        }
        Util.setUpdateServiceState(getApplicationContext(), StateKey, "downloading");
        Util.setUpdateServiceState(getApplicationContext(), VersionKey, mOTAVersion);
        mDownloadFWThread = new DownloadFilesThread(Util.VSOta.VS_OTA_Package, context);
        mDownloadFWThread.start();
        mOtaNotification.IssueDownloadNotification(context.getString(R.string.download_dlg_title), context.getString(R.string.download_dlg_message));
        mOtaNotification.CancelNewFirmwareNotification();
    }
    public void StopDownloadFirmware() {
        if (mDownloadFWThread!= null) {
            mDownloadFWThread.interrupt();
            mDownloadFWThread = null;
        }
    }
    public void query_new_version() {

        query_version query_version_thread = new query_version();
        query_version_thread.start();

    }
    class query_version extends Thread {
        public query_version() {

        }

        public void run() {
            URL url;
            HttpURLConnection urlConnection = null;
            try {
                url = new URL(Util.VSOta.VS_OTA_Version);
                urlConnection = (HttpURLConnection) url.openConnection();
                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                mOTAVersion = readStream(in);
                mNewFirmwareFound = CheckNewFirmware(mOTAVersion);
            }
            catch (Exception e) {
                e.printStackTrace();
                mNewFirmwareFound = false;
            } finally {
                if(urlConnection != null)
                    urlConnection.disconnect();
            }

            if (mNewFirmwareFound) {
                if (CheckLocalUpdateFile()) {
                    mOtaNotification.IssueFirmwareCompleteNotification(getApplicationContext().getString(R.string.new_fw_download), getApplicationContext().getString(R.string.tap_to_upgrade));
                    Util.setUpdateServiceState(getApplicationContext(), StateKey, "download_complete");
                }
                else {
                    mOtaNotification.IssueNewFirmwareNotification(getApplicationContext().getString(R.string.new_fw_found), getApplicationContext().getString(R.string.tap_to_download));
                    Util.setUpdateServiceState(getApplicationContext(), StateKey, "newfw");
                }
            }
            else {
                Util.setUpdateServiceState(getApplicationContext(), StateKey, "init");
                SetNextFWCheckTime(getApplicationContext());
            }
            if (mHandler != null) {
                if (mNewFirmwareFound) {
                    if (CheckLocalUpdateFile()) {
                        mHandler.sendMessage(mHandler.obtainMessage(SystemUpdateService.MSG_DOWNLOADSUCCESS));
                        Log.d(TAG, "Firmware file already there, send download complete message");
                    }
                    else {
                        mHandler.sendMessage(mHandler.obtainMessage(SystemUpdateService.MSG_NEWVERSIONDETECTED));
                        Log.d(TAG, "onQueryNewVersion, Send new version founded message.");
                    }
                }
                else {
                    mHandler.sendMessage(mHandler.obtainMessage(SystemUpdateService.MSG_NONEWVERSIONDETECTED));
                    Log.d(TAG, "onQueryNewVersion, no new version founded message.");
                }
            }
            SetFirmwareCheckTime();
            Log.i(TAG, " get OTA version " + mOTAVersion + " from " + Util.VSOta.VS_OTA_Version);
        }
        private String readStream(InputStream in) {
            BufferedReader reader = null;
            String line;
            StringBuffer response = new StringBuffer();
            try {
                reader = new BufferedReader(new InputStreamReader(in));
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return response.toString();
        }
        private boolean CheckLocalUpdateFile(){
            URL url;
            HttpURLConnection urlConnection = null;
            long RemoteFileSize = 0, LocalFileSize = 0;
            boolean file_exist = false;
            try {
                url = new URL(Util.VSOta.VS_OTA_Package);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setConnectTimeout(URL_CONNECT_TIMEOUT);
                urlConnection.setReadTimeout(URL_READ_TIMEOUT);
                RemoteFileSize = (long) urlConnection.getContentLength();

                File LocalFile = new File(getApplicationContext().getFilesDir(), Util.VSOta.VS_OTA_LocalPackage);
                if (LocalFile != null)
                    LocalFileSize =  LocalFile.length();
                else
                    LocalFileSize =  0;
            }
            catch (Exception e) {
                e.printStackTrace();
                file_exist = false;
            } finally {
                if(urlConnection != null)
                    urlConnection.disconnect();
            }
            if (RemoteFileSize == LocalFileSize )
                file_exist = true;
            Log.d(TAG, "Local update file : " + LocalFileSize + "Remote update file: " + RemoteFileSize + " " + file_exist);
            return file_exist;
        }
    }


}
