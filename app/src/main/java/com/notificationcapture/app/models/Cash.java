package com.notificationcapture.app.models;

import com.notificationcapture.app.enums.IngresoOEgreso;
import com.notificationcapture.app.enums.PaymentMethod;

public class Cash extends Transaction {

    public Cash(String title, String text, long timestamp) {
        super(PaymentMethod.EFECTIVO, title, text, timestamp);
    }

    public Cash(String title, String text, long timestamp, IngresoOEgreso type, Category category) {
        super(PaymentMethod.EFECTIVO, title, text, timestamp, type, category);
    }

    @Override
    public String getSourceName() {
        return "Efectivo";
    }
}
