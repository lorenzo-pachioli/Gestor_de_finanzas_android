package com.notificationcapture.app.constants;

public final class PrefsConstants {
    private PrefsConstants() {}

    public static final String PREFS_NAME = "NotificationPrefs";
    public static final String SECURE_PREFS_NAME = "encrypted_secure_prefs";
    public static final String SECURE_SETTINGS_NAME = "secure_app_settings";

    public static final String KEY_NOTIFICATIONS = "notifications";
    public static final String KEY_NOTIFICATIONS_PENDING = "notificationsNotFiltered";
    public static final String KEY_WALLETS = "wallets";
    public static final String KEY_CATEGORIES = "categories";
    public static final String KEY_CREDIT_CARDS = "credit_cards";

    public static final String KEY_LANGUAGE = "app_language";
    public static final String KEY_NIGHT_MODE = "night_mode";

    public static final String KEY_MIGRATED = "sqlite_migrated";
    public static final String KEY_MIGRATION_IN_PROGRESS = "sqlite_migration_in_progress";
    public static final String KEY_LAST_MAINTENANCE_MONTH = "last_maintenance_month";

    public static final String DEFAULT_LANGUAGE = "es";
}
