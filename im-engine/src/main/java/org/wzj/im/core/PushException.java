package org.wzj.im.core;

/**
 * Created by wens on 15-11-11.
 */
public class PushException extends RuntimeException {

    public PushException() {
    }

    public PushException(String message) {
        super(message);
    }

    public PushException(String message, Throwable cause) {
        super(message, cause);
    }

    public PushException(Throwable cause) {
        super(cause);
    }

    public PushException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
