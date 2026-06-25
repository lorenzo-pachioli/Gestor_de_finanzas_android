package com.notificationcapture.app.models;

import java.math.BigDecimal;

public class ResumenDeudaTarjeta {
    private final String creditCardId;
    private final String cardName;
    private final BigDecimal gastosDelMes;
    private final BigDecimal arrastreAnterior;
    private final BigDecimal pagosDelMes;
    private final long mesStart;
    private final long mesEnd;

    public ResumenDeudaTarjeta(String creditCardId, String cardName,
                               BigDecimal gastosDelMes, BigDecimal arrastreAnterior,
                               BigDecimal pagosDelMes, long mesStart, long mesEnd) {
        this.creditCardId = creditCardId;
        this.cardName = cardName;
        this.gastosDelMes = gastosDelMes;
        this.arrastreAnterior = arrastreAnterior;
        this.pagosDelMes = pagosDelMes;
        this.mesStart = mesStart;
        this.mesEnd = mesEnd;
    }

    public BigDecimal getDeudaTotal() {
        BigDecimal deuda = gastosDelMes.add(arrastreAnterior).subtract(pagosDelMes);
        return deuda.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : deuda;
    }

    public String getCreditCardId() { return creditCardId; }
    public String getCardName() { return cardName; }
    public BigDecimal getGastosDelMes() { return gastosDelMes; }
    public BigDecimal getArrastreAnterior() { return arrastreAnterior; }
    public BigDecimal getPagosDelMes() { return pagosDelMes; }
    public long getMesStart() { return mesStart; }
    public long getMesEnd() { return mesEnd; }
}
