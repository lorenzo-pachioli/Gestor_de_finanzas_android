package com.notificationcapture.app.constants;

public final class DatabaseConstants {
    private DatabaseConstants() {}

    public static final String DB_NAME = "notification_database";
    public static final String TABLE_TRANSACTIONS = "transactions";
    public static final String TABLE_CREDIT_PAYMENTS = "credit_card_payments";
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final int DB_VERSION = 5;
}
