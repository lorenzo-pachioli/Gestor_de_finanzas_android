package com.notificationcapture.app.models;

import com.notificationcapture.app.interfaces.SpinnerDisplayable;

import java.io.Serializable;

public class Wallets  implements Serializable, SpinnerDisplayable {

    private final String id;
    private String name;
    private String packageName;

    public Wallets(String name, String packageName) {
        this.id = generateId();
        this.name = name;
        this.packageName = packageName;
    }

    private String generateId() {
        return System.currentTimeMillis() + "_" + (int) (Math.random() * 10000);
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
        // Map package names to friendly app names
        switch (packageName) {
            case "com.mercadopago.wallet":
                return "Mercado Pago";
            case "com.uala.app":
                return "Ualá";
            case "brubank.app":
                return "Brubank";
            case "com.naranja.app":
                return "Naranja X";
            case "com.reba.contactless":
                return "Modo";
            case "personal.pay":
                return "Personal Pay";
            case "bimo.app":
                return "Bimo";
            case "ar.com.bind":
                return "BIND";
            case "ar.com.prex":
                return "Prex";
            case "ar.wilobank":
                return "Wilobank";
            case "ar.com.santander.rio":
                return "Santander Río";
            case "com.bbva.nxt_argentina":
                return "BBVA";
            case "ar.com.bancogalicia":
                return "Galicia";
            case "com.macro":
                return "Macro";
            case "ar.com.bna":
                return "Banco Nación";
            case "ar.gov.anses.mi":
                return "Mi Argentina";
            case "com.claro.pay":
                return "Claro Pay";
            default:
                // Fallback: capitalize first letter of last part
                String[] parts = packageName.split("\\.");
                if (parts.length > 0) {
                    String lastPart = parts[parts.length - 1];
                    return lastPart.substring(0, 1).toUpperCase() + lastPart.substring(1);
                }
                return packageName;
        }
    }

    @Override
    public String getDisplayName() {
        return this.getName();
    }

    @Override
    public Integer getDisplayColor() {
        return 0;
    }
}
