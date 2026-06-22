package com.notificationcapture.app.models;

import com.notificationcapture.app.enums.IngresoOEgreso;
import com.notificationcapture.app.enums.PaymentMethod;

public class Credit extends Transaction {

    private String creditCardId;
    private CreditCard creditCard; // Migración
    private int installments = 1; // Default 1
    private int currentInstallment = 1; // Default 1
    private String installmentGroupId; // ID to group related installments

    public Credit(String title, String text, long timestamp, String creditCardId, int installments,
            int currentInstallment, String installmentGroupId, boolean isNotification) {
        super(PaymentMethod.CREDITO, title, text, timestamp, isNotification);
        this.creditCardId = creditCardId;
        this.installments = installments;
        this.currentInstallment = currentInstallment;
        this.installmentGroupId = installmentGroupId;
    }

    public Credit(String title, String text, long timestamp, IngresoOEgreso type, String categoryId,
            String creditCardId, int installments, int currentInstallment, String installmentGroupId,
            boolean isNotification) {
        super(PaymentMethod.CREDITO, title, text, timestamp, type, categoryId, isNotification);
        this.creditCardId = creditCardId;
        this.installments = installments;
        this.currentInstallment = currentInstallment;
        this.installmentGroupId = installmentGroupId;
    }

    public String getCreditCardId() {
        if (creditCardId == null && creditCard != null) {
            return creditCard.getId();
        }
        return creditCardId;
    }

    public void setCreditCardId(String creditCardId) {
        this.creditCardId = creditCardId;
        this.creditCard = null; // Clear old object on new ID assignment
    }

    public int getInstallments() {
        return installments;
    }

    public void setInstallments(int installments) {
        this.installments = installments;
    }

    public int getCurrentInstallment() {
        return currentInstallment;
    }

    public void setCurrentInstallment(int currentInstallment) {
        this.currentInstallment = currentInstallment;
    }

    public String getInstallmentGroupId() {
        return installmentGroupId;
    }

    public void setInstallmentGroupId(String installmentGroupId) {
        this.installmentGroupId = installmentGroupId;
    }

    @Override
    public String getSourceName() {
        if (creditCard != null) return creditCard.getName();
        return (creditCardId != null && !creditCardId.isEmpty()) ? "Crédito (" + creditCardId + ")" : "Crédito";
    }
}
