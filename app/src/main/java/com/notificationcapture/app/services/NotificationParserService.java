package com.notificationcapture.app.services;

import com.notificationcapture.app.exceptions.AmountNotFoundException;
import com.notificationcapture.app.exceptions.ParserException;
import com.notificationcapture.app.models.Wallets;
import com.notificationcapture.app.utils.StringParser;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NotificationParserService {

    private final Set<String> globalWalletPackages;
    private final Set<String> keywordSet;
    private final WalletService walletService;

    NotificationParserService(List<String> globalPackages, List<String> keywords, WalletService walletService) {
        this.globalWalletPackages = new HashSet<>(globalPackages);
        this.keywordSet = new HashSet<>();
        for (String kw : keywords) {
            this.keywordSet.add(kw.toLowerCase());
        }
        this.walletService = walletService;
    }

    public boolean isPaymentNotification(String packageName, String title, String text, List<Wallets> userWallets)
            throws ParserException {
        if (packageName == null) return false;

        boolean isWallet = globalWalletPackages.contains(packageName);
        if (!isWallet && userWallets != null) {
            for (Wallets w : userWallets) {
                if (packageName.equals(w.getPackageName())) {
                    isWallet = true;
                    break;
                }
            }
        }
        if (!isWallet) return false;

        String fullText = ((title != null ? title : "") + " " + (text != null ? text : "")).toLowerCase();
        boolean hasKeyword = false;
        for (String kw : keywordSet) {
            if (fullText.contains(kw)) {
                hasKeyword = true;
                break;
            }
        }
        if (!hasKeyword) return false;

        BigDecimal amount = StringParser.extractAmount(title, text);
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new AmountNotFoundException(fullText);
        }
        return true;
    }
}
