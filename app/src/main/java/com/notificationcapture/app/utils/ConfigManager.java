package com.notificationcapture.app.utils;

import android.content.Context;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ConfigManager {
    private static ConfigManager instance;
    private final Context context;
    private final Gson gson;

    private List<String> walletApps;
    private List<String> paymentKeywords;
    private List<String> ingresoKeywords;
    private List<String> egresoKeywords;
    private List<com.notificationcapture.app.models.Wallets> globalWallets;
    private java.util.Map<String, String> packageToNameMap;
    private java.util.Map<String, String> nameToPackageMap;

    private ConfigManager(Context context) {
        this.context = context.getApplicationContext();
        this.gson = new Gson();
        loadConfigs();
        loadGlobalWallets();
    }

    public static synchronized void init(Context context) {
        if (instance == null) {
            instance = new ConfigManager(context);
        }
    }

    public static synchronized ConfigManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("ConfigManager must be initialized before use.");
        }
        return instance;
    }

    private void loadConfigs() {
        walletApps = loadAndMerge("wallet_apps.json");
        paymentKeywords = loadAndMerge("payment_keywords.json");
        ingresoKeywords = loadAndMerge("ingreso_keywords.json");
        egresoKeywords = loadAndMerge("egreso_keywords.json");
    }

    private List<String> loadAndMerge(String fileName) {
        List<String> combinedList = new ArrayList<>();
        combinedList.addAll(loadListFromJson("config/es/" + fileName));
        combinedList.addAll(loadListFromJson("config/en/" + fileName));
        return combinedList;
    }

    private List<String> loadListFromJson(String fileName) {
        try {
            InputStream is = context.getAssets().open(fileName);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String json = new String(buffer, StandardCharsets.UTF_8);
            Type listType = new TypeToken<ArrayList<String>>() {
            }.getType();
            return gson.fromJson(json, listType);
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public List<String> getWalletApps() {
        return walletApps;
    }

    public List<String> getPaymentKeywords() {
        return paymentKeywords;
    }

    public List<String> getIngresoKeywords() {
        return ingresoKeywords;
    }

    public List<String> getEgresoKeywords() {
        return egresoKeywords;
    }

    public List<com.notificationcapture.app.models.Wallets> getGlobalWallets() {
        return globalWallets;
    }

    public String getDefaultNameForPackage(String packageName) {
        if (packageToNameMap != null && packageToNameMap.containsKey(packageName)) {
            return packageToNameMap.get(packageName);
        }
        return null;
    }

    public String getPackageByName(String name) {
        if (nameToPackageMap != null && nameToPackageMap.containsKey(name)) {
            return nameToPackageMap.get(name);
        }
        return null;
    }

    private void loadGlobalWallets() {
        try {
            int resId = context.getResources().getIdentifier("wallets_global", "raw", context.getPackageName());
            if (resId == 0) {
                globalWallets = new java.util.ArrayList<>();
                packageToNameMap = new java.util.HashMap<>();
                return;
            }
            InputStream is = context.getResources().openRawResource(resId);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String json = new String(buffer, StandardCharsets.UTF_8);
            Type listType = new TypeToken<ArrayList<com.notificationcapture.app.models.Wallets>>() {
            }.getType();
            globalWallets = gson.fromJson(json, listType);

            // Populate Maps
            packageToNameMap = new java.util.HashMap<>();
            nameToPackageMap = new java.util.HashMap<>();
            if (globalWallets != null) {
                for (com.notificationcapture.app.models.Wallets wallet : globalWallets) {
                    packageToNameMap.put(wallet.getPackageName(), wallet.getName());
                    nameToPackageMap.put(wallet.getName(), wallet.getPackageName());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            globalWallets = new java.util.ArrayList<>();
            packageToNameMap = new java.util.HashMap<>();
            nameToPackageMap = new java.util.HashMap<>();
        }
    }
}
