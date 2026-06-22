package com.notificationcapture.app.models;

import com.notificationcapture.app.enums.IngresoOEgreso;
import com.notificationcapture.app.enums.PaymentMethod;

public class Debit extends Transaction {

    private String walletId;
    private Wallets wallet; // Migración

    public Debit(String title, String text, long timestamp, String walletId, boolean isNotification) {
        super(PaymentMethod.DEBITO, title, text, timestamp, isNotification);
        this.walletId = walletId;
    }

    public Debit(String title, String text, long timestamp, IngresoOEgreso type, String categoryId, String walletId,
            boolean isNotification) {
        super(PaymentMethod.DEBITO, title, text, timestamp, type, categoryId, isNotification);
        this.walletId = walletId;
    }

    public String getWalletId() {
        if (walletId == null && wallet != null) {
            return wallet.getId();
        }
        return walletId;
    }

    public void setWalletId(String walletId) {
        this.walletId = walletId;
        this.wallet = null; // Clear old object on new ID assignment
    }

    @Override
    public String getSourceName() {
        if (wallet != null) return wallet.getAppName();
        return walletId != null ? "Débito (" + walletId + ")" : "Débito";
    }
}
