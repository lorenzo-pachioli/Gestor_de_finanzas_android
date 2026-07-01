package com.notificationcapture.app.constants;

public final class CategoryConstants {
    private CategoryConstants() {}

    public static final String OTHER_INCOME_ID = "other_income";
    public static final String OTHER_OUTCOME_ID = "other_outcome";
    public static final String PAGO_TARJETA_ID = "pago_tarjeta";
    public static final String TRANSFER_IN_ID = "transfer_in";
    public static final String TRANSFER_OUT_ID = "transfer_out";

    public static final String OTHER_DISPLAY_NAME = "Otros";

    public static final String[] DEFAULT_OUTCOME_CATEGORIES = {
            OTHER_DISPLAY_NAME, "Comida", "Combustible", "Transporte", "Servicios",
            "Entretenimiento", "Salud", "Educación", "Compras", "Vivienda", "Pago de Tarjeta"
    };

    public static final String[] DEFAULT_INCOME_CATEGORIES = {
            OTHER_DISPLAY_NAME, "Salario", "Inversiones", "Reembolsos", "Familiares", "Venta"
    };
}
