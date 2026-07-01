package com.notificationcapture.app.repositories;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.notificationcapture.app.constants.CategoryConstants;
import com.notificationcapture.app.constants.PrefsConstants;
import com.notificationcapture.app.enums.IngresoOEgreso;
import com.notificationcapture.app.interfaces.GsonAccess;
import com.notificationcapture.app.models.Category;
import com.notificationcapture.app.utils.AppLogger;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class CategoryRepository implements GsonAccess {

    private SharedPreferences prefs;
    private Gson gson;
    private List<Category> cachedCategories = null;

    public CategoryRepository(Context context) {
        prefs = context.getSharedPreferences(PrefsConstants.PREFS_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
        initCategoryDefaults();
    }

    private void initCategoryDefaults() {
        if (!prefs.contains(KEY_CATEGORIES)) {
            List<Category> defaultCategories = new ArrayList<>();
            for (String name : CategoryConstants.DEFAULT_INCOME_CATEGORIES) {
                if (CategoryConstants.OTHER_DISPLAY_NAME.equals(name)) {
                    defaultCategories.add(new Category(CategoryConstants.OTHER_INCOME_ID, name, IngresoOEgreso.INGRESO));
                } else {
                    defaultCategories.add(new Category(name, IngresoOEgreso.INGRESO));
                }
            }
            defaultCategories.add(new Category(CategoryConstants.TRANSFER_IN_ID, "Transferencia entrante", IngresoOEgreso.INGRESO));

            for (String name : CategoryConstants.DEFAULT_OUTCOME_CATEGORIES) {
                if (CategoryConstants.OTHER_DISPLAY_NAME.equals(name)) {
                    defaultCategories.add(new Category(CategoryConstants.OTHER_OUTCOME_ID, name, IngresoOEgreso.EGRESO));
                } else if ("Pago de Tarjeta".equals(name)) {
                    defaultCategories.add(new Category(CategoryConstants.PAGO_TARJETA_ID, name, IngresoOEgreso.EGRESO));
                } else {
                    defaultCategories.add(new Category(name, IngresoOEgreso.EGRESO));
                }
            }
            defaultCategories.add(new Category(CategoryConstants.TRANSFER_OUT_ID, "Transferencia saliente", IngresoOEgreso.EGRESO));
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
        if (id == null) {
            return null;
        }
        List<Category> all = getAllCategories();
        for (Category c : all) {
            if (c.getId().equals(id)) {
                return c;
            }
        }
        if ("other".equals(id)) {
            for (Category c : all) {
                if (CategoryConstants.OTHER_DISPLAY_NAME.equalsIgnoreCase(c.getName()) && c.getType() == IngresoOEgreso.INGRESO) {
                    return c;
                }
            }
        } else if (CategoryConstants.OTHER_INCOME_ID.equals(id) || "Otros_INGRESO".equals(id)) {
            for (Category c : all) {
                if (CategoryConstants.OTHER_DISPLAY_NAME.equalsIgnoreCase(c.getName()) && c.getType() == IngresoOEgreso.INGRESO) {
                    return c;
                }
            }
        } else if (CategoryConstants.OTHER_OUTCOME_ID.equals(id) || "Otros_EGRESO".equals(id)) {
            for (Category c : all) {
                if (CategoryConstants.OTHER_DISPLAY_NAME.equalsIgnoreCase(c.getName()) && c.getType() == IngresoOEgreso.EGRESO) {
                    return c;
                }
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
            return new ArrayList<>(cachedCategories);
        }

        try {
            String json = prefs.getString(KEY_CATEGORIES, null);
            if (json == null) {
                return new ArrayList<>();
            }
            Type type = new TypeToken<List<Category>>() {}.getType();
            List<Category> cats = gson.fromJson(json, type);

            boolean needsSave = false;
            if (cats != null) {
                boolean hasPagoTarjeta = false;
                for (Category c : cats) {
                    if (c.getId() == null) {
                        c.setId(c.getName() + "_" + c.getType());
                        needsSave = true;
                    }
                    if ("other".equals(c.getId())) {
                        String correctId = c.getType() == IngresoOEgreso.INGRESO
                                ? CategoryConstants.OTHER_INCOME_ID
                                : CategoryConstants.OTHER_OUTCOME_ID;
                        c.setId(correctId);
                        needsSave = true;
                    }
                    if (CategoryConstants.OTHER_INCOME_ID.equals(c.getId()) || "Otros_INGRESO".equals(c.getId())) {
                        c.setId(CategoryConstants.OTHER_INCOME_ID);
                        needsSave = true;
                    } else if (CategoryConstants.OTHER_OUTCOME_ID.equals(c.getId()) || "Otros_EGRESO".equals(c.getId())) {
                        c.setId(CategoryConstants.OTHER_OUTCOME_ID);
                        needsSave = true;
                    }
                    if (CategoryConstants.PAGO_TARJETA_ID.equals(c.getId())) {
                        hasPagoTarjeta = true;
                    }
                }

                if (!hasPagoTarjeta) {
                    cats.add(new Category(CategoryConstants.PAGO_TARJETA_ID, "Pago de Tarjeta", IngresoOEgreso.EGRESO));
                    needsSave = true;
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
            cachedCategories = new ArrayList<>(categories);
            prefs.edit().putString(KEY_CATEGORIES, gson.toJson(categories)).apply();
        } catch (Exception e) {
            AppLogger.e("CategoryRepository", "Error saving categories: " + e.getMessage(), e);
        }
    }
}
