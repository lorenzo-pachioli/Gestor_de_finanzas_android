package com.notificationcapture.app.interfaces;

import com.notificationcapture.app.enums.PaymentMethod;

public interface PaymentMethodListener {
    void onPaymentMethodSelected(PaymentMethod method, String detail, int installments);
}
