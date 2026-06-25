package com.notificationcapture.app.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.Toast;
import com.notificationcapture.app.MyApplication;
import com.notificationcapture.app.enums.DialogType;

import java.lang.ref.WeakReference;

public class Dialog {

    public static void show(String message, DialogType type, Runnable onConfirm) {
        show(message, type, onConfirm, MyApplication.getCurrentActivity());
    }

    public static void show(String message, DialogType type, Runnable onConfirm, Context context) {
        if (message == null) message = "";
        message = message.substring(0, Math.min(message.length(), 1000));
        if (context == null) {
            return;
        }
        if (context instanceof Activity) {
            Activity activity = (Activity) context;
            if (activity.isFinishing()) {
                return;
            }
        }

        if (context instanceof Activity) {
            String finalMessage = message;
            ((Activity) context).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                                .setTitle(type.getTitle())
                                .setMessage(finalMessage)
                                .setIcon(type.getIcon());

                        // Positive Button
                        builder.setPositiveButton(type.getPositiveButtonText(), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (onConfirm != null) {
                                    onConfirm.run();
                                }
                                dialog.dismiss();
                            }
                        });

                        // Negative Button (Available for CONFIRMATION)
                        if (type == DialogType.CONFIRMATION) {
                            builder.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
                        }

                        AlertDialog dialog = builder.create();
                        dialog.show();

                        // Customize button colors after show()
                        if (type.getColor() != 0) {
                            int colorRes = androidx.core.content.ContextCompat.getColor(context, type.getColor());
                            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(colorRes);
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    public static void show(String message) {
        show(message, DialogType.ERROR, null);
    }
}
