package com.notificationcapture.app.models;

public class ResumenDeudaTarjeta {
    private final String creditCardId;
    private final String cardName;
    private final double gastosDelMes; // egresos reales del período
    private final double arrastreAnterior; // suma de arrastres con mesDestino = mes actual
    private final double pagosDelMes; // pagos registrados para este período
    private final long mesStart;
    private final long mesEnd;

    public ResumenDeudaTarjeta(String creditCardId, String cardName,
                                double gastosDelMes, double arrastreAnterior,
                                double pagosDelMes, long mesStart, long mesEnd) {
        this.creditCardId = creditCardId;
        this.cardName = cardName;
        this.gastosDelMes = gastosDelMes;
        this.arrastreAnterior = arrastreAnterior;
        this.pagosDelMes = pagosDelMes;
        this.mesStart = mesStart;
        this.mesEnd = mesEnd;
    }

    // Cálculo centralizado — única fuente de verdad
    public double getDeudaTotal() {
        double deuda = gastosDelMes + arrastreAnterior - pagosDelMes;
        return deuda < 0 ? 0 : deuda;
    }

    // Getters
    public String getCreditCardId() { return creditCardId; }
    public String getCardName() { return cardName; }
    public double getGastosDelMes() { return gastosDelMes; }
    public double getArrastreAnterior() { return arrastreAnterior; }
    public double getPagosDelMes() { return pagosDelMes; }
    public long getMesStart() { return mesStart; }
    public long getMesEnd() { return mesEnd; }

}
