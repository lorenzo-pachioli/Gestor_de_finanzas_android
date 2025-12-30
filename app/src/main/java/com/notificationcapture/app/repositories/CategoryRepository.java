package com.notificationcapture.app.repositories;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.notificationcapture.app.NotificationItem;
import com.notificationcapture.app.enums.TransactionType;
import com.notificationcapture.app.interfaces.GsonAccess;
import com.notificationcapture.app.models.Category;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class CategoryRepository implements GsonAccess {

    private SharedPreferences prefs;
    private Gson gson;

    public CategoryRepository(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
        initCategoryDefaults();
    }

    private void initCategoryDefaults() {
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
    }

    public List<Category> getCategories(TransactionType type) {
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
        Type type = new TypeToken<List<Category>>() {
        }.getType();
        return gson.fromJson(json, type);
    }

    private void saveCategories(List<com.notificationcapture.app.models.Category> categories) {
        prefs.edit().putString(KEY_CATEGORIES, gson.toJson(categories)).apply();
    }
}
