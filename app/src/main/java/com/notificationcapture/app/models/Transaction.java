package com.notificationcapture.app.models;

import static com.notificationcapture.app.utils.StringParser.detectTransactionType;
import static com.notificationcapture.app.utils.StringParser.extractAmount;

import com.notificationcapture.app.enums.IngresoOEgreso;
import com.notificationcapture.app.enums.PaymentMethod;

import java.io.Serializable;

public abstract class Transaction implements Serializable {
    private String id;
    private PaymentMethod paymentMethod;
    private String title;
    private String text;
    private long timestamp;
    private Double amount;
    private IngresoOEgreso type;
    private Category category;
    private boolean expanded = false;

    public Transaction(PaymentMethod paymentMethod, String title, String text, long timestamp) {
        this.id = generateId();
        this.paymentMethod = paymentMethod;
        this.title = title;
        this.text = text;
        this.timestamp = timestamp;
        this.amount = extractAmount(title, text);
        this.type = detectTransactionType(title, text);
        this.category = new Category("Otros", this.type); // Por defecto
    }

    // Constructor con tipo y categoría explícitos (para formulario manual)
    public Transaction(PaymentMethod paymentMethod, String title, String text, long timestamp,
                       IngresoOEgreso type, Category category) {
        this.id = generateId();
        this.paymentMethod = paymentMethod;
        this.title = title;
        this.text = text;
        this.timestamp = timestamp;
        this.amount = extractAmount(title, text);
        this.type = type;
        this.category = category;
    }

    private String generateId() {
        return System.currentTimeMillis() + "_" + (int) (Math.random() * 10000);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public IngresoOEgreso getType() {
        return type;
    }

    public void setType(IngresoOEgreso type) {
        this.type = type;
    }

    public Category getCategory() {
        if (category == null) {
            // Return a safe default to prevent NPEs
            return new Category("Otros", type != null ? type : IngresoOEgreso.EGRESO);
        }
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

    public String getFormattedAmount() {
        if (amount == null) {
            return null;
        }

        return String.format("$%.2f", amount)
                .replace(",", "#")
                .replace(".", ",")
                .replace("#", ".");
    }

    public boolean hasAmount() {
        return amount != null && amount > 0;
    }

    public abstract String getSourceName();
}
