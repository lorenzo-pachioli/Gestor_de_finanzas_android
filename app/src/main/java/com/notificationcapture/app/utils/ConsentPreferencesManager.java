package com.notificationcapture.app.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class ConsentPreferencesManager {

    private static final String PREFS_NAME = "consent_prefs";
    private static final String KEY_ACCEPTED = "terms_accepted";
    private static final String KEY_VERSION = "terms_version";
    private static final String KEY_TIMESTAMP = "accepted_timestamp";

    /** Increment when legal text changes */
    public static final String CURRENT_TERMS_VERSION = "1.0";

    private final SharedPreferences prefs;

    public ConsentPreferencesManager(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /** true only if accepted and version matches */
    public boolean isUpToDateConsentGiven() {
        return prefs.getBoolean(KEY_ACCEPTED, false)
                && CURRENT_TERMS_VERSION.equals(prefs.getString(KEY_VERSION, ""));
    }

    /** Call when user accepts */
    public void saveAcceptance() {
        prefs.edit()
                .putBoolean(KEY_ACCEPTED, true)
                .putString(KEY_VERSION, CURRENT_TERMS_VERSION)
                .putLong(KEY_TIMESTAMP, System.currentTimeMillis())
                .apply();
    }
}
