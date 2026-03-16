package com.notificationcapture.app.utils;

/**
 * A generic wrapper for repository and service results.
 * @param <T> The type of data returned on success.
 */
public class AppResult<T> {

    public interface Consumer<T> {
        void accept(T value);
    }

    private final T data;
    private final Exception error;

    private AppResult(T data, Exception error) {
        this.data = data;
        this.error = error;
    }

    public static <T> AppResult<T> success(T data) {
        return new AppResult<>(data, null);
    }

    public static <T> AppResult<T> failure(Exception error) {
        return new AppResult<>(null, error);
    }

    public boolean isSuccess() {
        return error == null;
    }

    public T getData() {
        return data;
    }

    public Exception getError() {
        return error;
    }

    public void onFailure(Consumer<Exception> action) {
        if (!isSuccess() && action != null) {
            action.accept(error);
        }
    }

    public void onSuccess(Consumer<T> action) {
        if (isSuccess() && action != null) {
            action.accept(data);
        }
    }
}
