package com.notificationcapture.app.enums;

import android.graphics.Color;

public enum CatColors {
    // Colores Cálidos (Ingresos)
    INGRESO_TEAL(Color.parseColor("#009688")),
    INGRESO_GREEN(Color.parseColor("#4CAF50")),
    INGRESO_LIGHT_GREEN(Color.parseColor("#8BC34A")),
    INGRESO_LIME(Color.parseColor("#CDDC39")),
    INGRESO_YELLOW(Color.parseColor("#FFEB3B")),
    INGRESO_ORANGE(Color.parseColor("#FF9800")),
    INGRESO_AMBER(Color.parseColor("#FFC107")),
    INGRESO_BROWN(Color.parseColor("#795548")),

    // Colores Fríos (Egresos)
    EGRESO_RED(Color.parseColor("#F44336")),
    EGRESO_PINK(Color.parseColor("#E91E63")),
    EGRESO_DEEP_ORANGE(Color.parseColor("#FF5722")),
    EGRESO_BLUE(Color.parseColor("#2196F3")),
    EGRESO_LIGHT_BLUE(Color.parseColor("#03A9F4")),
    EGRESO_CYAN(Color.parseColor("#00BCD4")),
    EGRESO_INDIGO(Color.parseColor("#3F51B5")),
    EGRESO_DEEP_PURPLE(Color.parseColor("#673AB7")),
    EGRESO_PURPLE(Color.parseColor("#9C27B0")),
    EGRESO_BLUE_GREY(Color.parseColor("#607D8B")),
    EGRESO_GREY(Color.parseColor("#9E9E9E"));

    private final int color;

    CatColors(int color) {
        this.color = color;
    }

    public int getColor() {
        return color;
    }

    // Metodo estático para obtener todos los colores de INGRESOS como array
    public static int[] getIntIngresosColors() {
        return new int[] {
                INGRESO_TEAL.color,
                INGRESO_GREEN.color,
                INGRESO_LIGHT_GREEN.color,
                INGRESO_LIME.color,
                INGRESO_YELLOW.color,
                INGRESO_ORANGE.color,
                INGRESO_AMBER.color,
                INGRESO_BROWN.color
        };
    }

    public static CatColors[] getIngresosColors() {
        return new CatColors[] {
                INGRESO_TEAL,
                INGRESO_GREEN,
                INGRESO_LIGHT_GREEN,
                INGRESO_LIME,
                INGRESO_YELLOW,
                INGRESO_ORANGE,
                INGRESO_AMBER,
                INGRESO_BROWN
        };
    }

    // Metodo estático para obtener todos los colores de EGRESOS como array
    public static int[] getIntEgresosColors() {
        return new int[] {
                EGRESO_RED.color,
                EGRESO_PINK.color,
                EGRESO_DEEP_ORANGE.color,
                EGRESO_BLUE.color,
                EGRESO_LIGHT_BLUE.color,
                EGRESO_CYAN.color,
                EGRESO_INDIGO.color,
                EGRESO_DEEP_PURPLE.color,
                EGRESO_PURPLE.color,
                EGRESO_BLUE_GREY.color,
                EGRESO_GREY.color
        };
    }

    public static CatColors[] getEgresosColors() {
        return new CatColors[] {
                EGRESO_RED,
                EGRESO_PINK,
                EGRESO_DEEP_ORANGE,
                EGRESO_BLUE,
                EGRESO_LIGHT_BLUE,
                EGRESO_CYAN,
                EGRESO_INDIGO,
                EGRESO_DEEP_PURPLE,
                EGRESO_PURPLE,
                EGRESO_BLUE_GREY,
                EGRESO_GREY
        };
    }

    // Metodo para obtener colores según el tipo de transacción
    public static int[] getColorsByType(IngresoOEgreso type) {
        return type == IngresoOEgreso.INGRESO ? getIntIngresosColors() : getIntEgresosColors();
    }

    public static int getOneIntColorByType(IngresoOEgreso type, int color) {
        return type == IngresoOEgreso.INGRESO ? getIntIngresosColors()[color] : getIntEgresosColors()[color];
    }

    public static CatColors getOneColorByType(IngresoOEgreso type, int color) {
        return type == IngresoOEgreso.INGRESO ? getIngresosColors()[color] : getEgresosColors()[color];
    }

    // Metodo para obtener un CatColors desde un valor de color int
    public static CatColors fromColor(int colorValue) {
        for (CatColors catColor : CatColors.values()) {
            if (catColor.color == colorValue) {
                return catColor;
            }
        }
        // Si no se encuentra, retornar el primer color de ingresos como default
        return INGRESO_TEAL;
    }
}
