package com.notificationcapture.app.models;

import com.notificationcapture.app.enums.IngresoOEgreso;
import com.notificationcapture.app.enums.PaymentMethod;

public class Cash extends Transaction {

    public Cash(String title, String text, long timestamp, boolean isNotification) {
        super(PaymentMethod.EFECTIVO, title, text, timestamp, isNotification);
    }

    public Cash(String title, String text, long timestamp, IngresoOEgreso type, String categoryId,
            boolean isNotification) {
        super(PaymentMethod.EFECTIVO, title, text, timestamp, type, categoryId, isNotification);
    }

    @Override
    public String getSourceName() {
        return PaymentMethod.DISPLAY_CASH;
    }
}
