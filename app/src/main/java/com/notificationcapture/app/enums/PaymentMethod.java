package com.notificationcapture.app.enums;

import com.notificationcapture.app.constants.PaymentConstants;

public enum PaymentMethod {
    EFECTIVO,
    DEBITO,
    CREDITO;

    public static final String TYPE_DEBIT = PaymentConstants.TYPE_DEBIT;
    public static final String TYPE_CREDIT = PaymentConstants.TYPE_CREDIT;
    public static final String TYPE_CASH = PaymentConstants.TYPE_CASH;
    public static final String DISPLAY_CASH = PaymentConstants.DISPLAY_CASH;
}
