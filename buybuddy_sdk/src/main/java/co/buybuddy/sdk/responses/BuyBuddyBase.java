package co.buybuddy.sdk.responses;

/**
 * Created by furkan on 6/13/17.
 * Gururla sunar. AHAHAHAHA Some spagetties
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

    public BuyBuddyApiError getErrors() {
        return errors.setResponsecode(statusCode);
    }
}
