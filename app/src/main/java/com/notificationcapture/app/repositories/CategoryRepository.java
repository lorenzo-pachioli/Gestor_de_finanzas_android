package com.notificationcapture.app.repositories;

import static android.content.ContentValues.TAG;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.notificationcapture.app.enums.IngresoOEgreso;
import com.notificationcapture.app.interfaces.GsonAccess;
import com.notificationcapture.app.models.Category;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class CategoryRepository implements GsonAccess {

    private SharedPreferences prefs;
    private Gson gson;

    private static final String[] OUTCOME_CATEGORIES = {
            "Otros", "Comida", "Combustible", "Transporte", "Servicios",
            "Entretenimiento", "Salud", "Educación", "Compras", "Vivienda"
    };

    private static final String[] INCOME_CATEGORIES = {
            "Otros", "Salario", "Inversiones", "Reembolsos", "Familiares", "Venta"
    };

    public CategoryRepository(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
        initCategoryDefaults();
    }

    private void initCategoryDefaults() {
        // Initialize Categories if empty
        if (!prefs.contains(KEY_CATEGORIES)) {
            List<Category> defaultCategories = new ArrayList<>();
            // Income Defaults
            for (String name : INCOME_CATEGORIES) {
                defaultCategories.add(new Category(name, IngresoOEgreso.INGRESO));
            }
            // Outcome Defaults
            for (String name : OUTCOME_CATEGORIES) {
                defaultCategories.add(new Category(name, IngresoOEgreso.EGRESO));
            }
            saveCategories(defaultCategories);
        }
    }

    public List<Category> getCategories(IngresoOEgreso type) {
        List<Category> all = getAllCategories();
        List<Category> filtered = new ArrayList<>();
        for (Category c : all) {
            if (c.getType() == type) {
                filtered.add(c);
            }
        }
        return filtered;
    }

    public List<String> getCategoryNames(IngresoOEgreso type) {
        List<Category> cats = getCategories(type);
        List<String> names = new ArrayList<>();
        for (Category c : cats) {
            names.add(c.getName());
        }
        return names;
    }

    public void addCategory(Category category) {
        List<Category> all = getAllCategories();
        all.add(category);
        saveCategories(all);
    }

    public void deleteCategory(String name, IngresoOEgreso type) {
        List<Category> all = getAllCategories();
        List<Category> toKeep = new ArrayList<>();
        for (Category c : all) {
            if (!(c.getName().equals(name) && c.getType() == type)) {
                toKeep.add(c);
            }
        }
        saveCategories(toKeep);
    }

    public void updateCategory(Category oldCat, Category newCat) {

        List<Category> all = getAllCategories();
        for (int i = 0; i < all.size(); i++) {
            Category c = all.get(i);
            if (c.getName().equals(oldCat.getName()) && c.getType() == oldCat.getType()) {
                all.set(i, newCat);
                break;
            }
        }
        saveCategories(all);
    }

    public List<Category> getAllCategories() {
        try {
            String json = prefs.getString(KEY_CATEGORIES, null);
            if (json == null)
                return new ArrayList<>();
            Type type = new TypeToken<List<Category>>() {
            }.getType();
            return gson.fromJson(json, type);
        } catch (Exception e) {
            Log.e(TAG, "Error adding category: " + e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    private void saveCategories(List<Category> categories) {
        try {
            prefs.edit().putString(KEY_CATEGORIES, gson.toJson(categories)).apply();
        } catch (Exception e) {
            Log.e(TAG, "Error adding category: " + e.getMessage(), e);
        }
    }
}
