package com.notificationcapture.app.exceptions;

/**
 * Excepción lanzada cuando no se encuentra una entidad (Wallet, Transacción, etc.).
 */
public class EntityNotFoundException extends DatabaseException {
    public EntityNotFoundException(String entityName, String identifier) {
        super("No se encontró " + entityName + " con el identificador: " + identifier);
    }
}
