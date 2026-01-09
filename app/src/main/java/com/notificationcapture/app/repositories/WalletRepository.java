package com.notificationcapture.app.repositories;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.notificationcapture.app.interfaces.GsonAccess;
import com.notificationcapture.app.models.Wallets;

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
        initWalletDefaults(context);
    }

    private void initWalletDefaults(Context context) {
        // Initialize Wallets if empty
        if (!prefs.contains(KEY_WALLETS)) {
            try {
                java.io.InputStream is = context.getResources().openRawResource(
                        context.getResources().getIdentifier("wallets", "raw", context.getPackageName()));
                java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(is));
                StringBuilder jsonBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    jsonBuilder.append(line);
                }
                is.close();

                Type type = new TypeToken<List<Wallets>>() {
                }.getType();
                List<Wallets> defaultWallets = gson.fromJson(jsonBuilder.toString(), type);

                if (defaultWallets != null) {
                    saveWallets(defaultWallets);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public List<Wallets> getAllWallets() {
        String json = prefs.getString(KEY_WALLETS, null);
        if (json == null)
            return new ArrayList<>();

        try {
            // Try to read as List<Wallets>
            Type type = new TypeToken<List<Wallets>>() {
            }.getType();
            List<Wallets> list = gson.fromJson(json, type);

            if (json.contains("\"") && !json.contains("{")) {
                // It's probably a List<String>
                Type stringType = new TypeToken<List<String>>() {
                }.getType();
                List<String> oldList = gson.fromJson(json, stringType);
                List<Wallets> migrated = new ArrayList<>();
                for (String s : oldList) {
                    migrated.add(new Wallets(s, getPackageNameFromApp(s)));
                }
                saveWallets(migrated);
                return migrated;
            }

            return list != null ? list : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public Wallets getWalletById(String id) {
        if (id == null)
            return null;
        for (Wallets w : getAllWallets()) {
            if (w.getId().equals(id))
                return w;
        }
        return null;
    }

    public void addWallet(Wallets wallet) {
        List<Wallets> wallets = getAllWallets();
        wallets.add(wallet);
        saveWallets(wallets);
    }

    public void deleteWallet(String id) {
        List<Wallets> wallets = getAllWallets();
        wallets.removeIf(w -> w.getId().equals(id));
        saveWallets(wallets);
    }

    public void updateWallet(Wallets updatedWallet) {
        List<Wallets> wallets = getAllWallets();
        for (int i = 0; i < wallets.size(); i++) {
            if (wallets.get(i).getId().equals(updatedWallet.getId())) {
                wallets.set(i, updatedWallet);
                saveWallets(wallets);
                break;
            }
        }
    }

    private void saveWallets(List<Wallets> wallets) {
        prefs.edit().putString(KEY_WALLETS, gson.toJson(wallets)).apply();
    }

    public Wallets getWalletByPackageName(String title, String packageName){
        List<Wallets> walletsList = getAllWallets();
        for(Wallets wallet : walletsList){
            if(wallet.getPackageName().equals(packageName)) return wallet;
        }
        Wallets newWallet = new Wallets(title, packageName);
        walletsList.add(newWallet);
        saveWallets(walletsList);
        return newWallet;
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
