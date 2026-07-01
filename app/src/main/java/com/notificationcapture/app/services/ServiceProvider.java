package com.notificationcapture.app.services;

import com.notificationcapture.app.models.GlobalWallet;
import com.notificationcapture.app.repositories.RepositoryProvider;
import com.notificationcapture.app.utils.ConfigManager;

import java.util.ArrayList;
import java.util.List;

public class ServiceProvider {

    private static volatile ServiceProvider instance;

    private CategoryService categoryService;
    private TransactionService transactionService;
    private WalletService walletService;
    private NotificationParserService notificationParserService;

    private ServiceProvider() {}

    public static ServiceProvider getInstance() {
        if (instance == null) {
            synchronized (ServiceProvider.class) {
                if (instance == null) {
                    instance = new ServiceProvider();
                }
            }
        }
        return instance;
    }

    public CategoryService getCategoryService() {
        if (categoryService == null) {
            categoryService = new CategoryService(
                    RepositoryProvider.getInstance().getCategoryRepository()
            );
        }
        return categoryService;
    }

    public TransactionService getTransactionService() {
        if (transactionService == null) {
            transactionService = new TransactionService(
                    RepositoryProvider.getInstance().getTransactionRepository()
            );
        }
        return transactionService;
    }

    public WalletService getWalletService() {
        if (walletService == null) {
            walletService = new WalletService(
                    RepositoryProvider.getInstance().getWalletRepository(),
                    RepositoryProvider.getInstance().getCreditCardRepository()
            );
        }
        return walletService;
    }

    public NotificationParserService getNotificationParserService() {
        if (notificationParserService == null) {
            ConfigManager config = ConfigManager.getInstance();
            List<String> globalPackages = new ArrayList<>();
            for (GlobalWallet gw : config.getGlobalWallets()) {
                globalPackages.addAll(gw.getPackageNames());
            }

            notificationParserService = new NotificationParserService(
                    globalPackages,
                    config.getPaymentKeywords(),
                    getWalletService()
            );
        }
        return notificationParserService;
    }

    public static synchronized void resetForTesting() {
        instance = null;
    }
}
