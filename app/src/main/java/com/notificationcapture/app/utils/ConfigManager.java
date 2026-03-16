package com.notificationcapture.app.utils;

import android.content.Context;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.notificationcapture.app.models.GlobalWallet;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigManager {
    private static ConfigManager instance;
    private final Context context;
    private final Gson gson;

    private List<String> walletApps;
    private List<String> paymentKeywords;
    private List<String> ingresoKeywords;
    private List<String> egresoKeywords;
    private List<GlobalWallet> globalWallets;
    // packageName -> GlobalWallet (un entry por cada packageName alternativo)
    private Map<String, GlobalWallet> packageToWalletMap;
    // name -> primary packageName
    private Map<String, String> nameToPackageMap;

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
            Type listType = new TypeToken<ArrayList<String>>() {}.getType();
            return gson.fromJson(json, listType);
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public List<String> getWalletApps() { return walletApps; }
    public List<String> getPaymentKeywords() { return paymentKeywords; }
    public List<String> getIngresoKeywords() { return ingresoKeywords; }
    public List<String> getEgresoKeywords() { return egresoKeywords; }
    public List<GlobalWallet> getGlobalWallets() { return globalWallets; }

    /** Busca el nombre de display para un packageName dado (cualquier alternativo). */
    public String getDefaultNameForPackage(String packageName) {
        if (packageToWalletMap != null && packageToWalletMap.containsKey(packageName)) {
            return packageToWalletMap.get(packageName).getName();
        }
        return null;
    }

    /** Devuelve el package primario (primero de la lista) dado un nombre de wallet. */
    public String getPackageByName(String name) {
        if (nameToPackageMap != null && nameToPackageMap.containsKey(name)) {
            return nameToPackageMap.get(name);
        }
        return null;
    }

    /** Verifica si un packageName es reconocido por alguna wallet global. */
    public boolean isGlobalWalletPackage(String packageName) {
        return packageToWalletMap != null && packageToWalletMap.containsKey(packageName);
    }

    private void loadGlobalWallets() {
        try {
            int resId = context.getResources().getIdentifier("wallets_global", "raw", context.getPackageName());
            if (resId == 0) {
                globalWallets = new ArrayList<>();
                packageToWalletMap = new HashMap<>();
                nameToPackageMap = new HashMap<>();
                return;
            }
            InputStream is = context.getResources().openRawResource(resId);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String json = new String(buffer, StandardCharsets.UTF_8);
            Type listType = new TypeToken<ArrayList<GlobalWallet>>() {}.getType();
            globalWallets = gson.fromJson(json, listType);

            packageToWalletMap = new HashMap<>();
            nameToPackageMap = new HashMap<>();
            if (globalWallets != null) {
                for (GlobalWallet wallet : globalWallets) {
                    // Indexar todos los packageNames alternativos
                    for (String pkg : wallet.getPackageNames()) {
                        packageToWalletMap.put(pkg, wallet);
                    }
                    nameToPackageMap.put(wallet.getName(), wallet.getPrimaryPackageName());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            globalWallets = new ArrayList<>();
            packageToWalletMap = new HashMap<>();
            nameToPackageMap = new HashMap<>();
        }
    }
}
