package com.viewsonic.vsotaupdate;

import android.content.Context;
import android.app.Notification;
import android.app.NotificationManager;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.content.Intent;
import android.app.PendingIntent;
import android.util.Log;
import com.viewsonic.vsotaupdate.MainActivity;

public class OtaNotification {
    private String TAG = "vsotaupdate/OtaNotification";
    public static final int NOTIFICATION_ID = 168;
    public static final int NEW_FW_NOTIFICATION_ID = 169;
    public static final int FW_COMPLETE_NOTIFICATION_ID = 170;
    private NotificationManager mNotificationManager;
    private Context mContext;
    NotificationCompat.Builder builder;
    NotificationCompat.Builder new_fw_builder;
    NotificationCompat.Builder fw_complete_builder;

    public OtaNotification(Context context){
        mContext = context;
    }
    public void CancelDownloadNotification() {
        if (mNotificationManager != null) {
            mNotificationManager.cancel(NOTIFICATION_ID);
            builder = null;
        }
    }
    public void CancelNewFirmwareNotification() {
        if (mNotificationManager != null) {
            mNotificationManager.cancel(NEW_FW_NOTIFICATION_ID);
            new_fw_builder = null;
        }
    }

    public void CancelFirmwareCompleteNotification() {
        if (mNotificationManager != null) {
            mNotificationManager.cancel(FW_COMPLETE_NOTIFICATION_ID);
            fw_complete_builder = null;
        }
    }
    public void SetDownloadProgress(int progress) {
        if (builder != null) {
            builder.setProgress(100, progress, false);
            mNotificationManager.notify(NOTIFICATION_ID, builder.build());
        }
    }

    public void IssueFirmwareCompleteNotification(String title, String content) {
        Log.d(TAG, "IssueFirmwareCompleteNotification");

        mNotificationManager = (NotificationManager) mContext.getSystemService(mContext.NOTIFICATION_SERVICE);

        // Constructs the Builder object.
        fw_complete_builder =
                new NotificationCompat.Builder(mContext)
                        .setSmallIcon(R.drawable.stat_download_completed)
                        .setContentTitle(title)
                        .setContentText(content)
                        .setDefaults(Notification.DEFAULT_ALL) // requires VIBRATE permission
                /*
                 * Sets the big view "big text" style and supplies the
                 * text (the user's reminder message) that will be displayed
                 * in the detail area of the expanded notification.
                 * These calls are ignored by the support library for
                 * pre-4.1 devices.
                 */
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(content));

        Intent resultIntent = new Intent(mContext, MainActivity.class);
        Bundle bundle = new Bundle();
        bundle.putInt("start_by", MainActivity.START_BY_FIRMWARE_COMPLETE_NOTIFY);
        resultIntent.putExtras(bundle);
        //resultIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        // Because clicking the notification opens a new ("special") activity, there's
        // no need to create an artificial back stack.
        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(
                        mContext,
                        0,
                        resultIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );

        fw_complete_builder.setContentIntent(resultPendingIntent);
        mNotificationManager.notify(FW_COMPLETE_NOTIFICATION_ID, fw_complete_builder.build());
    }

    public void IssueNewFirmwareNotification(String title, String content) {
        Log.d(TAG, "IssueNewFirmwareNotification");

        mNotificationManager = (NotificationManager) mContext.getSystemService(mContext.NOTIFICATION_SERVICE);

        // Constructs the Builder object.
        new_fw_builder =
                new NotificationCompat.Builder(mContext)
                        .setSmallIcon(R.drawable.new_fw)
                        .setContentTitle(title)
                        .setContentText(content)
                        .setDefaults(Notification.DEFAULT_ALL) // requires VIBRATE permission
                /*
                 * Sets the big view "big text" style and supplies the
                 * text (the user's reminder message) that will be displayed
                 * in the detail area of the expanded notification.
                 * These calls are ignored by the support library for
                 * pre-4.1 devices.
                 */
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(content));

        Intent resultIntent = new Intent(mContext, MainActivity.class);
        Bundle bundle = new Bundle();
        bundle.putInt("start_by", MainActivity.START_BY_NEW_FIRMWARE_NOTIFY);
        resultIntent.putExtras(bundle);
        //resultIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        // Because clicking the notification opens a new ("special") activity, there's
        // no need to create an artificial back stack.
        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(
                        mContext,
                        0,
                        resultIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );

        new_fw_builder.setContentIntent(resultPendingIntent);
        mNotificationManager.notify(NEW_FW_NOTIFICATION_ID, new_fw_builder.build());
    }

    public void IssueDownloadNotification(String title, String content) {
        Log.d(TAG, "IssueNotification");

        mNotificationManager = (NotificationManager) mContext.getSystemService(mContext.NOTIFICATION_SERVICE);
        /*
        Intent dismissIntent = new Intent(mContext, MainActivity.class);
        Bundle bundle = new Bundle();
        bundle.putInt("start_by", MainActivity.START_BY_CANCEL_DOWNLOAD);
        dismissIntent.putExtras(bundle);
        PendingIntent piDismiss = PendingIntent.getActivity(mContext, 0, dismissIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        */
        // Constructs the Builder object.
        builder =
                new NotificationCompat.Builder(mContext)
                        .setSmallIcon(R.drawable.stat_download_downloading)
                        .setContentTitle(title)
                        .setContentText(content)
                        .setDefaults(Notification.DEFAULT_LIGHTS)
                /*
                 * Sets the big view "big text" style and supplies the
                 * text (the user's reminder message) that will be displayed
                 * in the detail area of the expanded notification.
                 * These calls are ignored by the support library for
                 * pre-4.1 devices.
                 */
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(content));
                        /*
                        .addActi Cookies must be retained in subsequent requests.on (R.drawable.ic_stat_dismiss,
                                mContext.getString(R.string.cancel), piDismiss);
                        */
        Intent resultIntent = new Intent(mContext, MainActivity.class);
        Bundle bundle1 = new Bundle();
        bundle1.putInt("start_by", MainActivity.START_BY_FIRMWARE_DOWNLOADING);
        resultIntent.putExtras(bundle1);
        //resultIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        // Because clicking the notification opens a new ("special") activity, there's
        // no need to create an artificial back stack.
        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(
                        mContext,
                        0,
                        resultIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );

        builder.setContentIntent(resultPendingIntent);
        builder.setProgress(100, 0, false);
        mNotificationManager.notify(NOTIFICATION_ID, builder.build());
    }
}
