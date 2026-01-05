package com.notificationcapture.app.models;

import com.notificationcapture.app.enums.TransactionType;

public class Debit extends Transaction {

    private Wallets wallet;

    public Debit(String title, String text, long timestamp, Wallets wallet) {
        super(title, text, timestamp);
        this.wallet = wallet;
    }

    public Debit(String title, String text, long timestamp, TransactionType type, Category category, Wallets wallet) {
        super(title, text, timestamp, type, category);
        this.wallet = wallet;
    }

    public Wallets getWallet() {
        return wallet;
    }

    public void setWallet(Wallets wallet) {
        this.wallet = wallet;
    }
}
