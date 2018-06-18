package co.buybuddy.sdk.location;

/**
 * Created by furkan on 2/18/18.
 * Gururla sunar. AHAHAHAHA Some spagetties
 */

public interface BuyBuddyStoreInfoDelegate {
    void enterRegion(BuyBuddyStore store);
    void activeRegion(BuyBuddyStore store);
    void exitRegion(BuyBuddyStore store);
}
