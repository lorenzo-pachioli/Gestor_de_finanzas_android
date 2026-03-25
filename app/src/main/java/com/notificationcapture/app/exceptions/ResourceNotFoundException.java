package com.notificationcapture.app.exceptions;

/**
 * Excepción lanzada cuando falta un recurso (ej. archivo de mocks).
 */
public class ResourceNotFoundException extends BaseAppException {
    public ResourceNotFoundException(String resourceName) {
        super("No se pudo encontrar el recurso: " + resourceName);
    }
}
