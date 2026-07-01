package com.notificationcapture.app.services;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Pool de threads centralizado para toda la aplicación.
 * Singleton - acceder via AppExecutors.getInstance()
 */
public final class AppExecutors {

    private static volatile AppExecutors instance;

    private final Executor diskIO;
    private final Executor computation;
    private final Executor mainThread;
    private final ScheduledExecutorService scheduler;

    private AppExecutors() {
        diskIO = Executors.newSingleThreadExecutor();
        computation = Executors.newFixedThreadPool(
                Math.max(2, Runtime.getRuntime().availableProcessors() - 1)
        );
        mainThread = new MainThreadExecutor();
        scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    public static AppExecutors getInstance() {
        if (instance == null) {
            synchronized (AppExecutors.class) {
                if (instance == null) {
                    instance = new AppExecutors();
                }
            }
        }
        return instance;
    }

    public Executor diskIO() {
        return diskIO;
    }

    public Executor computation() {
        return computation;
    }

    public Executor mainThread() {
        return mainThread;
    }

    public ScheduledExecutorService scheduler() {
        return scheduler;
    }

    private static class MainThreadExecutor implements Executor {
        private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

        @Override
        public void execute(Runnable command) {
            mainThreadHandler.post(command);
        }
    }

    static synchronized void resetForTesting() {
        instance = null;
    }
}
