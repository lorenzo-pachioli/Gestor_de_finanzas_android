package com.notificationcapture.app.interfaces;

import com.notificationcapture.app.constants.AppConstants;
import com.notificationcapture.app.constants.PrefsConstants;

public interface GsonAccess {
    String PREFS_NAME = PrefsConstants.PREFS_NAME;
    String KEY_NOTIFICATIONS = PrefsConstants.KEY_NOTIFICATIONS;
    String KEY_NOTIFICATIONS_NOT_FILTERED = PrefsConstants.KEY_NOTIFICATIONS_PENDING;
    String KEY_WALLETS = PrefsConstants.KEY_WALLETS;
    String KEY_CATEGORIES = PrefsConstants.KEY_CATEGORIES;
    String KEY_CREDIT_CARDS = PrefsConstants.KEY_CREDIT_CARDS;
    int MAX_NOTIFICATIONS = AppConstants.MAX_NOTIFICATIONS;
}
