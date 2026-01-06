package com.notificationcapture.app.models;

import com.notificationcapture.app.enums.IngresoOEgreso;
import com.notificationcapture.app.enums.PaymentMethod;

public class Debit extends Transaction {

    private Wallets wallet;

    public Debit(String title, String text, long timestamp, Wallets wallet) {
        super(PaymentMethod.DEBITO, title, text, timestamp);
        this.wallet = wallet;
    }

    public Debit(String title, String text, long timestamp, IngresoOEgreso type, Category category, Wallets wallet) {
        super(PaymentMethod.DEBITO, title, text, timestamp, type, category);
        this.wallet = wallet;
    }

    public Wallets getWallet() {
        return wallet;
    }

    public void setWallet(Wallets wallet) {
        this.wallet = wallet;
    }

    @Override
    public String getSourceName() {
        return wallet != null ? wallet.getAppName() : "Desconocido";
    }
}
