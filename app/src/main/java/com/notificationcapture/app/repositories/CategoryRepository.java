package com.notificationcapture.app.repositories;

import static android.content.ContentValues.TAG;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import com.notificationcapture.app.utils.AppLogger;

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
    private List<Category> cachedCategories = null; // Memory Cache

    public static final String OTHER_INCOME_ID = "other_income";
    public static final String OTHER_OUTCOME_ID = "other_outcome";

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
                if (name.equals("Otros")) {
                    defaultCategories.add(new Category(OTHER_INCOME_ID, name, IngresoOEgreso.INGRESO));
                } else {
                    defaultCategories.add(new Category(name, IngresoOEgreso.INGRESO));
                }
            }
            // Outcome Defaults
            for (String name : OUTCOME_CATEGORIES) {
                if (name.equals("Otros")) {
                    defaultCategories.add(new Category(OTHER_OUTCOME_ID, name, IngresoOEgreso.EGRESO));
                } else {
                    defaultCategories.add(new Category(name, IngresoOEgreso.EGRESO));
                }
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
        if (category.getId() == null) {
            category.setId(java.util.UUID.randomUUID().toString());
        }
        List<Category> all = getAllCategories();
        all.add(category);
        saveCategories(all);
    }

    public void deleteCategory(String id) {
        List<Category> all = getAllCategories();
        List<Category> toKeep = new ArrayList<>();
        for (Category c : all) {
            if (!c.getId().equals(id)) {
                toKeep.add(c);
            }
        }
        saveCategories(toKeep);
    }

    public Category getCategoryById(String id) {
        if (id == null)
            return null;
        List<Category> all = getAllCategories();
        for (Category c : all) {
            if (c.getId().equals(id)) {
                return c;
            }
        }
        // Fallback para migraciones de objetos antiguos y IDs por defecto cruzados
        if (OTHER_INCOME_ID.equals(id) || "Otros_INGRESO".equals(id)) {
            for (Category c : all) {
                if ("Otros".equalsIgnoreCase(c.getName()) && c.getType() == IngresoOEgreso.INGRESO) return c;
            }
        } else if (OTHER_OUTCOME_ID.equals(id) || "Otros_EGRESO".equals(id)) {
            for (Category c : all) {
                if ("Otros".equalsIgnoreCase(c.getName()) && c.getType() == IngresoOEgreso.EGRESO) return c;
            }
        }
        return null;
    }

    public void updateCategory(Category newCat) {
        List<Category> all = getAllCategories();
        for (int i = 0; i < all.size(); i++) {
            Category c = all.get(i);
            if (c.getId().equals(newCat.getId())) {
                all.set(i, newCat);
                break;
            }
        }
        saveCategories(all);
    }

    public List<Category> getAllCategories() {
        if (cachedCategories != null) {
            return new ArrayList<>(cachedCategories); // Devuelve copia rápida desde RAM O(1)
        }
        
        try {
            String json = prefs.getString(KEY_CATEGORIES, null);
            if (json == null)
                return new ArrayList<>();
            Type type = new TypeToken<List<Category>>() {
            }.getType();
            List<Category> cats = gson.fromJson(json, type);

            // Migración: asegurar que todos tengan ID
            boolean needsSave = false;
            if (cats != null) {
                for (Category c : cats) {
                    if (c.getId() == null) {
                        c.setId(c.getName() + "_" + c.getType());
                        needsSave = true;
                    }
                }
                if (needsSave) {
                    saveCategories(cats);
                }
            } else {
                return new ArrayList<>();
            }

            cachedCategories = cats;
            return new ArrayList<>(cachedCategories);
        } catch (Exception e) {
            AppLogger.e("CategoryRepository", "Error getting categories: " + e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    private void saveCategories(List<Category> categories) {
        try {
            cachedCategories = new ArrayList<>(categories); // Sincroniza caché en memoria
            prefs.edit().putString(KEY_CATEGORIES, gson.toJson(categories)).apply();
        } catch (Exception e) {
            AppLogger.e("CategoryRepository", "Error saving categories: " + e.getMessage(), e);
        }
    }
}
