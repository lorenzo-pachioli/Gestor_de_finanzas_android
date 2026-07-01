package com.notificationcapture.app.utils;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;
import com.notificationcapture.app.constants.PrefsConstants;
import java.io.IOException;
import java.security.GeneralSecurityException;

public class SecurityPreferencesManager {
    private SharedPreferences encryptedPrefs;

    public SecurityPreferencesManager(Context context) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            encryptedPrefs = EncryptedSharedPreferences.create(
                    context,
                    PrefsConstants.SECURE_SETTINGS_NAME,
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
            encryptedPrefs.edit().putString(PrefsConstants.KEY_LANGUAGE, lang).apply();
        }
    }

    public String getLanguage(String defaultLang) {
        return encryptedPrefs != null ? encryptedPrefs.getString(PrefsConstants.KEY_LANGUAGE, defaultLang) : defaultLang;
    }

    public void saveNightMode(int mode) {
        if (encryptedPrefs != null) {
            encryptedPrefs.edit().putInt(PrefsConstants.KEY_NIGHT_MODE, mode).apply();
        }
    }

    public int getNightMode(int defaultMode) {
        return encryptedPrefs != null ? encryptedPrefs.getInt(PrefsConstants.KEY_NIGHT_MODE, defaultMode) : defaultMode;
    }
}
