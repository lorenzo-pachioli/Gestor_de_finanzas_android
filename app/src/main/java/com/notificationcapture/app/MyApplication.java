package com.notificationcapture.app;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.os.Process;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class MyApplication extends Application implements Application.ActivityLifecycleCallbacks {

    private static volatile Activity currentActivity;

    @Override
    public void onCreate() {
        super.onCreate();
        com.notificationcapture.app.utils.ConfigManager.init(this);
        registerActivityLifecycleCallbacks(this);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        if (com.notificationcapture.app.repositories.RepositoryProvider.isInitialized()) {
            try {
                com.notificationcapture.app.repositories.RepositoryProvider.getInstance().getTransactionRepository().shutdown();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        Process.killProcess(Process.myPid());
        System.exit(1);
    }

    public static Activity getCurrentActivity() {
        return currentActivity;
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        currentActivity = activity;
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        currentActivity = activity;
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
        if (currentActivity == activity) {
            currentActivity = null;
        }
    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
    }
}
