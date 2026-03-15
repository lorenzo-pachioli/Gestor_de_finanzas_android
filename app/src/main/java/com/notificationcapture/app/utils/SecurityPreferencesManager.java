package com.notificationcapture.app.utils;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;
import java.io.IOException;
import java.security.GeneralSecurityException;

public class SecurityPreferencesManager {
    private SharedPreferences encryptedPrefs;
    private static final String PREFS_NAME = "secure_app_settings";
    private static final String KEY_LANGUAGE = "app_language";
    private static final String KEY_NIGHT_MODE = "night_mode";

    public SecurityPreferencesManager(Context context) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            encryptedPrefs = EncryptedSharedPreferences.create(
                    context,
                    PREFS_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
        }
    }

    public void saveLanguage(String lang) {
        if (encryptedPrefs != null) {
            encryptedPrefs.edit().putString(KEY_LANGUAGE, lang).apply();
        }
    }

    public String getLanguage(String defaultLang) {
        return encryptedPrefs != null ? encryptedPrefs.getString(KEY_LANGUAGE, defaultLang) : defaultLang;
    }

    public void saveNightMode(int mode) {
        if (encryptedPrefs != null) {
            encryptedPrefs.edit().putInt(KEY_NIGHT_MODE, mode).apply();
        }
    }

    public int getNightMode(int defaultMode) {
        return encryptedPrefs != null ? encryptedPrefs.getInt(KEY_NIGHT_MODE, defaultMode) : defaultMode;
    }
}
