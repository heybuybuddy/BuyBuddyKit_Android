package co.buybuddy.sdk.ble.exception;

/**
 * Created by Furkan Ençkü on 9/12/17.
 * This code written by buybuddy Android Team
 */

public class BleException extends RuntimeException {

    public BleException() {
        super();
    }

    public BleException(String message) {
        super(message);
    }

    public BleException(Throwable throwable) {
        super(throwable);
    }

    String toStringCauseIfExists() {
        Throwable throwableCause = getCause();
        return (throwableCause != null ? ", cause=" + throwableCause.toString() : "");
    }
}