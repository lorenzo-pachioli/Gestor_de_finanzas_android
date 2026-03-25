package com.notificationcapture.app.exceptions;

/**
 * Excepción lanzada cuando se intenta registrar una wallet que ya existe.
 */
public class DuplicateWalletException extends DatabaseException {
    public DuplicateWalletException(String packageName) {
        super("La wallet con el paquete '" + packageName + "' ya está registrada.");
    }
}
