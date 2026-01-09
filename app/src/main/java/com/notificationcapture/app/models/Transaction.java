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
    private String categoryId;
    private Category category; // Mantener para migración de datos viejos
    private boolean expanded = false;
    private boolean isNotification;

    public Transaction(PaymentMethod paymentMethod, String title, String text, long timestamp, boolean isNotification) {
        this.id = generateId();
        this.paymentMethod = paymentMethod;
        this.title = title;
        this.text = text;
        this.timestamp = timestamp;
        this.amount = extractAmount(title, text);
        this.type = detectTransactionType(title, text);
        this.isNotification = isNotification;
        // Asignar ID por defecto de "Otros" según el tipo detectado
        this.categoryId = (this.type == IngresoOEgreso.INGRESO) ? "other_income" : "other_outcome";
        // necesario
    }

    // Constructor con tipo y categoría explícitos (para formulario manual)
    public Transaction(PaymentMethod paymentMethod, String title, String text, long timestamp,
            IngresoOEgreso type, String categoryId, boolean isNotification) {
        this.id = generateId();
        this.paymentMethod = paymentMethod;
        this.title = title;
        this.text = text;
        this.timestamp = timestamp;
        this.amount = extractAmount(title, text);
        this.type = type;
        this.categoryId = categoryId;
        this.isNotification = isNotification;
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

    public String getCategoryId() {
        if (categoryId == null && category != null) {
            // Migración: si no hay ID pero hay objeto, devolver el ID del objeto
            return category.getId();
        }
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
        this.category = null; // Limpiar objeto viejo al asignar nuevo ID
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

    public boolean isNotification() {
        return isNotification;
    }

    public void setNotification(boolean notification) {
        isNotification = notification;
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
