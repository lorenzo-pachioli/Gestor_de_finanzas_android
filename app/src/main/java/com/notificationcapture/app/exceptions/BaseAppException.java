package com.notificationcapture.app.exceptions;

/**
 * Clase base para todas las excepciones de la aplicación.
 */
public class BaseAppException extends Exception {
    public BaseAppException(String message) {
        super(message);
    }

    public BaseAppException(String message, Throwable cause) {
        super(message, cause);
    }
}
