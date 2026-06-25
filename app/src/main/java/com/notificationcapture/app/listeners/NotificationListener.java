package com.notificationcapture.app.listeners;

import android.content.Intent;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.app.Notification;

import com.notificationcapture.app.models.Debit;
import com.notificationcapture.app.models.GlobalWallet;
import com.notificationcapture.app.models.Wallets;
import com.notificationcapture.app.repositories.RepositoryProvider;
import com.notificationcapture.app.repositories.TransactionRepository;
import com.notificationcapture.app.repositories.WalletRepository;
import com.notificationcapture.app.exceptions.*;
import com.notificationcapture.app.utils.AppLogger;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.math.BigDecimal;

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
        // Indexa TODOS los packageNames alternativos de cada wallet global
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
        try {
            if (!isPaymentRelatedNotification(packageName, title, text)) {
                return; // Ignorar completamente las notificaciones que no son transacciones
            }
        } catch (ParserException e) {
            // Logs específicos por si queremos debugear por qué falló el parseo de un pago
            AppLogger.d("NotificationListener", "Filtro skipping: " + e.getMessage());
            return;
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

    boolean isPaymentRelatedNotification(String packageName, String title, String text) throws ParserException {
        if (packageName == null) return false;

        // 1. Debe pertenecer a una wallet de la lista global (o a una personalizada del usuario)
        boolean isWallet = false;
        if (globalWalletPackagesSet.contains(packageName)) {
            isWallet = true;
        } else {
            List<com.notificationcapture.app.models.Wallets> userWallets = walletsRepository.getAllWallets();
            if (userWallets != null) {
                for (com.notificationcapture.app.models.Wallets userWallet : userWallets) {
                    if (packageName.equals(userWallet.getPackageName())) {
                        isWallet = true;
                        break;
                    }
                }
            }
        }

        if (!isWallet) {
            // No lanzamos excepción aquí, simplemente no es de una wallet conocida, es normal
            return false;
        }

        // 2. La notificación debe contener alguna palabra clave que indique que es una transacción
        boolean hasKeyword = false;
        String fullText = (title + " " + text).toLowerCase();
        for (String keyword : keywordSet) {
            if (fullText.contains(keyword)) {
                hasKeyword = true;
                break;
            }
        }

        if (!hasKeyword) {
            // Si es una app de wallet pero no tiene palabras clave de pago, es ruido (ej. publicidad de la app)
            return false;
        }

        // 3. Se debe reconocer el monto de la transferencia en la notificación
        BigDecimal amount = com.notificationcapture.app.utils.StringParser.extractAmount(title, text);
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            // Aquí sí lanzamos excepción porque ES de una wallet y TIENE keywords, pero fallamos al parsear el monto
            throw new AmountNotFoundException(fullText);
        }

        return true;
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        // Opcional: manejar cuando se elimina una notificación
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ioExecutor.shutdown();
    }
}