package com.notificationcapture.app.listeners;

import android.content.Intent;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.app.Notification;

import androidx.annotation.Nullable;

import com.notificationcapture.app.models.Debit;
import com.notificationcapture.app.models.GlobalWallet;
import com.notificationcapture.app.models.Wallets;
import com.notificationcapture.app.repositories.RepositoryProvider;
import com.notificationcapture.app.repositories.TransactionRepository;
import com.notificationcapture.app.repositories.WalletRepository;
import com.notificationcapture.app.exceptions.*;
import com.notificationcapture.app.utils.AppLogger;
import com.notificationcapture.app.utils.ConfigManager;
import com.notificationcapture.app.utils.StringParser;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.math.BigDecimal;

public class NotificationListener extends NotificationListenerService {

    private TransactionRepository repository;
    private WalletRepository walletsRepository;
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();

    private Set<String> globalWalletPackagesSet;
    private Set<String> keywordSet;
    private List<String> walletAppsSubstring;

    @Override
    public void onCreate() {
        super.onCreate();
        repository = RepositoryProvider.getInstance().getTransactionRepository();
        walletsRepository = RepositoryProvider.getInstance().getWalletRepository();

        ConfigManager config = ConfigManager.getInstance();
                
        globalWalletPackagesSet = new HashSet<>();
        for (GlobalWallet w : config.getGlobalWallets()) {
            globalWalletPackagesSet.addAll(w.getPackageNames());
        }
        
        keywordSet = new HashSet<>();
        for (String kw : config.getPaymentKeywords()) {
            keywordSet.add(kw.toLowerCase());
        }
        
        walletAppsSubstring = config.getWalletApps();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        String packageName = sbn.getPackageName();
        Notification notification = sbn.getNotification();

        if (notification == null)
            return;

        if (packageName.equals(getPackageName())) {
            return;
        }

        Bundle extras = notification.extras;
        if (extras == null)
            return;

        String title = extras.getString(Notification.EXTRA_TITLE);
        String text = extras.getCharSequence(Notification.EXTRA_TEXT) != null
                ? extras.getCharSequence(Notification.EXTRA_TEXT).toString()
                : "";

        try {
            if (!isPaymentRelatedNotification(packageName, title, text)) {
                return;
            }
        } catch (ParserException e) {
            AppLogger.d("NotificationListener", "Filtro skipping: " + e.getMessage());
            return;
        }

        long timestamp = sbn.getPostTime();
        
        ioExecutor.execute(() -> {
            Wallets wallet = walletsRepository.getWalletByPackageName(title, packageName);
            Debit item = new Debit(
                    title != null ? title : "Sin título",
                    text,
                    timestamp,
                    wallet != null ? wallet.getId() : "1",
                    true);
    
            repository.saveTransactionNotFiltered(item);
    
            Intent intent = new Intent("com.notificationcapture.NEW_NOTIFICATION");
            sendBroadcast(intent);
        });
    }

    boolean isPaymentRelatedNotification(String packageName, String title, String text) throws ParserException {
        if (packageName == null) return false;

        boolean isWallet = false;
        if (globalWalletPackagesSet.contains(packageName)) {
            isWallet = true;
        } else {
            List<Wallets> userWallets = walletsRepository.getAllWallets();
            if (userWallets != null) {
                for (Wallets userWallet : userWallets) {
                    if (packageName.equals(userWallet.getPackageName())) {
                        isWallet = true;
                        break;
                    }
                }
            }
        }

        if (!isWallet) {
            return false;
        }

        boolean hasKeyword = false;
        String fullText = (title + " " + text).toLowerCase();
        for (String keyword : keywordSet) {
            if (fullText.contains(keyword)) {
                hasKeyword = true;
                break;
            }
        }

        if (!hasKeyword) {
            return false;
        }

        BigDecimal amount = StringParser.extractAmount(title, text);
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new AmountNotFoundException(fullText);
        }

        return true;
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ioExecutor.shutdown();
    }
}