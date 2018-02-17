package co.buybuddy.sdk;

import android.annotation.SuppressLint;

/**
 * Created by Furkan Ençkü on 6/13/17.
 * This code written by buybuddy Android Team
 */

@SuppressLint("ApplySharedPref")
class BuyBuddyTokenManager {

    private String jwt;
    private String token;
    private int userId;

    static BuyBuddyTokenManager getCurrent(){
        BuyBuddyTokenManager mToken = new BuyBuddyTokenManager();
        mToken.jwt = BuyBuddyUtil.getSP().getString(BuyBuddyUtil.JWT_KEY, null);
        mToken.token = BuyBuddyUtil.getSP().getString(BuyBuddyUtil.TOKEN_KEY, null);
        mToken.userId = BuyBuddyUtil.getSP().getInt(BuyBuddyUtil.USER_ID, 0);

        return mToken;
    }

    public int getUserId() {
        return userId;
    }

    void setJwt(String jwt, int userId){
        this.jwt = jwt;
        this.userId = userId;
        setUserIdToDisk(userId);
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
                .commit();
    }

    public static void setUserIdToDisk(int userId) {
        BuyBuddyUtil.getSP().edit()
                .putInt(BuyBuddyUtil.USER_ID, userId)
                .commit();
    }

    private static void setTokenToDisk(String token){
         BuyBuddyUtil.getSP().edit()
                .putString(BuyBuddyUtil.TOKEN_KEY, token)
                .commit();
    }
}
