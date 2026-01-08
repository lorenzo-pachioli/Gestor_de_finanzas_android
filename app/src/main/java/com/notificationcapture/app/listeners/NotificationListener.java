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

public class NotificationListener extends NotificationListenerService {

    private TransactionRepository repository;

    // Lista de apps de wallet/bancos conocidas (añade las que uses)
    private String[] walletApps = {
            "mercadopago",
            "mercado pago",
            "ualá",
            "uala",
            "brubank",
            "naranja",
            "galicia",
            "santander",
            "bbva",
            "hsbc",
            "macro",
            "nacion",
            "ciudad",
            "patagonia",
            "icbc",
            "supervielle",
            "bind",
            "modo",
            "personal.pay",
            "prex",
            "lemon",
            "belo",
            "flow",
            "wallet",
    };

    // Palabras clave que indican pago exitoso o transferencia recibida
    private String[] paymentKeywords = {
            "pago exitoso",
            "pago realizado",
            "pago aprobado",
            "pagaste",
            "pago acreditado",
            "transferencia exitosa",
            "transferencia realizada",
            "transferiste",
            "recibiste",
            "recibiste dinero",
            "ingreso de dinero",
            "ingresó dinero",
            "ingresaste",
            "te enviaron",
            "te enviaron dinero",
            "cobro exitoso",
            "cobraste",
            "acreditación exitosa",
            "dinero acreditado",
            "saldo acreditado",
            "successful payment",
            "payment successful",
            "payment approved",
            "transfer successful",
            "money received",
            "you received"
    };

    @Override
    public void onCreate() {
        super.onCreate();
        repository = RepositoryProvider.getInstance().getTransactionRepository();
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

        long timestamp = sbn.getPostTime();
        // Crear el objeto Transaction (Debit por defecto)
        Wallets wallet = new Wallets(title, packageName); // Usamos packageName temporalmente como nombre si no hay
                                                          // mapping
        Debit item = new Debit(
                title != null ? title : "Sin título",
                text,
                timestamp,
                wallet,
                true);

        repository.saveTransactionNotFiltered(item);

        // Filtrar solo notificaciones de wallets/pagos
        if (!isPaymentRelatedNotification(packageName, title, text)) {
            return;
        }

        // Guardar la notificación
        repository.saveTransaction(item);

        // Notificar a la actividad para actualizar la UI
        Intent intent = new Intent("com.notificationcapture.NEW_NOTIFICATION");
        sendBroadcast(intent);
    }

    boolean isPaymentRelatedNotification(String packageName, String title, String text) {

        // Verificar si es una app de wallet
        boolean isWalletApp = false;
        for (String wallet : walletApps) {
            if (packageName.toLowerCase().contains(wallet)) {
                isWalletApp = true;
                break;
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