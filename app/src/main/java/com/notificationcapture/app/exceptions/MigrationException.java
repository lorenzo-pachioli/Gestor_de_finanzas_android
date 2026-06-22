package com.notificationcapture.app.exceptions;

/**
 * Excepción lanzada durante errores en la migración de base de datos.
 */
public class MigrationException extends DatabaseException {
    public MigrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
