package co.buybuddy.sdk.responses;

/**
 * Created by Furkan Ençkü on 6/13/17.
 * This code written by buybuddy Android Team
 */

public class BuyBuddyBase {

    private BuyBuddyApiError errors;
    private int statusCode = 0;

    public int getStatusCode(){
        return statusCode;
    }

    public BuyBuddyBase setStatusCode(int statusCode) {
        this.statusCode = statusCode;
        return this;
    }

    public BuyBuddyBase setErrors(BuyBuddyApiError errors) {
        this.errors = errors;
        return this;
    }

    public BuyBuddyApiError getErrors() {
        return errors.setResponsecode(statusCode);
    }
}
