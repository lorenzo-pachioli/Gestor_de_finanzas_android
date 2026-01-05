package com.notificationcapture.app.models;

import com.notificationcapture.app.enums.TransactionType;

public class Credit extends Transaction {

    private CreditCard creditCard;
    private int installments = 1; // Default 1
    private int currentInstallment = 1; // Default 1
    private String installmentGroupId; // ID to group related installments

    public Credit(String title, String text, long timestamp, CreditCard creditCard, int installments, int currentInstallment, String installmentGroupId) {
        super(title, text, timestamp);
        this.creditCard = creditCard;
        this.installments = installments;
        this.currentInstallment = currentInstallment;
        this.installmentGroupId = installmentGroupId;
    }

    public Credit(String title, String text, long timestamp, TransactionType type, Category category, CreditCard creditCard, int installments, int currentInstallment, String installmentGroupId) {
        super(title, text, timestamp, type, category);
        this.creditCard = creditCard;
        this.installments = installments;
        this.currentInstallment = currentInstallment;
        this.installmentGroupId = installmentGroupId;
    }

    public CreditCard getCreditCard() {
        return creditCard;
    }

    public void setCreditCard(CreditCard creditCard) {
        this.creditCard = creditCard;
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
}
