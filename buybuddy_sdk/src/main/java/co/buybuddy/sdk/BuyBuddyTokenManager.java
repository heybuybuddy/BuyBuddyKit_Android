package co.buybuddy.sdk;

import android.annotation.SuppressLint;

import java.util.UUID;

/**
 * Created by Furkan Ençkü on 6/13/17.
 * This code written by buybuddy Android Team
 */

@SuppressLint("ApplySharedPref")
class BuyBuddyTokenManager {

    private String jwt;
    private String token;
    private String apiKey;
    private String apiUser;
    private String uuid;
    private int userId;

    static BuyBuddyTokenManager getCurrent(){
        BuyBuddyTokenManager mToken = new BuyBuddyTokenManager();
        mToken.jwt = BuyBuddyUtil.getSP().getString(BuyBuddyUtil.JWT_KEY, null);
        mToken.token = BuyBuddyUtil.getSP().getString(BuyBuddyUtil.TOKEN_KEY, null);
        mToken.userId = BuyBuddyUtil.getSP().getInt(BuyBuddyUtil.USER_ID, 0);
        mToken.apiKey = BuyBuddyUtil.getSP().getString(BuyBuddyUtil.API_KEY, null);
        mToken.apiUser = BuyBuddyUtil.getSP().getString(BuyBuddyUtil.API_USER, null);
        mToken.uuid = BuyBuddyUtil.getSP().getString(BuyBuddyUtil.USER_UUID, null);

        return mToken;
    }

    public void setApiKeyAndApiUser(String apiKey, String apiUser) {
        String currentApiUser = BuyBuddyUtil.getSP().getString(BuyBuddyUtil.API_USER, null);
        String currentApiKey = BuyBuddyUtil.getSP().getString(BuyBuddyUtil.API_KEY, null);

        if (!apiKey.equals(currentApiKey) || !apiUser.equals(currentApiUser)) {
            String uuid = UUID.randomUUID().toString();

            BuyBuddyUtil.getSP().edit().putString(BuyBuddyUtil.USER_UUID, uuid).commit();
            BuyBuddyUtil.getSP().edit().putString(BuyBuddyUtil.API_KEY, apiKey).commit();
            BuyBuddyUtil.getSP().edit().putString(BuyBuddyUtil.API_USER, apiUser).commit();

            this.uuid = uuid;
        }

        this.apiKey = apiKey;
        this.apiUser = apiUser;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getApiUser() {
        return apiUser;
    }

    public String getUuid() {
        return uuid;
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
