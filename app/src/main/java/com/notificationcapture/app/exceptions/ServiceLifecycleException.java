package com.notificationcapture.app.exceptions;

/**
 * Excepción para errores en el ciclo de vida del servicio NotificationListener.
 */
public class ServiceLifecycleException extends BaseAppException {
    public ServiceLifecycleException(String message, Throwable cause) {
        super(message, cause);
    }
}
