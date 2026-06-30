package com.notificationcapture.app.ads;

//import android.app.Activity;
//import android.content.Context;
//import android.content.SharedPreferences;
//import androidx.annotation.NonNull;
//import androidx.annotation.Nullable;
//import com.google.android.gms.ads.*;
//import com.google.android.gms.ads.interstitial.InterstitialAd;
//import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
//import com.notificationcapture.app.utils.AppLogger;
//import java.util.Arrays;
//import java.util.concurrent.TimeUnit;

public class AdManager {
//
//    private static final String TAG        = "AdManager";
//    private static final String PREFS_NAME = "ad_manager_prefs";
//    private static final String KEY_LAST_SHOWN = "last_ad_shown_timestamp";
//    private static final long   INTERVAL_MS    = TimeUnit.DAYS.toMillis(7);
//
//    // IDs de prueba oficiales de Google
//    private static final String AD_UNIT_ID_TEST = "ca-app-pub-3940256099942544/1033173712";
//    private static final String AD_UNIT_ID_PROD = "ca-app-pub-XXXXXXXXXXXXXXXX/YYYYYYYYYY";
//
//    private static final boolean USE_TEST_ADS = true;
//
//    private static AdManager instance;
//
//    private final Context appContext;
//    private InterstitialAd interstitialAd;
//    private boolean isLoading   = false;
//    private boolean initialized = false;
//
//    private AdManager(Context context) {
//        this.appContext = context.getApplicationContext();
//    }
//
//    public static synchronized AdManager getInstance(Context context) {
//        if (instance == null) instance = new AdManager(context);
//        return instance;
//    }
//
//    public void init() {
//        if (initialized) return;
//        initialized = true;
//
//        RequestConfiguration config = new RequestConfiguration.Builder()
//                .setTestDeviceIds(Arrays.asList("DEVICE_ID_DE_TEST_AQUI"))
//                .build();
//        MobileAds.setRequestConfiguration(config);
//
//        MobileAds.initialize(appContext, status -> {
//            AppLogger.d(TAG, "AdMob inicializado");
//            loadAd();
//        });
//    }
//
//    private String getAdUnitId() {
//        return USE_TEST_ADS ? AD_UNIT_ID_TEST : AD_UNIT_ID_PROD;
//    }
//
//    private SharedPreferences prefs() {
//        return appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
//    }
//
//    private void loadAd() {
//        if (isLoading || interstitialAd != null) return;
//        isLoading = true;
//
//        InterstitialAd.load(appContext, getAdUnitId(), new AdRequest.Builder().build(),
//                new InterstitialAdLoadCallback() {
//                    @Override
//                    public void onAdLoaded(@NonNull InterstitialAd ad) {
//                        interstitialAd = ad;
//                        isLoading = false;
//                        AppLogger.d(TAG, "Interstitial precargado");
//                    }
//
//                    @Override
//                    public void onAdFailedToLoad(@NonNull LoadAdError error) {
//                        interstitialAd = null;
//                        isLoading = false;
//                        AppLogger.e(TAG, "Error cargando interstitial: " + error.getMessage());
//                    }
//                });
//    }
//
//    public boolean shouldShowAd() {
//        long lastShown = prefs().getLong(KEY_LAST_SHOWN, 0L);
//        return System.currentTimeMillis() - lastShown >= INTERVAL_MS;
//    }
//
//    public void maybeShowAd(@NonNull Activity activity, @Nullable Runnable onClosed) {
//        if (!shouldShowAd() || interstitialAd == null) {
//            if (onClosed != null) onClosed.run();
//            loadAd();
//            return;
//        }
//
//        interstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
//            @Override
//            public void onAdDismissedFullScreenContent() {
//                interstitialAd = null;
//                prefs().edit()
//                       .putLong(KEY_LAST_SHOWN, System.currentTimeMillis())
//                       .apply();
//                loadAd();
//                if (onClosed != null) onClosed.run();
//            }
//
//            @Override
//            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
//                interstitialAd = null;
//                loadAd();
//                if (onClosed != null) onClosed.run();
//            }
//        });
//
//        interstitialAd.show(activity);
//    }
}
