package com.notificationcapture.app.utils;

/**
 * Custom exceptions for the NotificationCapture app.
 */
public class AppExceptions {

    public static class BaseAppException extends Exception {
        public BaseAppException(String message) {
            super(message);
        }

        public BaseAppException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class DatabaseException extends BaseAppException {
        public DatabaseException(String message) {
            super(message);
        }

        public DatabaseException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class ValidationException extends BaseAppException {
        public ValidationException(String message) {
            super(message);
        }
    }

    public static class MigrationException extends BaseAppException {
        public MigrationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
    
    public static class ConfigurationException extends BaseAppException {
        public ConfigurationException(String message) {
            super(message);
        }
    }
}
