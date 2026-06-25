package com.notificationcapture.app;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.os.Process;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.lang.ref.WeakReference;

public class MyApplication extends Application implements Application.ActivityLifecycleCallbacks {

    private static WeakReference<Activity> currentActivityRef = new WeakReference<>(null);

    @Override
    public void onCreate() {
        super.onCreate();
        com.notificationcapture.app.utils.ConfigManager.init(this);
        registerActivityLifecycleCallbacks(this);
    }

    @Override
    public void onTerminate() {
        // No longer used. Cleanup is handled by AppLifecycleObserver.
        super.onTerminate();
    }

    public static Activity getCurrentActivity() {
        return currentActivityRef.get();
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        currentActivityRef = new WeakReference<>(activity);
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        currentActivityRef = new WeakReference<>(activity);
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
        if (currentActivityRef.get() == activity) {
            currentActivityRef.clear();
        }
    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
    }
}
