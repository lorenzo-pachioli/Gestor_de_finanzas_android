package com.notificationcapture.app.repositories;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import com.notificationcapture.app.NotificationItem;
import com.notificationcapture.app.interfaces.GsonAccess;

public class TransactionRepository implements GsonAccess {

    private SharedPreferences prefs;
    private Gson gson;

    public TransactionRepository(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    public void saveTransactionNotFiltered(NotificationItem item) {
        List<NotificationItem> notifications = getAllTransactions();

        // Agregar al inicio de la lista
        notifications.add(0, item);

        // Limitar el número de notificaciones guardadas
        if (notifications.size() > MAX_NOTIFICATIONS) {
            notifications = notifications.subList(0, MAX_NOTIFICATIONS);
        }

        // Guardar en SharedPreferences
        String json = gson.toJson(notifications);
        prefs.edit().putString(KEY_NOTIFICATIONS_NOT_FILTERED, json).apply();
    }

    public void saveTransaction(NotificationItem item) {
        List<NotificationItem> notifications = getAllTransactions();

        // Agregar al inicio de la lista
        notifications.add(0, item);

        // Limitar el número de notificaciones guardadas
        if (notifications.size() > MAX_NOTIFICATIONS) {
            notifications = notifications.subList(0, MAX_NOTIFICATIONS);
        }

        // Guardar en SharedPreferences
        String json = gson.toJson(notifications);
        prefs.edit().putString(KEY_NOTIFICATIONS, json).apply();
    }

    public List<NotificationItem> getAllTransactionNotFiltered() {
        String json = prefs.getString(KEY_NOTIFICATIONS_NOT_FILTERED, null);

        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }

        Type type = new TypeToken<List<NotificationItem>>() {
        }.getType();
        List<NotificationItem> notifications = gson.fromJson(json, type);

        return notifications != null ? notifications : new ArrayList<>();
    }

    public List<NotificationItem> getAllTransactions() {
        String json = prefs.getString(KEY_NOTIFICATIONS, null);

        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }

        Type type = new TypeToken<List<NotificationItem>>() {
        }.getType();
        List<NotificationItem> notifications = gson.fromJson(json, type);

        return notifications != null ? notifications : new ArrayList<>();
    }

    public void deleteTransaction(String id) {
        List<NotificationItem> notifications = getAllTransactions();

        // Find the notification to be deleted first to check for group ID
        String groupId = null;
        for (NotificationItem item : notifications) {
            if (item.getId().equals(id)) {
                groupId = item.getInstallmentGroupId();
                break;
            }
        }

        // Filtrar removiendo la notificación con el ID especificado o todas las del
        // grupo
        List<NotificationItem> updatedList = new ArrayList<>();
        for (NotificationItem item : notifications) {
            boolean shouldDelete = false;

            if (groupId != null && item.getInstallmentGroupId() != null) {
                // If part of the same group, delete it
                if (groupId.equals(item.getInstallmentGroupId())) {
                    shouldDelete = true;
                }
            } else if (item.getId().equals(id)) {
                // Standard single deletion
                shouldDelete = true;
            }

            if (!shouldDelete) {
                updatedList.add(item);
            }
        }

        // Guardar la lista actualizada
        String json = gson.toJson(updatedList);
        prefs.edit().putString(KEY_NOTIFICATIONS, json).apply();
    }

    public void deleteTransactionNotFiltered(String id) {
        List<NotificationItem> notifications = getAllTransactions();

        // Find the notification to be deleted first to check for group ID
        String groupId = null;
        for (NotificationItem item : notifications) {
            if (item.getId().equals(id)) {
                groupId = item.getInstallmentGroupId();
                break;
            }
        }

        // Filtrar removiendo la notificación con el ID especificado o todas las del
        // grupo
        List<NotificationItem> updatedList = new ArrayList<>();
        for (NotificationItem item : notifications) {
            boolean shouldDelete = false;

            if (groupId != null && item.getInstallmentGroupId() != null) {
                if (groupId.equals(item.getInstallmentGroupId())) {
                    shouldDelete = true;
                }
            } else if (item.getId().equals(id)) {
                shouldDelete = true;
            }

            if (!shouldDelete) {
                updatedList.add(item);
            }
        }

        // Guardar la lista actualizada
        String json = gson.toJson(updatedList);
        prefs.edit().putString(KEY_NOTIFICATIONS_NOT_FILTERED, json).apply();
    }

    public void updateTransaction(NotificationItem updatedItem) {
        List<NotificationItem> notifications = getAllTransactions();

        boolean found = false;
        for (int i = 0; i < notifications.size(); i++) {
            if (notifications.get(i).getId().equals(updatedItem.getId())) {
                notifications.set(i, updatedItem);
                found = true;
                break;
            }
        }

        if (found) {
            String json = gson.toJson(notifications);
            prefs.edit().putString(KEY_NOTIFICATIONS, json).apply();
        }
    }

    public void clearAllTransaction() {
        prefs.edit().remove(KEY_NOTIFICATIONS).apply();
    }

}