package com.notificationcapture.app.database;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import com.notificationcapture.app.enums.IngresoOEgreso;
import com.notificationcapture.app.enums.PaymentMethod;
import java.math.BigDecimal;

@Entity(tableName = "transactions", 
        indices = {@Index(value = {"timestamp"}), @Index(value = {"status"}), @Index(value = {"creditCardId"})})
public class TransactionEntity {

    @PrimaryKey
    @NonNull
    private String id;
    
    private PaymentMethod paymentMethod;
    private String title;
    private String text;
    private long timestamp;
    private BigDecimal amount;
    private IngresoOEgreso type;
    private String categoryId;
    private boolean isNotification; // UI flag only
    private String status; // Logical state (PENDING/APPROVED)
    public static final String STATUS_UNRECOGNIZED = "UNRECOGNIZED";
    
    private boolean expanded;
    
    // Subclass flattened fields
    private String creditCardId;
    private int installments;
    private int currentInstallment;
    private String installmentGroupId;
    private String walletId;
    private String sourcePackageName;
    
    private String rawNotification;

    public TransactionEntity(@NonNull String id, PaymentMethod paymentMethod, String title, String text, 
                             long timestamp, BigDecimal amount, IngresoOEgreso type, String categoryId, 
                             boolean isNotification, String status) {
        this.id = id;
        this.paymentMethod = paymentMethod;
        this.title = title;
        this.text = text;
        this.timestamp = timestamp;
        this.amount = amount;
        this.type = type;
        this.categoryId = categoryId;
        this.isNotification = isNotification;
        this.status = status;
    }

    // Getters and Setters
    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public IngresoOEgreso getType() { return type; }
    public void setType(IngresoOEgreso type) { this.type = type; }

    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }

    public boolean isNotification() { return isNotification; }
    public void setNotification(boolean notification) { isNotification = notification; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isExpanded() { return expanded; }
    public void setExpanded(boolean expanded) { this.expanded = expanded; }

    public String getCreditCardId() { return creditCardId; }
    public void setCreditCardId(String creditCardId) { this.creditCardId = creditCardId; }

    public int getInstallments() { return installments; }
    public void setInstallments(int installments) { this.installments = installments; }

    public int getCurrentInstallment() { return currentInstallment; }
    public void setCurrentInstallment(int currentInstallment) { this.currentInstallment = currentInstallment; }

    public String getInstallmentGroupId() { return installmentGroupId; }
    public void setInstallmentGroupId(String installmentGroupId) { this.installmentGroupId = installmentGroupId; }

    public String getWalletId() { return walletId; }
    public void setWalletId(String walletId) { this.walletId = walletId; }

    public String getSourcePackageName() { return sourcePackageName; }
    public void setSourcePackageName(String sourcePackageName) { this.sourcePackageName = sourcePackageName; }

    public String getRawNotification() { return rawNotification; }
    public void setRawNotification(String rawNotification) { this.rawNotification = rawNotification; }
}
