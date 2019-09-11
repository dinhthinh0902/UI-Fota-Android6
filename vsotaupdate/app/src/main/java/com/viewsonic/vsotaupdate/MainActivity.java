package com.viewsonic.vsotaupdate;

import android.graphics.Color;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.os.Handler;
import android.content.Context;
import android.content.Intent;
import android.os.Message;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.content.ComponentName;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Date;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.view.LayoutInflater;
import android.content.DialogInterface;
import android.support.v4.app.FragmentActivity;
import android.net.Uri;
import java.net.URL;
import com.viewsonic.vsotaupdate.DownloadFilesTask;
import android.app.ProgressDialog;
import android.view.WindowManager;
import com.viewsonic.vsotaupdate.OtaVerify;
import com.viewsonic.vsotaupdate.OtaRecovery;
import com.viewsonic.vsotaupdate.OtaAlertDialog;
//import com.viewsonic.vsotaupdate.SampleAlarmReceiver;
import android.widget.ProgressBar;
import android.widget.Button;

public class MainActivity extends ActionBarActivity {
    //private final SampleAlarmReceiver alarm = new SampleAlarmReceiver();
    private static final String TAG = "vsota/MainActivity";
    private SystemUpdateService mService = null;
    private ProgressDialog mDownloadProgress = null;
    private ProgressDialog mProgress;
    //private OtaProgressDialog mDownloadProgress = null;
    private FirmwareDownloadDialog mFirmwareDownloadDlg = null;
    private FirmwareInstallDialog mFirmwareInstallDlg = null;
    private AlertDialog mAlertDlg;
    static final int START_BY_LAUNCHER = 0;
    static final int START_BY_NEW_FIRMWARE_NOTIFY = 1;
    static final int START_BY_FIRMWARE_COMPLETE_NOTIFY = 2;
    static final int START_BY_CANCEL_DOWNLOAD = 3;
    static final int START_BY_FIRMWARE_DOWNLOADING = 4;
    boolean mForeground = true;
    int mStartReason = 0;
    String mServiceState = "init";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "[onCreate]");
        setContentView(R.layout.activity_main);
        //alarm.setAlarm(this);
        // connect to SystemUpdateService
        Intent serviceIntent = new Intent(this, SystemUpdateService.class);
        serviceIntent.setAction(Util.Action.ACTION_NORMAL);
        bindService(serviceIntent, mConnection, Context.BIND_AUTO_CREATE);
        //startService(serviceIntent);
    }
    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "[onStart]");
        mForeground = true;
    }
    protected void onResume () {
        Log.d(TAG, "[onResume]");
        super.onResume();

        mForeground = true;
        /*••••••••
        if (mService != null)
            mServiceState = mService.getUpdateServiceState();
        Log.d(TAG, "Update service state :  " + mServiceState );
        */
        mStartReason = 0;
        Bundle bundle = getIntent().getExtras();
        if (bundle != null ) {
            int start_reason = bundle.getInt("start_by");
            mStartReason = start_reason;
            Log.d(TAG, "Resume Activity by " + start_reason);

            bundle.putInt("start_by", MainActivity.START_BY_LAUNCHER);
            getIntent().putExtras(bundle);
        }
        if (mService != null) {
            mService.SetAppForeground(mForeground);
            DoUIActions();
        }
    }
    public void onSaveInstanceState(Bundle savedInstanceState) {
        Log.d(TAG, "[onSaveInstanceState]");
        /*
        if (mFirmwareDownloadDlg != null) {
            Log.d(TAG, "[shutdown download dialog]");
            mFirmwareDownloadDlg.dismiss();
            mFirmwareDownloadDlg = null;
        }
        */
        super.onSaveInstanceState(savedInstanceState);
    }
    protected void onPause () {
        Log.d(TAG, "[onPause]");
        super.onPause();
        if (mDownloadProgress != null) {
            mDownloadProgress.dismiss();
            mDownloadProgress = null;
        }

        mForeground = false;
        if (mService != null) {
            mService.SetAppForeground(mForeground);
        }

    }
    protected void onDestroy () {
        Log.d(TAG, "[onDestory]");
        super.onDestroy();

        unbindService(mConnection);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    public void DoUIActions() {
        if ( mService != null ) {
            mServiceState = mService.getUpdateServiceState();
        }
        Log.d(TAG, "[DoUIActions] Check Update service state: " + mServiceState);
        Log.d(TAG, "[DoUIActions] Check start reason: " + mStartReason);

        ProgressBar spinner =  (ProgressBar)findViewById(R.id.progressBar1);
        Button but = (Button) findViewById(R.id.button);
        switch (mServiceState) {
            case "init":
                spinner.setProgress(100);
                break;
            case "newfw":
                mUiHandler.sendMessage(mUiHandler.obtainMessage(SystemUpdateService.MSG_NEWVERSIONDETECTED));
                mService.CancelNewFirmwareNotify();
                spinner.setProgress(100);
                break;
            case "download_complete":
                mUiHandler.sendMessage(mUiHandler.obtainMessage(SystemUpdateService.MSG_DOWNLOADSUCCESS));
                mService.CancelDownloadCompleteNotify();
                spinner.setProgress(100);
                break;
            case "downloading":
                boolean isDownload = mService.CheckDownloadThreadAlive();
                if (isDownload) but.setText(R.string.cancel_update);
                else but.setText(R.string.check_update);
            default:
                break;
        }
        ShowFirmwareCheckDate();

    }
    public class FirmwareInstallDialog extends DialogFragment {
        private boolean isCancel = false;
        private boolean isOK = false;
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            LayoutInflater inflater = getActivity().getLayoutInflater();
            builder.setView(inflater.inflate(R.layout.fwinstall_dialog, null));
            isCancel = false;
            isOK = false;
            builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    isOK = true;
                    OtaVerify otaverify = new OtaVerify(getApplicationContext(), mUiHandler);
                    otaverify.VerifySystemUpdate_Thread();
                    ProgressBar spinner = (ProgressBar)findViewById(R.id.progressBar1);
                    spinner.setIndeterminate(true);

                }
            });
            builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    isCancel = true;
                    FirmwareInstallDialog.this.getDialog().cancel();
                }
            });
            return builder.create();
        }
        public void onPause() {
            if ( (!isCancel) && (!isOK) )
                getDialog().cancel();
            super.onPause();
        }
    }
    public class FirmwareDownloadDialog extends DialogFragment {
        private boolean isCancel = false;
        private boolean isOK = false;
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            LayoutInflater inflater = getActivity().getLayoutInflater();
            builder.setView(inflater.inflate(R.layout.fwdownload_dialog, null));
            isCancel = false;
            isOK = false;
            builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    isOK = true;
                    //CreateProgressDialog(getString(R.string.download_dlg_title), getString(R.string.download_dlg_message), getString(R.string.cancel));
                    mUiHandler.sendMessage(mUiHandler.obtainMessage(SystemUpdateService.MSG_STARTRESUMEDOWNLOAD));
                    mService.DownloadFirmware(getApplicationContext());
                }
            });
            builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    isCancel = true;
                    FirmwareDownloadDialog.this.getDialog().cancel();
                    mService.CancelNewFirmwareNotify();
                }
            });
            return builder.create();
        }
        public void onStop() {
            super.onStop();
        }
        public void onDestroy() {
            super.onStop();
        }
        public void onPause() {
            if ( (!isCancel) && (!isOK) )
                getDialog().cancel();
            super.onPause();
        }
    }
    private void CreateProgressDialog(String title, String message, String buttonText) {
        mDownloadProgress = new ProgressDialog(this);
        mDownloadProgress.setTitle(title);
        mDownloadProgress.setMessage(message);
        mDownloadProgress.setCancelable(false);
        mDownloadProgress.setMax(100);
        mDownloadProgress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mDownloadProgress.setButton(buttonText, new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int which)
            {
                // Use either finish() or return() to either close the activity or just the dialog
                Log.d(TAG, "[onClick] Cancel download progress");
                mService.StopDownloadFirmware();
                return;
            }
        });
        mDownloadProgress.show();
        /*
        if (mForeground) {
            Log.d(TAG, "ReCreate Progress Dialog!!");
            mDownloadProgress = new OtaProgressDialog();
            mDownloadProgress.SetService(mService);
            mDownloadProgress.show(getFragmentManager(), "install_progress");
        }
        else
            mDownloadProgress = null;
        */
    }
    private AlertDialog ShowSimpleAlertDialog(String message) {
        AlertDialog dlg;
        AlertDialog.Builder builder;
        builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.action_bar_title));
        builder.setMessage(message);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // do nothing
            }
        });
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        dlg = builder.create();
        builder.show();
        return dlg;
    }
    private AlertDialog ShowSimpleAlertDialog(String message, boolean cancel) {
        AlertDialog dlg;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.action_bar_title));
        builder.setMessage(message);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                OtaRecovery.RemoveSystemUpdateFile(getApplicationContext());
                OtaRecovery.WriteRecoveryCommand(getApplicationContext());
                OtaRecovery.RebootRecovery(getApplicationContext());
            }
        });
        if (cancel)
            builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                    OtaRecovery.RemoveSystemUpdateFile(getApplicationContext());
            }
        });
        dlg = builder.create();
        builder.show();
        return dlg;
    }
    public void onCheckVersion(View v) {
        Log.d(TAG, "[onCheckVersion]");

        if (mService != null ) {
            switch (mService.getUpdateServiceState()) {
                case "init":
                case "newfw":
                    mService.query_new_version();
                    break;
                case "download_complete":
                    LaunchFirmwareInstallDlg();
                    break;
                case "downloading":
                    //CreateProgressDialog(getString(R.string.download_dlg_title), getString(R.string.download_dlg_message), getString(R.string.cancel));
                    Button but = (Button) findViewById(R.id.button);
                    if (but.getText().equals(getString(R.string.check_update))) {
                        mService.DownloadFirmware(getApplicationContext());
                        but.setText(R.string.cancel_update);
                        mUiHandler.sendMessage(mUiHandler.obtainMessage(SystemUpdateService.MSG_STARTRESUMEDOWNLOAD));
                    }
                    else if (but.getText().equals(getString(R.string.cancel_update))) {
                        //mService.StopDownloadFirmware();
                        but.setText(R.string.check_update);
                        mUiHandler.sendMessage(mUiHandler.obtainMessage(SystemUpdateService.MSG_CANCELDOWNLOAD));
                    }
                    break;
            }

            /*
            if (mService.mOTAVersion != null)
                launch_fw_download_dialog();
            */
        }
    }
    public void ShowFirmwareCheckDate() {
        String show_str;
        TextView t = (TextView)findViewById(R.id.update_time);
        show_str = Util.getUpdateServiceState(this, SystemUpdateService.FwCheckKey);
        if (show_str.equals("init")) show_str = "Never";
        t.setText(show_str);
    }
    public void LaunchFirmwareInstallDlg() {
        if (mForeground) {
            Log.d(TAG, "Launch Firmware Install Dialog");
            FirmwareInstallDialog mFirmwareInstallDlg = new FirmwareInstallDialog();
            if (mDownloadProgress != null) {
                mDownloadProgress.dismiss();
                mDownloadProgress = null;
            }
            mFirmwareInstallDlg.show(getFragmentManager(), "firmware_install");
        }
    }
    public void launch_fw_download_dialog() {
        if (mForeground) {
            Log.d(TAG, "ReLaunch Firmware download Dialog");
            if (mFirmwareDownloadDlg == null ) {
                FirmwareDownloadDialog mFirmwareDownloadDlg = new FirmwareDownloadDialog();
                mFirmwareDownloadDlg.show(getFragmentManager(), "firmware");
            }
        }
        else mFirmwareDownloadDlg = null;
    }

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "[onServiceConnected], thread name = " + Thread.currentThread().getName());

            mService = ((SystemUpdateService.ServiceBinder) service).getService();
            Log.d(TAG, "[onServiceConnected], mService = " + mService);
            if (mService != null) {
                mService.setHandler(mUiHandler);
                mService.SetAppForeground(mForeground);
                DoUIActions();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "[onServiceDisconnected]");
            if (mService != null) {
                mService.setHandler(null);

                mService = null;
            }
        }

    };


    private Handler mUiHandler = new Handler() {
        @SuppressWarnings("unchecked")
        @Override
        public void handleMessage(Message msg) {
            /*
            Log.d(TAG,
                    "[mUiHandler] handlerMessage " + msg.what + ", Thread = "
                            + Thread.currentThread());
            */
            ProgressBar spinner =  (ProgressBar)findViewById(R.id.progressBar1);
            TextView percentage = (TextView) findViewById(R.id.percentage);
            Button but = (Button) findViewById(R.id.button);

            switch (msg.what) {
                case SystemUpdateService.MSG_NEWVERSIONDETECTED:
                    //todo
                    launch_fw_download_dialog();
                    Log.d(TAG, "New version detected");
                    ShowFirmwareCheckDate();
                    break;

                case SystemUpdateService.MSG_DOWNLOADPROGRESS:
                    Bundle bundle = msg.getData();
                    int progress = bundle.getInt("progress");

                    /*
                    if (mDownloadProgress != null)
                        mDownloadProgress.setProgress(progress);
                    else {
                        CreateProgressDialog(getString(R.string.download_dlg_title), getString(R.string.download_dlg_message), getString(R.string.cancel));
                        mDownloadProgress.setProgress(progress);
                    }
                    */
                    spinner.setIndeterminate(false);
                    spinner.setProgress(progress);
                    percentage.setText(new String(getString(R.string.progress) + " " + progress + " %"));
                    break;
                case SystemUpdateService.MSG_NONEWVERSIONDETECTED:
                    ShowFirmwareCheckDate();
                    break;
                case SystemUpdateService.MSG_DOWNLOADSUCCESS:

                    LaunchFirmwareInstallDlg();
                    Log.d(TAG, "Re Launch Firmware Install Dialog");

                    spinner.setIndeterminate(false);
                    //spinner.setBackgroundResource(getResources().getColor(android.R.color.holo_blue_light));
                    percentage.setText("");
                    spinner.setProgress(100);
                    but.setText(R.string.check_update);
                    break;
                case SystemUpdateService.MSG_CANCELDOWNLOAD:
                    Log.d(TAG,"Cancel Download");
                    mService.StopDownloadFirmware();
                    spinner.setIndeterminate(false);
                    spinner.setProgress(100);
                    percentage.setText("");
                    break;
                case SystemUpdateService.MSG_STARTRESUMEDOWNLOAD:
                    Log.d(TAG,"Resume Dowload");
                    /*
                    CreateProgressDialog(getString(R.string.download_dlg_title), getString(R.string.download_dlg_message), getString(R.string.cancel));
                    spinner.setIndeterminate(true);
                    */
                    spinner.setMax(100);
                    spinner.setProgress(0);
                    spinner.setIndeterminate(false);
                    //spinner.setBackgroundResource(getResources().getColor(android.R.color.transparent));
                    percentage.setText(new String(getString(R.string.progress) + " 0%"));
                    but.setText(R.string.cancel_update);
                    break;
                case SystemUpdateService.MSG_DOWNLOADFAIL:
                    Log.d(TAG,"Download fail");
                    if (mDownloadProgress != null) {
                        mDownloadProgress.dismiss();
                        mDownloadProgress = null;
                    }
                    //spinner.setBackgroundResource(getResources().getColor(android.R.color.holo_blue_dark));
                    spinner.setIndeterminate(false);
                    spinner.setProgress(100);
                    percentage.setText("");
                    but.setText(R.string.check_update);
                    break;
                case OtaVerify.RESULT_BATTERY:
                    mAlertDlg = ShowSimpleAlertDialog(getString(R.string.battery_error_message));
                    spinner.setIndeterminate(false);
                    break;
                case OtaVerify.RESULT_NO_AC:
                    mAlertDlg = ShowSimpleAlertDialog(getString(R.string.AC_error_message));
                    spinner.setIndeterminate(false);
                    break;
                case OtaVerify.RESULT_MD5_ERROR:
                case OtaVerify.RESULT_UNZIP_ERROR:
                case OtaVerify.RESULT_FILE_NOT_FOUND:
                    mAlertDlg = ShowSimpleAlertDialog(getString(R.string.file_error_message));
                    mService.HandleOtaVerifyUseCase(msg.what);
                    spinner.setIndeterminate(false);
                    break;

                case OtaVerify.RESULT_MD5_OK:
                    /*
                    mAlertDlg = ShowSimpleAlertDialog(getString(R.string.confirm_reboot), true);
                    spinner.setIndeterminate(false);
                    */
                    OtaAlertDialog df = OtaAlertDialog.newInstance(getString(R.string.action_bar_title), getString(R.string.confirm_reboot), true);
                    df.setUIhandler(getApplicationContext(), mUiHandler);
                    df.show(getFragmentManager(), "confirm");
                    //mAlertDlg.show();
                case OtaVerify.RESULT_CONFIRM_UPDATE:
                    mService.HandleOtaVerifyUseCase(msg.what);
                    OtaRecovery.RemoveSystemUpdateFile(getApplicationContext());
                    OtaRecovery.WriteRecoveryCommand(getApplicationContext());
                    OtaRecovery.RebootRecovery(getApplicationContext());
                    break;
                case OtaVerify.RESULT_CANCEL_UPDATE:
                    spinner.setIndeterminate(false);
                    spinner.setProgress(100);
                    break;
                case SystemUpdateService.MSG_UNKNOWERROR:
                    break;
                default:
                    break;
            }
        }
    };

}
