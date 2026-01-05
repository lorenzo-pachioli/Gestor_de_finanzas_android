package com.notificationcapture.app.models;

import com.notificationcapture.app.enums.TransactionType;

public class Cash extends Transaction {

    public Cash(String title, String text, long timestamp) {
        super(title, text, timestamp);
    }

    public Cash(String title, String text, long timestamp, TransactionType type, Category category) {
        super(title, text, timestamp, type, category);
    }
}
