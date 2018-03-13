package co.buybuddy.sdk.ble.btleasyncgatt;

/**
 * Created by Furkan on 12.03.2018.
 */

public class BtleAsyncGattError extends Error {

    public BtleAsyncGattError(String cause) {
        super.initCause(new Throwable(cause));
    }
}
