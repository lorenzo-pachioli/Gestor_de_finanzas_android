package com.notificationcapture.app.enums;

import com.notificationcapture.app.R;

public enum DialogType {
    ERROR("Error", android.R.drawable.ic_dialog_alert, R.color.red, "Cerrar"),
    CONFIRMATION("Confirmación", android.R.drawable.ic_dialog_alert, R.color.accent_main, "Confirmar"),
    INFO("Información", android.R.drawable.ic_dialog_info, R.color.primary_main, "Entendido"),
    SUCCESS("Éxito", android.R.drawable.ic_input_add, R.color.green, "Genial");

    private final String title;
    private final int icon;
    private final int color;
    private final String positiveButtonText;

    DialogType(String title, int icon, int color, String positiveButtonText) {
        this.title = title;
        this.icon = icon;
        this.color = color;
        this.positiveButtonText = positiveButtonText;
    }

    public String getTitle() {
        return title;
    }

    public int getIcon() {
        return icon;
    }

    public int getColor() {
        return color;
    }

    public String getPositiveButtonText() {
        return positiveButtonText;
    }
}
