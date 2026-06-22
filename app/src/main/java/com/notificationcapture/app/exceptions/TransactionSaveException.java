package com.notificationcapture.app.exceptions;

/**
 * Excepción para errores al guardar transacciones.
 */
public class TransactionSaveException extends DatabaseException {
    public TransactionSaveException(String message, Throwable cause) {
        super(message, cause);
    }
}
