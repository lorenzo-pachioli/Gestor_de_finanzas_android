package com.notificationcapture.app;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Collections;
import java.util.List;
import com.notificationcapture.app.enums.TransactionType;

public class NotificationRepository {

    private static final String PREFS_NAME = "NotificationPrefs";
    private static final String KEY_NOTIFICATIONS = "notifications";
    private static final String KEY_NOTIFICATIONS_NOT_FILTERED = "notificationsNotFiltered";
    private static final String KEY_CATEGORIES = "categories";
    private static final String KEY_WALLETS = "wallets";
    private static final String KEY_CREDIT_CARDS = "credit_cards";
    private static final int MAX_NOTIFICATIONS = 100;

    private SharedPreferences prefs;
    private Gson gson;

    public NotificationRepository(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
        initDefaults();
    }

    private void initDefaults() {
        // Initialize Categories if empty
        if (!prefs.contains(KEY_CATEGORIES)) {
            List<com.notificationcapture.app.models.Category> defaultCategories = new ArrayList<>();
            // Income Defaults
            for (String name : NotificationItem.INCOME_CATEGORIES) {
                defaultCategories.add(new com.notificationcapture.app.models.Category(name,
                        android.graphics.Color.parseColor("#4CAF50"), TransactionType.INGRESO));
            }
            // Outcome Defaults
            for (String name : NotificationItem.OUTCOME_CATEGORIES) {
                defaultCategories.add(new com.notificationcapture.app.models.Category(name,
                        android.graphics.Color.parseColor("#eb1005"), TransactionType.EGRESO));
            }
            saveCategories(defaultCategories);
        }

        // Initialize Wallets if empty
        if (!prefs.contains(KEY_WALLETS)) {
            List<String> defaultWallets = new ArrayList<>();
            // Assuming walletApps array is available or copy here. Since it's local in
            // Fragment, copying common ones.
            String[] commonWallets = {
                    "Mercado Pago", "Ualá", "Brubank", "Naranja X", "Modo", "Personal Pay",
                    "Santander Río", "BBVA", "Galicia", "Macro", "Banco Nación", "Efectivo"
            };
            Collections.addAll(defaultWallets, commonWallets);
            saveWallets(defaultWallets);
        }
    }

    public void saveNotificationNotFiltered(NotificationItem item) {
        List<NotificationItem> notifications = getAllNotifications();

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

    public void saveNotification(NotificationItem item) {
        List<NotificationItem> notifications = getAllNotifications();

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

    public List<NotificationItem> getAllNotificationsNotFiltered() {
        String json = prefs.getString(KEY_NOTIFICATIONS_NOT_FILTERED, null);

        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }

        Type type = new TypeToken<List<NotificationItem>>() {
        }.getType();
        List<NotificationItem> notifications = gson.fromJson(json, type);

        return notifications != null ? notifications : new ArrayList<>();
    }

    public List<NotificationItem> getAllNotifications() {
        String json = prefs.getString(KEY_NOTIFICATIONS, null);

        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }

        Type type = new TypeToken<List<NotificationItem>>() {
        }.getType();
        List<NotificationItem> notifications = gson.fromJson(json, type);

        return notifications != null ? notifications : new ArrayList<>();
    }

