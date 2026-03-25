package com.notificationcapture.app.exceptions;

/**
 * Excepción lanzada cuando no se puede determinar si es ingreso o egreso.
 */
public class InvalidTransactionTypeException extends ParserException {
    public InvalidTransactionTypeException(String context) {
        super("No se pudo determinar el tipo de transacción en el contexto: " + context);
    }
}
