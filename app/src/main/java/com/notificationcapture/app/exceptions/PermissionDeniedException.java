package com.notificationcapture.app.exceptions;

/**
 * Excepción lanzada cuando el usuario no otorga permisos de notificación.
 */
public class PermissionDeniedException extends BaseAppException {
    public PermissionDeniedException() {
        super("Permiso de acceso a notificaciones denegado por el usuario.");
    }
}
