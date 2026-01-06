package com.notificationcapture.app.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import com.notificationcapture.app.MyApplication;

public class ErrorDialog {

    public static void show(String message) {
        Activity activity = MyApplication.getCurrentActivity();
        if (activity == null)
            return;

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    new AlertDialog.Builder(activity)
                            .setTitle("Error")
                            .setMessage(message)
                            .setPositiveButton("Cerrar", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            })
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show();
                } catch (Exception e) {
                    // Prevent crash if activity is finishing or destroyed
                    e.printStackTrace();
                }
            }
        });
    }
}
