package com.notificationcapture.app.exceptions;

/**
 * Excepción lanzada cuando los archivos de configuración están mal formados.
 */
public class InvalidConfigurationException extends BaseAppException {
    public InvalidConfigurationException(String detail) {
        super("Configuración inválida: " + detail);
    }
}