    public void deleteNotification(String id) {
        List<NotificationItem> notifications = getAllNotifications();

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

    public void deleteNotificationNotFiltered(String id) {
        List<NotificationItem> notifications = getAllNotifications();

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

    public void updateNotification(NotificationItem updatedItem) {
        List<NotificationItem> notifications = getAllNotifications();

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

    public void clearAllNotifications() {
        prefs.edit().remove(KEY_NOTIFICATIONS).apply();
    }

    // --- Category Management ---

    public List<com.notificationcapture.app.models.Category> getCategories(TransactionType type) {
        List<com.notificationcapture.app.models.Category> all = getAllCategories();
        List<com.notificationcapture.app.models.Category> filtered = new ArrayList<>();
        for (com.notificationcapture.app.models.Category c : all) {
            if (c.getType() == type) {
                filtered.add(c);
            }
        }
        return filtered;
    }

    public List<String> getCategoryNames(TransactionType type) {
        List<com.notificationcapture.app.models.Category> cats = getCategories(type);
        List<String> names = new ArrayList<>();
        for (com.notificationcapture.app.models.Category c : cats) {
            names.add(c.getName());
        }
        return names;
    }

    public void addCategory(com.notificationcapture.app.models.Category category) {
        List<com.notificationcapture.app.models.Category> all = getAllCategories();
        all.add(category);
        saveCategories(all);
    }

    public void deleteCategory(String name, TransactionType type) {
        List<com.notificationcapture.app.models.Category> all = getAllCategories();
        List<com.notificationcapture.app.models.Category> toKeep = new ArrayList<>();
        for (com.notificationcapture.app.models.Category c : all) {
            if (!(c.getName().equals(name) && c.getType() == type)) {
                toKeep.add(c);
            }
        }
        saveCategories(toKeep);
    }

    public void updateCategory(com.notificationcapture.app.models.Category oldCat,
            com.notificationcapture.app.models.Category newCat) {
        List<com.notificationcapture.app.models.Category> all = getAllCategories();
        for (int i = 0; i < all.size(); i++) {
            com.notificationcapture.app.models.Category c = all.get(i);
            if (c.getName().equals(oldCat.getName()) && c.getType() == oldCat.getType()) {
                all.set(i, newCat);
                break;
            }
        }
        saveCategories(all);
    }

    public List<com.notificationcapture.app.models.Category> getAllCategories() {
        String json = prefs.getString(KEY_CATEGORIES, null);
        if (json == null)
            return new ArrayList<>();
        Type type = new TypeToken<List<com.notificationcapture.app.models.Category>>() {
        }.getType();
        return gson.fromJson(json, type);
    }

    private void saveCategories(List<com.notificationcapture.app.models.Category> categories) {
        prefs.edit().putString(KEY_CATEGORIES, gson.toJson(categories)).apply();
    }

    // --- Wallet Management ---

    public List<String> getWallets() {
        String json = prefs.getString(KEY_WALLETS, null);
        if (json == null)
            return new ArrayList<>();
        Type type = new TypeToken<List<String>>() {
        }.getType();
        return gson.fromJson(json, type);
    }

    public void addWallet(String name) {
        List<String> wallets = getWallets();
        if (!wallets.contains(name)) {
            wallets.add(name);
            saveWallets(wallets);
        }
    }

    public void deleteWallet(String name) {
        List<String> wallets = getWallets();
        wallets.remove(name);
        saveWallets(wallets);
    }

    public void updateWallet(String oldName, String newName) {
        List<String> wallets = getWallets();
        if (!wallets.contains(newName)) {
            int index = wallets.indexOf(oldName);
            if (index != -1) {
                wallets.set(index, newName);
                saveWallets(wallets);
            }
        }
    }

    private void saveWallets(List<String> wallets) {
        prefs.edit().putString(KEY_WALLETS, gson.toJson(wallets)).apply();
    }

    // --- Credit Card Management ---

    public List<com.notificationcapture.app.models.CreditCard> getCreditCards() {
        String json = prefs.getString(KEY_CREDIT_CARDS, null);
        if (json == null)
            return new ArrayList<>();
        Type type = new TypeToken<List<com.notificationcapture.app.models.CreditCard>>() {
        }.getType();
        return gson.fromJson(json, type);
    }

    public void addCreditCard(com.notificationcapture.app.models.CreditCard card) {
        List<com.notificationcapture.app.models.CreditCard> cards = getCreditCards();
        cards.add(card);
        saveCreditCards(cards);
    }

    public void deleteCreditCard(String id) {
        List<com.notificationcapture.app.models.CreditCard> cards = getCreditCards();
        for (int i = 0; i < cards.size(); i++) {
            if (cards.get(i).getId().equals(id)) {
                cards.remove(i);
                break;
            }
        }
        saveCreditCards(cards);
    }

    public void updateCreditCard(com.notificationcapture.app.models.CreditCard updatedCard) {
        List<com.notificationcapture.app.models.CreditCard> cards = getCreditCards();
        for (int i = 0; i < cards.size(); i++) {
            if (cards.get(i).getId().equals(updatedCard.getId())) {
                cards.set(i, updatedCard);
                break;
            }
        }
        saveCreditCards(cards);
    }

    private void saveCreditCards(List<com.notificationcapture.app.models.CreditCard> cards) {
        prefs.edit().putString(KEY_CREDIT_CARDS, gson.toJson(cards)).apply();
    }

    public String getPackageNameFromApp(String appName) {
        switch (appName) {
            case "Mercado Pago":
                return "com.mercadopago.wallet";
            case "Ualá":
                return "com.uala.app";
            case "Brubank":
                return "brubank.app";
            case "Naranja X":
                return "com.naranja.app";
            case "Modo":
                return "com.reba.contactless";
            case "Personal Pay":
                return "personal.pay";
            case "Bimo":
                return "bimo.app";
            case "BIND":
                return "ar.com.bind";
            case "Prex":
                return "ar.com.prex";
            case "Wilobank":
                return "ar.wilobank";
            case "Santander Río":
                return "ar.com.santander.rio";
            case "BBVA":
                return "com.bbva.nxt_argentina";
            case "Galicia":
                return "ar.com.bancogalicia";
            case "Macro":
                return "com.macro";
            case "Banco Nación":
                return "ar.com.bna";
            case "Mi Argentina":
                return "ar.gov.anses.mi";
            case "Claro Pay":
                return "com.claro.pay";
            default:
                return "com.wallet.custom";
        }
    }
}