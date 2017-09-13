package co.buybuddy.sdk;

/**
 * Created by Furkan Ençkü on 6/13/17.
 * This code written by buybuddy Android Team
 */

class BuyBuddyJwt {

    private String jwt;
    private int exp;
    private int user_id;
    private int passphrase_id;

    String getJwt(){
        return jwt;
    }
}
