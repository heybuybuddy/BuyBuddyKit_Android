package co.buybuddy.android;

import android.annotation.SuppressLint;

/**
 * Created by furkan on 6/13/17.
 * Gururla sunar. AHAHAHAHA Some spagetties
 */

@SuppressLint("ApplySharedPref")
class BuyBuddyTokenManager {

    private String jwt;
    private String token;

    static BuyBuddyTokenManager getCurrent(){
        BuyBuddyTokenManager mToken = new BuyBuddyTokenManager();
        mToken.jwt = BuyBuddyUtil.getSP().getString(BuyBuddyUtil.JWT_KEY, null);
        mToken.token = BuyBuddyUtil.getSP().getString(BuyBuddyUtil.TOKEN_KEY, null);

        return mToken;
    }

    void setJwt(String jwt){
        this.jwt = jwt;
        setJwtToDisk(jwt);
    }

    void setToken(String token){
        this.token = token;
        setTokenToDisk(token);
    }

    String getToken(){
        return token;
    }

    String getJwt(){
        return jwt;
    }

    private static void setJwtToDisk(String jwt){
         BuyBuddyUtil.getSP().edit()
                .putString(BuyBuddyUtil.JWT_KEY, jwt)
                .apply();
    }

    private static void setTokenToDisk(String token){
         BuyBuddyUtil.getSP().edit()
                .putString(BuyBuddyUtil.TOKEN_KEY, token)
                .apply();
    }
}
