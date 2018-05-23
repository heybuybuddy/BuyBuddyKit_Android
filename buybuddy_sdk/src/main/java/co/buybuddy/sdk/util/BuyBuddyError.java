package co.buybuddy.sdk.util;

/**
 * Created by Furkan Ençkü on 6/22/17.
 * This code written by buybuddy Android Team
 */

public class BuyBuddyError extends Exception {
    @Override
    public String getMessage() {
        return "BuyBuddy SDK initialize first";
    }
}
