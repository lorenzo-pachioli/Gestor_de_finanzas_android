package com.notificationcapture.app;

import static com.notificationcapture.app.utils.StringParser.detectTransactionType;
import static com.notificationcapture.app.utils.StringParser.extractAmount;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.io.Serializable;

import com.notificationcapture.app.enums.TransactionType;
import com.notificationcapture.app.enums.PaymentMethod;

public class NotificationItem implements Serializable {
    private String id;
    private String packageName;
    private String title;
    private String text;
    private long timestamp;
    private Double amount;
    private TransactionType type;
    private String category;

    // Enums moved to package com.notificationcapture.app.enums

    private PaymentMethod paymentMethod;
    private String paymentMethodDetail; // Wallet name or Credit Card name
    private int installments = 1; // Default 1
    private int currentInstallment = 1; // Default 1
    private String installmentGroupId; // ID to group related installments

    public static final String[] OUTCOME_CATEGORIES = {
            "Otros", "Comida", "Combustible", "Transporte", "Servicios",
            "Entretenimiento", "Salud", "Educación", "Compras", "Vivienda"
    };

    public static final String[] INCOME_CATEGORIES = {
            "Otros", "Salario", "Inversiones", "Reembolsos", "Familiares", "Venta"
    };

    public NotificationItem(String packageName, String title, String text, long timestamp) {
        this.id = generateId();
        this.packageName = packageName;
        this.title = title;
        this.text = text;
        this.timestamp = timestamp;
        this.amount = extractAmount(title, text);
        this.type = detectTransactionType(title, text);
        this.category = "Otros"; // Por defecto
    }

    // Constructor con tipo y categoría explícitos (para formulario manual)
    public NotificationItem(String packageName, String title, String text, long timestamp,
            TransactionType type, String category) {
        this.id = generateId();
        this.packageName = packageName;
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

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
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

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
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

    public String getAppName() {
        // Map package names to friendly app names
        switch (packageName) {
            case "com.mercadopago.wallet":
                return "Mercado Pago";
            case "com.uala.app":
                return "Ualá";
            case "brubank.app":
                return "Brubank";
            case "com.naranja.app":
                return "Naranja X";
            case "com.reba.contactless":
                return "Modo";
            case "personal.pay":
                return "Personal Pay";
            case "bimo.app":
                return "Bimo";
            case "ar.com.bind":
                return "BIND";
            case "ar.com.prex":
                return "Prex";
            case "ar.wilobank":
                return "Wilobank";
            case "ar.com.santander.rio":
                return "Santander Río";
            case "com.bbva.nxt_argentina":
                return "BBVA";
            case "ar.com.bancogalicia":
                return "Galicia";
            case "com.macro":
                return "Macro";
            case "ar.com.bna":
                return "Banco Nación";
            case "ar.gov.anses.mi":
                return "Mi Argentina";
            case "com.claro.pay":
                return "Claro Pay";
            default:
                // Fallback: capitalize first letter of last part
                String[] parts = packageName.split("\\.");
                if (parts.length > 0) {
                    String lastPart = parts[parts.length - 1];
                    return lastPart.substring(0, 1).toUpperCase() + lastPart.substring(1);
                }
                return packageName;
        }
    }

    private boolean expanded = false;

    public boolean isExpanded() {
        return expanded;
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getPaymentMethodDetail() {
        return paymentMethodDetail;
    }

    public void setPaymentMethodDetail(String paymentMethodDetail) {
        this.paymentMethodDetail = paymentMethodDetail;
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
