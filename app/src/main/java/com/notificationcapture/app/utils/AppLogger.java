package com.notificationcapture.app.utils;

import android.util.Log;

/**
 * Centralized logger for the application.
 */
public class AppLogger {
    private static final String DEFAULT_TAG = "NotificationCapture";

    public static void d(String message) {
        Log.d(DEFAULT_TAG, message);
    }

    public static void d(String tag, String message) {
        Log.d(tag, message);
    }

    public static void i(String message) {
        Log.i(DEFAULT_TAG, message);
    }

    public static void w(String message) {
        Log.w(DEFAULT_TAG, message);
    }

    public static void e(String message) {
        Log.e(DEFAULT_TAG, message);
    }

    public static void e(String tag, String message) {
        Log.e(tag, message);
    }

    public static void e(String message, Throwable throwable) {
        Log.e(DEFAULT_TAG, message, throwable);
    }

    public static void e(String tag, String message, Throwable throwable) {
        Log.e(tag, message, throwable);
    }
}
