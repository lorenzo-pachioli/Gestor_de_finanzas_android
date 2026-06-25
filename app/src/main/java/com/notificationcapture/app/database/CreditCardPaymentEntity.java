package com.notificationcapture.app.database;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.math.BigDecimal;

@Entity(tableName = "credit_card_payments",
        indices = {@Index(value = {"creditCardId"}), @Index(value = {"startTimestamp"})})
public class CreditCardPaymentEntity {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String creditCardId;
    private long startTimestamp;
    private long endTimestamp;
    private BigDecimal montoTotalResumen;
    private BigDecimal montoPagado;
    private String walletIdPago;
    private long timestampPago;

    // Constuctor
    public CreditCardPaymentEntity(String creditCardId, long startTimestamp, long endTimestamp,
                                   BigDecimal montoTotalResumen, BigDecimal montoPagado,
                                   String walletIdPago, long timestampPago) {
        this.creditCardId = creditCardId;
        this.startTimestamp = startTimestamp;
        this.endTimestamp = endTimestamp;
        this.montoTotalResumen = montoTotalResumen;
        this.montoPagado = montoPagado;
        this.walletIdPago = walletIdPago;
        this.timestampPago = timestampPago;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getCreditCardId() { return creditCardId; }
    public void setCreditCardId(String creditCardId) { this.creditCardId = creditCardId; }

    public long getStartTimestamp() { return startTimestamp; }
    public void setStartTimestamp(long startTimestamp) { this.startTimestamp = startTimestamp; }

    public long getEndTimestamp() { return endTimestamp; }
    public void setEndTimestamp(long endTimestamp) { this.endTimestamp = endTimestamp; }

    public BigDecimal getMontoTotalResumen() { return montoTotalResumen; }
    public void setMontoTotalResumen(BigDecimal montoTotalResumen) { this.montoTotalResumen = montoTotalResumen; }

    public BigDecimal getMontoPagado() { return montoPagado; }
    public void setMontoPagado(BigDecimal montoPagado) { this.montoPagado = montoPagado; }

    public String getWalletIdPago() { return walletIdPago; }
    public void setWalletIdPago(String walletIdPago) { this.walletIdPago = walletIdPago; }

    public long getTimestampPago() { return timestampPago; }
    public void setTimestampPago(long timestampPago) { this.timestampPago = timestampPago; }
}
