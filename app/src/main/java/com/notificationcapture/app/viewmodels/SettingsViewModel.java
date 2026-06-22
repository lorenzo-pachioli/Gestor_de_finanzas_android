package com.notificationcapture.app.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.appcompat.app.AppCompatDelegate;

public class SettingsViewModel extends ViewModel {
    private final MutableLiveData<String> languageState = new MutableLiveData<>();
    private final MutableLiveData<Integer> nightModeState = new MutableLiveData<>(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

    public LiveData<String> getLanguageState() {
        return languageState;
    }

    public void setLanguage(String langCode) {
        languageState.setValue(langCode);
    }

    public LiveData<Integer> getNightModeState() {
        return nightModeState;
    }

    public void setNightMode(int mode) {
        nightModeState.setValue(mode);
    }
}
