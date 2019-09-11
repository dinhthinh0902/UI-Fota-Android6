package com.viewsonic.vsotaupdate;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.os.Message;
import android.view.LayoutInflater;
import android.content.DialogInterface;
import android.content.Context;
import android.widget.ProgressBar;
import android.os.Handler;

public class OtaAlertDialog extends DialogFragment {
    Context mContext;
    String mTitle;
    String mMessage;
    Handler mHandler;
    boolean mHasCancel;
    boolean isOK = false;
    boolean isCancel = false;

    public static OtaAlertDialog newInstance(String title, String message, boolean hasCancel) {
        OtaAlertDialog dlg = new OtaAlertDialog();
        Bundle args = new Bundle();
        args.putString("title", title);
        args.putString("msg", message);
        args.putBoolean("cancel", hasCancel);
        dlg.setArguments(args);
        return dlg;
    }
    public void setUIhandler(Context context, Handler handler) {mContext = context; mHandler = handler;}
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mTitle = getArguments().getString("title");
        mMessage = getArguments().getString("msg");
        mHasCancel = getArguments().getBoolean("cancel");

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        builder.setMessage(mMessage);
        builder.setTitle(mTitle);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                isOK = true;
                //OtaRecovery.RemoveSystemUpdateFile(mContext);
                Message msg = mHandler.obtainMessage(OtaVerify.RESULT_CONFIRM_UPDATE);
                mHandler.sendMessage(msg);
                /*
                OtaRecovery.WriteRecoveryCommand(mContext);
                OtaRecovery.RebootRecovery(mContext);
                */
            }
        });
        if (mHasCancel) {
            builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    isCancel = true;
                    Message msg = mHandler.obtainMessage(OtaVerify.RESULT_CANCEL_UPDATE);
                    mHandler.sendMessage(msg);
                    //OtaRecovery.RemoveSystemUpdateFile(mContext);
                }
            });
        }
        return builder.create();
    }
    public void onPause() {
        if ( (!isCancel) && (!isOK) )
            getDialog().cancel();
        super.onPause();
    }
}
