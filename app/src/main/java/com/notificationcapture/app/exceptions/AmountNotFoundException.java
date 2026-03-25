package com.notificationcapture.app.exceptions;

/**
 * Excepción lanzada cuando no se puede extraer el monto de una notificación de pago.
 */
public class AmountNotFoundException extends ParserException {
    public AmountNotFoundException(String text) {
        super("No se pudo detectar un monto válido en el texto: " + text);
    }
}
