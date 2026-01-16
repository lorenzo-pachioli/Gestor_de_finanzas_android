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

    private ConfigManager(Context context) {
        this.context = context.getApplicationContext();
        this.gson = new Gson();
        loadConfigs();
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
        walletApps = loadListFromJson("config/wallet_apps.json");
        paymentKeywords = loadListFromJson("config/payment_keywords.json");
        ingresoKeywords = loadListFromJson("config/ingreso_keywords.json");
        egresoKeywords = loadListFromJson("config/egreso_keywords.json");
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
}
