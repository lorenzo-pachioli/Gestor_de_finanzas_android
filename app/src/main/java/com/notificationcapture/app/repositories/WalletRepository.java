package com.notificationcapture.app.repositories;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.notificationcapture.app.interfaces.GsonAccess;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WalletRepository implements GsonAccess {

    private SharedPreferences prefs;
    private Gson gson;

    public WalletRepository(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
        initWalletDefaults();
    }

    private void initWalletDefaults() {

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

    public static String getPackageNameFromApp(String appName) {
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
