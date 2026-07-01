package com.notificationcapture.app.services;

import com.notificationcapture.app.constants.PaymentConstants;
import com.notificationcapture.app.models.Cash;
import com.notificationcapture.app.models.Credit;
import com.notificationcapture.app.models.CreditCard;
import com.notificationcapture.app.models.Debit;
import com.notificationcapture.app.models.Transaction;
import com.notificationcapture.app.models.Wallets;
import com.notificationcapture.app.repositories.CreditCardRepository;
import com.notificationcapture.app.repositories.WalletRepository;

public class WalletService {

    private final WalletRepository walletRepo;
    private final CreditCardRepository cardRepo;

    WalletService(WalletRepository walletRepo, CreditCardRepository cardRepo) {
        this.walletRepo = walletRepo;
        this.cardRepo = cardRepo;
    }

    public String resolveSourceDisplayName(Transaction transaction) {
        if (transaction instanceof Debit) {
            Wallets w = walletRepo.getWalletById(((Debit) transaction).getWalletId());
            return w != null ? w.getAppName() : PaymentConstants.DISPLAY_DEBIT;
        }
        if (transaction instanceof Credit) {
            CreditCard c = cardRepo.getCreditCardById(((Credit) transaction).getCreditCardId());
            return c != null ? c.getName() : PaymentConstants.DISPLAY_CREDIT;
        }
        if (transaction instanceof Cash) {
            return PaymentConstants.DISPLAY_CASH;
        }
        return PaymentConstants.DISPLAY_CASH;
    }

    public Wallets resolveWalletByPackage(String title, String packageName) {
        return walletRepo.getWalletByPackageName(title, packageName);
    }
}
