package co.buybuddy.sdk.ble.exception;

/**
 * Created by Furkan Ençkü on 9/12/17.
 * This code written by buybuddy Android Team
 */

public class HitagReleaserException extends RuntimeException {

    public HitagReleaserException() {
        super();
    }

    public HitagReleaserException(String message) {
        super(message);
    }

    public HitagReleaserException(Throwable throwable) {
        super(throwable);
    }

    String toStringCauseIfExists() {
        Throwable throwableCause = getCause();
        return (throwableCause != null ? ", cause=" + throwableCause.toString() : "");
    }
}