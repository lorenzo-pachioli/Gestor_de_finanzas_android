package com.notificationcapture.app.enums;

public enum IngresoOEgreso {
    INGRESO (false),
    EGRESO (true);

    private final boolean valor;
    IngresoOEgreso(boolean valor) {
        this.valor = valor;
    }

    public static IngresoOEgreso getTransactionType(Boolean valor){
        return valor ? EGRESO : INGRESO;
    }

    public static String getTypeIndicator(IngresoOEgreso type){
        return type == INGRESO ? "+" : "-";
    }
}
