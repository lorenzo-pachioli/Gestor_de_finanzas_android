package com.notificationcapture.app.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.widget.RemoteViews;

import androidx.appcompat.app.AppCompatDelegate;

import com.notificationcapture.app.MainActivity;
import com.notificationcapture.app.R;
import com.notificationcapture.app.utils.SecurityPreferencesManager;
import com.notificationcapture.app.views.FastChargeActivity;

public class FastChargeWidgetProvider extends AppWidgetProvider {

    private static final String EXTRA_NIGHT_MODE_OVERRIDE = "night_mode_override";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(intent.getAction())) {
            // Si el Intent trae un override de modo oscuro (enviado desde PerfilFragment),
            // lo usamos directamente sin leer del disco (evita race condition con .apply())
            int nightModeOverride = intent.getIntExtra(EXTRA_NIGHT_MODE_OVERRIDE, -1);
            int[] ids = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
            if (ids != null && ids.length > 0) {
                AppWidgetManager mgr = AppWidgetManager.getInstance(context);
                for (int id : ids) {
                    updateAppWidget(context, mgr, id, nightModeOverride);
                }
                return;
            }
        }
        super.onReceive(context, intent);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId, -1);
        }
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
        updateAppWidget(context, appWidgetManager, appWidgetId, -1);
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
    }

    public static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId, int nightModeOverride) {
        boolean isDarkMode;

        if (nightModeOverride == AppCompatDelegate.MODE_NIGHT_YES) {
            isDarkMode = true;
        } else if (nightModeOverride == AppCompatDelegate.MODE_NIGHT_NO) {
            isDarkMode = false;
        } else {
            // Fallback: leer de las preferencias (para updates periódicos del sistema)
            SecurityPreferencesManager prefsManager = new SecurityPreferencesManager(context);
            int nightMode = prefsManager.getNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
            if (nightMode == AppCompatDelegate.MODE_NIGHT_YES) {
                isDarkMode = true;
            } else if (nightMode == AppCompatDelegate.MODE_NIGHT_NO) {
                isDarkMode = false;
            } else {
                int currentNightMode = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
                isDarkMode = (currentNightMode == Configuration.UI_MODE_NIGHT_YES);
            }
        }

        Bundle options = appWidgetManager.getAppWidgetOptions(appWidgetId);
        int minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);

        int layoutId;
        if (minWidth != 0 && minWidth < 110) {
            layoutId = isDarkMode ? R.layout.widget_fast_charge_small_dark : R.layout.widget_fast_charge_small;
        } else if (minWidth != 0 && minWidth < 180) {
            layoutId = isDarkMode ? R.layout.widget_fast_charge_medium_dark : R.layout.widget_fast_charge_medium;
        } else {
            layoutId = isDarkMode ? R.layout.widget_fast_charge_dark : R.layout.widget_fast_charge;
        }

        RemoteViews views = new RemoteViews(context.getPackageName(), layoutId);

        // PendingIntent para FastChargeActivity (toque en el área "Crear +")
        Intent fastChargeIntent = new Intent(context, FastChargeActivity.class);
        fastChargeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent fastChargePendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId,
                fastChargeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        views.setOnClickPendingIntent(R.id.widget_root, fastChargePendingIntent);

        // PendingIntent para MainActivity (toque en el logo) — solo layouts grandes/medianos
        if (minWidth == 0 || minWidth >= 110) {
            Intent mainIntent = new Intent(context, MainActivity.class);
            mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent mainPendingIntent = PendingIntent.getActivity(
                    context,
                    appWidgetId + 1000,
                    mainIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            if (minWidth == 0 || minWidth >= 180) {
                views.setOnClickPendingIntent(R.id.widget_logo, mainPendingIntent);
            }
        }

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
}
