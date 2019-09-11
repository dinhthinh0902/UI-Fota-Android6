package com.viewsonic.vsotaupdate;

/**
 * Created by brianchin on 2015/4/14.
 */

import android.app.ProgressDialog;
import android.os.AsyncTask;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import android.content.Context;
import java.io.File;
import java.io.FileOutputStream;
import android.util.Log;
import android.app.ProgressDialog;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;

public class DownloadFilesTask extends AsyncTask<String, Integer, Long> {

    public Context mContext;
    private long mFileSize;
    private static final String TAG = "vsotaupdate/DownloadFilesTask";
    public ProgressDialog mProgressDialog;

    protected Long doInBackground(String... urls) {
        URL url;
        HttpURLConnection urlConnection = null;
        FileOutputStream outputStream;
        int count=0;
        Integer result;

        try {
            url = new URL(urls[0]);
            urlConnection = (HttpURLConnection) url.openConnection();
            mFileSize = (long) urlConnection.getContentLength();

            InputStream in = new BufferedInputStream(urlConnection.getInputStream(),8192);
            outputStream = mContext.openFileOutput(Util.VSOta.VS_OTA_LocalPackage, mContext.MODE_PRIVATE);
            byte data[] = new byte[4096];
            long total = 0;

            while ((count = in.read(data)) != -1) {
                if ( isCancelled() ) break;
                total += count;
                // publishing the progress....
                // After this onProgressUpdate will be called
                publishProgress((int)((total*100)/(float) mFileSize));
                // writing data to file
                outputStream.write(data, 0, count);
                Log.d(TAG, "write data : " + total + " " + (int)((total*100)/(float) mFileSize));
            }

            if (outputStream != null) {
                outputStream.flush();
                outputStream.close();
            }
            if ( isCancelled() ) {
                new File(Util.VSOta.VS_OTA_LocalPackage).delete();
                Log.d(TAG, "Cancel download");
            }
            else if (total != mFileSize) {
                Log.d(TAG, "fail to download due to unknown reason");
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            mProgressDialog.dismiss();
            result = 0;
            return result.longValue();

        } finally {
            if(urlConnection != null)
                urlConnection.disconnect();
        }
        Log.d(TAG, "byebye");
        result = 1;
        return result.longValue();

    }
    protected void onCancelled (Long result) {
        Log.d(TAG, "Cancel AsyncTask " + result );
    }
    protected void onProgressUpdate(Integer... progress) {
        Log.d(TAG, "Update Progress data : " + progress[0]);
        mProgressDialog.setProgress(progress[0]);
    }

    protected void onPostExecute(Long result) {
        Log.d(TAG, "Task finished : " + result);
        mProgressDialog.dismiss();
        if (result == 0)
            new File(Util.VSOta.VS_OTA_LocalPackage).delete();
    }

}
