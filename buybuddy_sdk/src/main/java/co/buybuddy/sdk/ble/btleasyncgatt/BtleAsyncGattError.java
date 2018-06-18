package co.buybuddy.sdk.ble.btleasyncgatt;

/**
 * Created by Furkan on 12.03.2018.
 */

class BtleAsyncGattError extends Error {

    BtleAsyncGattError(String cause) {
        super.initCause(new Throwable(cause));
    }
}
