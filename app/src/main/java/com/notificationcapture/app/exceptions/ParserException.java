package com.notificationcapture.app.exceptions;

/**
 * Clase base para errores de procesamiento de texto.
 */
public class ParserException extends BaseAppException {
    public ParserException(String message) {
        super(message);
    }

    public ParserException(String message, Throwable cause) {
        super(message, cause);
    }
}
