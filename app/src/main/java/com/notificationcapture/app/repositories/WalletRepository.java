package com.notificationcapture.app.repositories;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.notificationcapture.app.interfaces.GsonAccess;
import com.notificationcapture.app.models.Wallets;
import com.notificationcapture.app.utils.AppLogger;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WalletRepository implements GsonAccess {

    private SharedPreferences prefs;
    private Gson gson;
    private List<Wallets> cachedWallets = null; // Memory Cache

    public WalletRepository(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
        initWalletDefaults(context);
    }

    private void initWalletDefaults(Context context) {
        // Initialize Wallets only if the preference doesn't exist yet
        if (!prefs.contains(KEY_WALLETS)) {
            try {
                // Try to get resource ID directly or via identifier
                int resId = context.getResources().getIdentifier("wallets", "raw", context.getPackageName());

                if (resId == 0) {
                    // Fallback to direct reference if identifier fails
                    // This assumes the R class is accessible, which it might not be in a generic
                    // repo if not imported
                    // But we can try to find it via reflection or just log the failure.
                    AppLogger.e("WalletRepository", "Could not find raw resource 'wallets'");
                    return;
                }

                java.io.InputStream is = context.getResources().openRawResource(resId);
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

                if (defaultWallets != null && !defaultWallets.isEmpty()) {
                    saveWallets(defaultWallets);
                    AppLogger.d("WalletRepository", "Loaded " + defaultWallets.size() + " default wallets.");
                }
            } catch (Exception e) {
                AppLogger.e("WalletRepository", "Error loading default wallets", e);
            }
        }
    }

    public List<Wallets> getAllWallets() {
        if (cachedWallets != null) {
            return new ArrayList<>(cachedWallets); // Devuelve copia rápida desde RAM O(1)
        }
        
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

            cachedWallets = list != null ? list : new ArrayList<>();
            return new ArrayList<>(cachedWallets);
        } catch (Exception e) {
            AppLogger.e("WalletRepository", "Error getting all wallets", e);
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
        java.util.Iterator<Wallets> iterator = wallets.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getId().equals(id)) {
                iterator.remove();
            }
        }
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
        cachedWallets = new ArrayList<>(wallets); // Sincroniza caché en memoria
        prefs.edit().putString(KEY_WALLETS, gson.toJson(wallets)).apply();
    }

    public Wallets getWalletByPackageName(String title, String packageName) {
        if (packageName == null) return null;
        
        List<Wallets> walletsList = getAllWallets();
        // 1. Check if user already has an exact match
        for (Wallets wallet : walletsList) {
            String pName = wallet.getPackageName();
            if (pName != null && pName.equals(packageName)) {
                return wallet;
            }
        }
        
        // 2. Check if it matches any global wallet (by exact or substring)
        com.notificationcapture.app.utils.ConfigManager config = com.notificationcapture.app.utils.ConfigManager.getInstance();
        for (com.notificationcapture.app.models.GlobalWallet gw : config.getGlobalWallets()) {
            boolean matches = false;
            if (gw.matchesPackage(packageName)) {
                matches = true;
            } else {
                for (String pkg : gw.getPackageNames()) {
                    // Prevenir matches demasiado genéricos con substrings muy cortos o vacíos
                    if (pkg.length() > 2 && packageName.toLowerCase().contains(pkg.toLowerCase())) {
                        matches = true;
                        break;
                    }
                }
            }

            if (matches) {
                // Verificar si el usuario ya la tiene con su nombre primario
                for (Wallets wallet : walletsList) {
                    if (wallet.getPackageName().equals(gw.getPrimaryPackageName())) {
                        return wallet;
                    }
                }
                // Si no, autogenerar la wallet oficial
                Wallets newGlobalWallet = gw.toUserWallet();
                walletsList.add(newGlobalWallet);
                saveWallets(walletsList);
                return newGlobalWallet;
            }
        }

        // 3. Unknown app, NO CREAR NUEVA BILLETERA. 
        // Retornar null para que asigne walletId="1" y ponga "Desconocido".
        return null;
    }

    public static String getPackageNameFromApp(String appName) {
        String globalPackage = com.notificationcapture.app.utils.ConfigManager.getInstance().getPackageByName(appName);
        if (globalPackage != null) {
            return globalPackage;
        }
        return "com.wallet.custom";
    }
}
