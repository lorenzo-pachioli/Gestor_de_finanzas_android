package com.notificationcapture.app.exceptions;

/**
 * Excepción lanzada cuando el origen de la notificación no coincide con ninguna wallet conocida.
 */
public class UnknownWalletException extends ParserException {
    public UnknownWalletException(String packageName) {
        super("La aplicación '" + packageName + "' no está configurada como una billetera válida.");
    }
}
