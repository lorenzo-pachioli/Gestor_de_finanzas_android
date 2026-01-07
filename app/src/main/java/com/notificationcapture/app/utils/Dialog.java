package com.notificationcapture.app.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import com.notificationcapture.app.MyApplication;
import com.notificationcapture.app.enums.DialogType;

public class Dialog {

    public static void show(String message, DialogType type, Runnable onConfirm) {
        Activity activity = MyApplication.getCurrentActivity();
        if (activity == null)
            return;

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    AlertDialog.Builder builder = new AlertDialog.Builder(activity)
                            .setTitle(type.getTitle())
                            .setMessage(message)
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
                    // Get buttons to apply colors
                    if (type.getColor() != 0) {
                        // We can set color here if needed, typically with getButton().setTextColor
                        // For now, let's just stick to default or theme colors unless user strictly
                        // requested custom button text colors
                        // User said: "defini un color para cada uno para usar en el boton y el icon"
                        // So we should try to set the button color.
                        int colorRes = androidx.core.content.ContextCompat.getColor(activity, type.getColor());
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(colorRes);
                        // Negative button usually stays default/grey
                    }

                } catch (Exception e) {
                    // Prevent crash if activity is finishing or destroyed
                    e.printStackTrace();
                }
            }
        });
    }

    public static void show(String message) {
        show(message, DialogType.ERROR, null);
    }
}
