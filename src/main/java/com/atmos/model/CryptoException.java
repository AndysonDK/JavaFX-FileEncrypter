package com.atmos.model;

public class CryptoException extends Exception {

    /**
     * An Exception...
     *
     * @param message a {@code String} that contains the descriptive message of what might have gone wrong
     * @param throwable so the exception can keep the stacktrace of the original exceptions
     */
    CryptoException(String message, Throwable throwable) {
        super(message, throwable);
    }

    CryptoException(String message) {
        super(message);
    }
}
