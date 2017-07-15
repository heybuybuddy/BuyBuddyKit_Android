package co.buybuddy.sdk.responses;

/**
 * Created by furkan on 6/13/17.
 * Gururla sunar. AHAHAHAHA Some spagetties
 */

public class BuyBuddyApiObject<T> extends BuyBuddyBase {
    private T data;

    public T getData() {
        return data;
    }
}
