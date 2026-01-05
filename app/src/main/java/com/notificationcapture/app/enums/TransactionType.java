package com.notificationcapture.app.enums;

public enum TransactionType {
    INGRESO (false),
    EGRESO (true);

    private final boolean valor;
    TransactionType(boolean valor) {
        this.valor = valor;
    }

    public static TransactionType getTransactionType(Boolean valor){
        return valor ? EGRESO : INGRESO;
    }

    public static String getTypeIndicator(TransactionType type){
        return type == INGRESO ? "+" : "-";
    }
}
