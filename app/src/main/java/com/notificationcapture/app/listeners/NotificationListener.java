package com.notificationcapture.app.listeners;

import android.app.Notification;
import android.content.Intent;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import com.notificationcapture.app.constants.NotificationConstants;
import com.notificationcapture.app.exceptions.ParserException;
import com.notificationcapture.app.models.Debit;
import com.notificationcapture.app.models.Wallets;
import com.notificationcapture.app.repositories.RepositoryProvider;
import com.notificationcapture.app.repositories.TransactionRepository;
import com.notificationcapture.app.repositories.WalletRepository;
import com.notificationcapture.app.services.ServiceProvider;
import com.notificationcapture.app.utils.AppLogger;
import com.notificationcapture.app.utils.StringParser;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NotificationListener extends NotificationListenerService {

    private TransactionRepository repository;
    private WalletRepository walletsRepository;
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();

    @Override
    public void onCreate() {
        super.onCreate();
        repository = RepositoryProvider.getInstance().getTransactionRepository();
        walletsRepository = RepositoryProvider.getInstance().getWalletRepository();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        String packageName = sbn.getPackageName();
        Notification notification = sbn.getNotification();

        if (notification == null || packageName == null || packageName.equals(getPackageName())) {
            return;
        }

        Bundle extras = notification.extras;
        if (extras == null) {
            return;
        }

        String title = extras.getString(Notification.EXTRA_TITLE);
        String text = extras.getCharSequence(Notification.EXTRA_TEXT) != null
                ? extras.getCharSequence(Notification.EXTRA_TEXT).toString()
                : "";

        List<Wallets> userWallets = walletsRepository.getAllWallets();
        boolean isKnownWallet = false;
        try {
            isKnownWallet = ServiceProvider.getInstance()
                    .getNotificationParserService()
                    .isPaymentNotification(packageName, title, text, userWallets);
        } catch (ParserException e) {
            AppLogger.d("NotificationListener", "Filtro skipping: " + e.getMessage());
            isKnownWallet = false;
        }

        if (isKnownWallet) {
            saveKnownWalletNotification(sbn, packageName, title, text);
            return;
        }

        if (!isUnrecognizedPaymentCandidate(packageName, title, text)) {
            return;
        }

        saveUnrecognizedNotification(sbn, packageName, title, text);
    }

    private void saveKnownWalletNotification(StatusBarNotification sbn, String packageName, String title, String text) {
        long timestamp = sbn.getPostTime();

        ioExecutor.execute(() -> {
            Wallets wallet = ServiceProvider.getInstance()
                    .getWalletService()
                    .resolveWalletByPackage(title, packageName);
            Debit item = new Debit(
                    title != null ? title : "Sin título",
                    text,
                    timestamp,
                    wallet != null ? wallet.getId() : "1",
                    true);
            item.setSourcePackageName(packageName);

            repository.saveTransactionNotFiltered(item);

            Intent intent = new Intent(NotificationConstants.ACTION_NEW_NOTIFICATION);
            sendBroadcast(intent);
        });
    }

    private void saveUnrecognizedNotification(StatusBarNotification sbn, String packageName, String title, String text) {
        long timestamp = sbn.getPostTime();

        ioExecutor.execute(() -> {
            Debit item = new Debit(
                    title != null ? title : "Sin título",
                    text,
                    timestamp,
                    (String) null,
                    true);
            item.setSourcePackageName(packageName);

            BigDecimal amount = StringParser.extractAmount(title, text);
            item.setAmount(amount);
            item.setType(StringParser.detectTransactionType(title, text));

            repository.saveUnrecognizedTransaction(item);

            Intent intent = new Intent(NotificationConstants.ACTION_NEW_NOTIFICATION);
            sendBroadcast(intent);
        });
    }

    private boolean isUnrecognizedPaymentCandidate(String packageName, String title, String text) {
        if (packageName == null) return false;
        if (packageName.equals(getPackageName())) return false;

        String fullText = ((title != null ? title : "") + " " + (text != null ? text : "")).toLowerCase();
        if (!ServiceProvider.getInstance().getNotificationParserService().hasPaymentKeyword(title, text)) {
            return false;
        }

        BigDecimal amount = StringParser.extractAmount(title, text);
        return amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
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
