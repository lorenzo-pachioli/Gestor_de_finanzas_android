package com.notificationcapture.app.widget;

import android.app.PendingIntent;
import android.os.Bundle;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.widget.RemoteViews;

import androidx.appcompat.app.AppCompatDelegate;

import com.notificationcapture.app.R;
import com.notificationcapture.app.utils.SecurityPreferencesManager;
import com.notificationcapture.app.views.FastChargeActivity;

public class FastChargeWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
        updateAppWidget(context, appWidgetManager, appWidgetId);
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
    }

    public static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        // Leer preferencia de modo oscuro de la app
        SecurityPreferencesManager prefsManager = new SecurityPreferencesManager(context);
        int nightMode = prefsManager.getNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

        // Determinar si debemos forzar colores de modo oscuro
        boolean isDarkMode;
        if (nightMode == AppCompatDelegate.MODE_NIGHT_YES) {
            isDarkMode = true;
        } else if (nightMode == AppCompatDelegate.MODE_NIGHT_NO) {
            isDarkMode = false;
        } else {
            // Seguir sistema
            int currentNightMode = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            isDarkMode = (currentNightMode == Configuration.UI_MODE_NIGHT_YES);
        }

        // Obtener dimensiones actuales para decidir qué versión mostrar
        Bundle options = appWidgetManager.getAppWidgetOptions(appWidgetId);
        int minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
        
        // Seleccionar el layout adecuado según el ancho
        int layoutId;
        if (minWidth != 0 && minWidth < 110) {
            // 1x1: Solo el "+"
            layoutId = isDarkMode ? R.layout.widget_fast_charge_small_dark : R.layout.widget_fast_charge_small;
        } else if (minWidth != 0 && minWidth < 180) {
            // 1x2: Solo "Crear +"
            layoutId = isDarkMode ? R.layout.widget_fast_charge_medium_dark : R.layout.widget_fast_charge_medium;
        } else {
            // 1x3 o más: Logo + "Crear +"
            layoutId = isDarkMode ? R.layout.widget_fast_charge_dark : R.layout.widget_fast_charge;
        }
        
        RemoteViews views = new RemoteViews(context.getPackageName(), layoutId);

        // Lanzar FastChargeActivity al tocar
        Intent intent = new Intent(context, FastChargeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 
                appWidgetId, 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Hacer que todo el contenedor sea clickable
        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent);

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
}
