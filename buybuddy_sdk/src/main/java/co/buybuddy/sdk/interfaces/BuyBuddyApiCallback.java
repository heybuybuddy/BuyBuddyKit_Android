package co.buybuddy.sdk.interfaces;

import co.buybuddy.sdk.responses.BuyBuddyApiError;
import co.buybuddy.sdk.responses.BuyBuddyApiObject;

/**
 * Created by Furkan Ençkü on 6/13/17.
 * This code written by buybuddy Android Team
 */

public interface BuyBuddyApiCallback<T> {
    void success(BuyBuddyApiObject<T> response);
    void error(BuyBuddyApiError error);
}
