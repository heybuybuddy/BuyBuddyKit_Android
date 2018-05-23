package co.buybuddy.sdk.responses;

/**
 * Created by Furkan Ençkü on 6/13/17.
 * This code written by buybuddy Android Team
 */

public class BuyBuddyApiObject<T> extends BuyBuddyBase {
    private T data;

    public T getData() {
        return data;
    }
}
