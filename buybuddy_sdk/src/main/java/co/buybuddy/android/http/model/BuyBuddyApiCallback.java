package co.buybuddy.android.http.model;

/**
 * Created by furkan on 6/13/17.
 * Gururla sunar. AHAHAHAHA Some spagetties
 */

public interface BuyBuddyApiCallback<T> {
    void success(BuyBuddyApiObject<T> response);
    void error(BuyBuddyApiError error);
}
