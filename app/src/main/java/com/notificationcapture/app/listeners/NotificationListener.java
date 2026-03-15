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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NotificationListener extends NotificationListenerService {

    private TransactionRepository repository;
    private WalletRepository walletsRepository;
    private final java.util.concurrent.ExecutorService ioExecutor = java.util.concurrent.Executors.newSingleThreadExecutor();

    // Listas cacheadas para O(1) matching
    private Set<String> globalWalletPackagesSet;
    private Set<String> keywordSet;
    private List<String> walletAppsSubstring;

    @Override
    public void onCreate() {
        super.onCreate();
        repository = RepositoryProvider.getInstance().getTransactionRepository();
        walletsRepository = RepositoryProvider.getInstance().getWalletRepository();

        // Cargar configuraciones
        com.notificationcapture.app.utils.ConfigManager config = com.notificationcapture.app.utils.ConfigManager
                .getInstance();
                
        // Optimización algorítmica: Cachear en HashSets para matching O(1)
        globalWalletPackagesSet = new HashSet<>();
        for (com.notificationcapture.app.models.Wallets w : config.getGlobalWallets()) {
            globalWalletPackagesSet.add(w.getPackageName());
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
        
        ioExecutor.execute(() -> {
            // Crear el objeto Transaction (Debit por defecto)
            Wallets wallet = walletsRepository.getWalletByPackageName(title, packageName); // Usamos packageName
                                                                                           // temporalmente como nombre si
                                                                                           // no hay
            // mapping
            Debit item = new Debit(
                    title != null ? title : "Sin título",
                    text,
                    timestamp,
                    wallet != null ? wallet.getId() : "1",
                    true);
    
            // Guardar en la lista de notificaciones pendientes de revisión
            repository.saveTransactionNotFiltered(item);
    
            // Notificar a la actividad para actualizar la UI
            Intent intent = new Intent("com.notificationcapture.NEW_NOTIFICATION");
            sendBroadcast(intent);
        });
    }

    boolean isPaymentRelatedNotification(String packageName, String title, String text) {

        // 1. Verificar contra el packagename exacto en la lista global O(1)
        if (globalWalletPackagesSet.contains(packageName)) {
            return true;
        }

        // 2. Verificar contra wallets personalizadas del usuario
        List<com.notificationcapture.app.models.Wallets> userWallets = walletsRepository.getAllWallets();
        for (com.notificationcapture.app.models.Wallets userWallet : userWallets) {
            if (userWallet.getPackageName().equals(packageName)) {
                return true;
            }
        }

        // 3. Fallback: Verificar si contiene palabras clave en el packagename
        String packageNameLower = packageName.toLowerCase();
        for (String wallet : walletAppsSubstring) {
            if (packageNameLower.contains(wallet)) {
                return true;
            }
        }

        // 4. Verificación por palabras clave en el cuerpo del mensaje O(1)
        String fullText = (title + " " + text).toLowerCase();
        for (String keyword : keywordSet) {
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