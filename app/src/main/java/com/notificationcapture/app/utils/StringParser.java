package com.notificationcapture.app.utils;

import com.notificationcapture.app.enums.IngresoOEgreso;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringParser {

    /**
     * Detecta automáticamente si es un ingreso o egreso basado en palabras clave
     */
    public static IngresoOEgreso detectTransactionType(String title, String text) {
        String combinedText = (title + " " + text).toLowerCase();

        ConfigManager config = ConfigManager.getInstance();
        java.util.List<String> ingresoKeywords = config.getIngresoKeywords();
        java.util.List<String> egresoKeywords = config.getEgresoKeywords();

        // Verificar ingresos primero
        for (String keyword : ingresoKeywords) {
            if (combinedText.contains(keyword.toLowerCase())) {
                return IngresoOEgreso.INGRESO;
            }
        }

        // Verificar egresos
        for (String keyword : egresoKeywords) {
            if (combinedText.contains(keyword.toLowerCase())) {
                return IngresoOEgreso.EGRESO;
            }
        }

        // Por defecto, si no se detecta, asumimos EGRESO (más común en notificaciones)
        return IngresoOEgreso.EGRESO;
    }

    /**
     * Extrae el monto del texto de la notificación
     * Busca primero en el título y luego en el texto
     */
    public static Double extractAmount(String title, String text) {
        // Intentar extraer del título primero
        Double amount = extractAmountFromString(title);

        // Si no se encontró en el título, buscar en el texto
        if (amount == null) {
            amount = extractAmountFromString(text);
        }

        return amount;
    }

    /**
     * Extrae el monto de una cadena de texto
     */
    public static Double extractAmountFromString(String input) {
        if (input == null || input.isEmpty()) {
            return null;
        }

        String[] patterns = {
                // $1.000,50 o $1.000
                "\\$\\s*(\\d{1,3}(?:\\.\\d{3})*(?:,\\d{2})?)(?!\\d)",
                // $1000,50 (sin separador de miles, con decimales coma)
                "\\$\\s*(\\d+(?:,\\d{2}))(?!\\d)",
                // $1000.50 o $1000
                "\\$\\s*(\\d+(?:\\.\\d{2})?)(?!\\d)",
                // 1.000,50 o 1.000 (sin símbolo)
                "(\\d{1,3}(?:\\.\\d{3})+(?:,\\d{2})?)(?!\\d)",
                // 1000.50 o 1000 (sin símbolo)
                "(\\d+\\.\\d{2})(?!\\d)",
                // Solo números grandes (más de 3 dígitos)
                "(?:^|\\s)(\\d{4,})(?:$|\\s)"
        };

        for (String patternStr : patterns) {
            Pattern pattern = Pattern.compile(patternStr);
            Matcher matcher = pattern.matcher(input);

            if (matcher.find()) {
                String amountStr = matcher.group(1);
                try {
                    if (amountStr.contains(".") && amountStr.contains(",")) {
                        amountStr = amountStr.replace(".", "").replace(",", ".");
                    } else if (amountStr.contains(",")) {
                        amountStr = amountStr.replace(",", ".");
                    } else if (amountStr.contains(".") &&
                            amountStr.substring(amountStr.lastIndexOf(".") + 1).length() == 2) {
                        // Ya está bien
                    } else if (amountStr.contains(".")) {
                        amountStr = amountStr.replace(".", "");
                    }

                    return Double.parseDouble(amountStr);
                } catch (NumberFormatException e) {
                    // Continuar
                }
            }
        }

        return null;
    }
}
