package com.notificationcapture.app.listeners;

import android.content.Intent;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.app.Notification;

import com.notificationcapture.app.models.Debit;
import com.notificationcapture.app.models.Wallets;
import com.notificationcapture.app.repositories.RepositoryProvider;
import com.notificationcapture.app.repositories.TransactionRepository;
import com.notificationcapture.app.repositories.WalletRepository;

public class NotificationListener extends NotificationListenerService {

    private TransactionRepository repository;
    private WalletRepository walletsRepository;

    // Listas cargadas desde JSON vía ConfigManager
    private java.util.List<String> walletApps;
    private java.util.List<String> paymentKeywords;
    private java.util.List<com.notificationcapture.app.models.Wallets> globalWallets;

    @Override
    public void onCreate() {
        super.onCreate();
        repository = RepositoryProvider.getInstance().getTransactionRepository();
        walletsRepository = RepositoryProvider.getInstance().getWalletRepository();

        // Cargar configuraciones
        com.notificationcapture.app.utils.ConfigManager config = com.notificationcapture.app.utils.ConfigManager
                .getInstance();
        walletApps = config.getWalletApps();
        paymentKeywords = config.getPaymentKeywords();
        globalWallets = config.getGlobalWallets();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        String packageName = sbn.getPackageName();
        Notification notification = sbn.getNotification();

        if (notification == null)
            return;

        // Evitar capturar notificaciones de la propia app
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

        // Filtrar solo notificaciones de wallets/pagos
        if (!isPaymentRelatedNotification(packageName, title, text)) {
            return; // Ignorar completamente las notificaciones que no son transacciones
        }

        long timestamp = sbn.getPostTime();
        // Crear el objeto Transaction (Debit por defecto)
        Wallets wallet = walletsRepository.getWalletByPackageName(title, packageName); // Usamos packageName
                                                                                       // temporalmente como nombre si
                                                                                       // no hay
        // mapping
        Debit item = new Debit(
                title != null ? title : "Sin título",
                text,
                timestamp,
                wallet.getId(),
                true);

        // Guardar en la lista de notificaciones pendientes de revisión
        repository.saveTransactionNotFiltered(item);

        // Notificar a la actividad para actualizar la UI
        Intent intent = new Intent("com.notificationcapture.NEW_NOTIFICATION");
        sendBroadcast(intent);
    }

    boolean isPaymentRelatedNotification(String packageName, String title, String text) {

        // Verificar si es una app de wallet (prioridad: lista global)
        boolean isWalletApp = false;

        // 1. Verificar contra el packagename exacto en la lista global
        for (com.notificationcapture.app.models.Wallets wallet : globalWallets) {
            if (wallet.getPackageName().equals(packageName)) {
                isWalletApp = true;
                break;
            }
        }

        // 2. Fallback: Verificar si contiene palabras clave en el packagename (compatibilidad lista anterior)
        if (!isWalletApp) {
            for (String wallet : walletApps) {
                if (packageName.toLowerCase().contains(wallet)) {
                    isWalletApp = true;
                    break;
                }
            }
        }

        if (!isWalletApp) {
            return false;
        }

        String fullText = (title + " " + text).toLowerCase();

        for (String keyword : paymentKeywords) {
            if (fullText.contains(keyword)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        // Opcional: manejar cuando se elimina una notificación
    }
}