package com.notificationcapture.app.exceptions;

/**
 * Excepción para errores relacionados con la base de datos.
 */
public class DatabaseException extends BaseAppException {
    public DatabaseException(String message) {
        super(message);
    }

    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
