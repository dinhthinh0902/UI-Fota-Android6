package com.viewsonic.vsotaupdate;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.app.ProgressDialog;
import android.util.Log;

public class OtaProgressDialog extends DialogFragment {
    private static final String TAG = "vsota/OtaProgressDialog";
    private SystemUpdateService mService = null;
    boolean isCancel = false;
    public ProgressDialog ProgressDlg;
    public void SetService(SystemUpdateService service) {
        mService = service;
    }
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        ProgressDlg = new ProgressDialog(getActivity(), getTheme());
        ProgressDlg.setTitle(getString(R.string.download_dlg_title));
        ProgressDlg.setMessage(getString(R.string.download_dlg_message));
        ProgressDlg.setCancelable(false);
        ProgressDlg.setMax(100);
        ProgressDlg.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        isCancel = false;
        ProgressDlg.setButton(getString(R.string.cancel), new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int which)
            {
                // Use either finish() or return() to either close the activity or just the dialog
                Log.d(TAG, "[onClick] Cancel download progress");
                mService.StopDownloadFirmware();
                isCancel = true;
                return;
            }
        });
        return ProgressDlg;
    }
    public void onPause() {
        if ( (!isCancel)  )
            ProgressDlg.cancel();
        super.onPause();
    }
}
