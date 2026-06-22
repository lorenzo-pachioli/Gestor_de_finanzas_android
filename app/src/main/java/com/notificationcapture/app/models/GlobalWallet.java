package com.notificationcapture.app.models;

import com.notificationcapture.app.interfaces.SpinnerDisplayable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class GlobalWallet implements Serializable, SpinnerDisplayable {

    private String id;
    private String name;
    private List<String> packageNames;

    public GlobalWallet(String id, String name, List<String> packageNames) {
        this.id = id;
        this.name = name;
        this.packageNames = packageNames != null ? packageNames : new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<String> getPackageNames() {
        return packageNames != null ? packageNames : new ArrayList<>();
    }

    /** Devuelve el primer packageName como representante canónico (para guardar en wallets del usuario). */
    public String getPrimaryPackageName() {
        if (packageNames != null && !packageNames.isEmpty()) {
            return packageNames.get(0);
        }
        return "";
    }

    /** Verifica si un packageName dado es reconocido por esta wallet global. */
    public boolean matchesPackage(String packageName) {
        if (packageName == null || packageNames == null) return false;
        for (String pkg : packageNames) {
            if (pkg.equalsIgnoreCase(packageName)) return true;
        }
        return false;
    }

    @Override
    public String getDisplayName() {
        return name;
    }

    @Override
    public Integer getDisplayColor() {
        return 0;
    }

    /** Convierte a Wallets (modelo del usuario) usando el package primario. */
    public Wallets toUserWallet() {
        return new Wallets(id, name, getPrimaryPackageName());
    }
}
