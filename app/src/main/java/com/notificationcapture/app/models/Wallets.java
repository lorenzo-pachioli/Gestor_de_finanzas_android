package com.notificationcapture.app.models;

import com.notificationcapture.app.interfaces.SpinnerDisplayable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class Wallets implements Serializable, SpinnerDisplayable {

    private String id;
    private String name;
    private String packageName;

    public Wallets(String name, String packageName) {
        this.id = generateId();
        this.name = name;
        this.packageName = packageName;
    }

    public Wallets(String id, String name, String packageName) {
        this.id = id;
        this.name = name;
        this.packageName = packageName;
    }

    private String generateId() {
        return UUID.randomUUID().toString();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getAppName() {
        if (packageName == null) {
            return "";
        }
        if (name != null && !name.isEmpty()) {
            return name;
        }
        // Lookup in global config if name is missing
        String globalName = com.notificationcapture.app.utils.ConfigManager.getInstance().getDefaultNameForPackage(packageName);
        if (globalName != null) {
            return globalName;
        }

        // Fallback: capitalize first letter of last part
        String[] parts = packageName.split("\\.");
        if (parts.length > 0) {
            String lastPart = parts[parts.length - 1];
            return lastPart.substring(0, 1).toUpperCase() + lastPart.substring(1);
        }
        return packageName;
    }

    @Override
    public String getDisplayName() {
        return this.getName();
    }

    @Override
    public Integer getDisplayColor() {
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Wallets wallets = (Wallets) o;
        return Objects.equals(id, wallets.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}