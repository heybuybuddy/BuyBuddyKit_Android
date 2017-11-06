package co.buybuddy.sdk;

import android.util.Log;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.UUID;

import co.buybuddy.sdk.interfaces.BuyBuddyUserTokenExpiredDelegate;
import co.buybuddy.sdk.responses.BuyBuddyApiError;
import co.buybuddy.sdk.responses.BuyBuddyApiObject;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

class BuyBuddyAuthorization implements Interceptor{

    private static String TAG = "BuyBuddyAuthorization";
    private BuyBuddyTokenManager tokenManager;
    private BuyBuddyUserTokenExpiredDelegate delegate;

    public BuyBuddyAuthorization(){
        this.tokenManager = BuyBuddyTokenManager.getCurrent();
    }

    public void updateToken(String token){
        tokenManager.setToken(token);
    }

    public void setUserTokenExpiredDelegate(BuyBuddyUserTokenExpiredDelegate delegate){
        this.delegate = delegate;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();

        UUID uuid = UUID.randomUUID();

        //Build new request
        Request.Builder builder = request.newBuilder();
        builder.header("Accept", "application/json"); //if necessary, say to consume JSON

        //save token of this request for future

        String jwt = tokenManager.getJwt();
        if (jwt == null){
            synchronized (this) {
                BuyBuddyUtil.printD(TAG, "SYNCRONIZED BLOCK !");
                refreshJwt();
            }
        }

        setAuthHeader(builder); //write current token to request
        request = builder.build(); //overwrite old request
        Response response = chain.proceed(request);

         //perform request, here original request will be executed
        if (response.code() == 401) { //if unauthorized
            BuyBuddyUtil.printD(TAG, "UNAUTHORIZED REQ: " + uuid.toString() + "\n" + "token" + tokenManager.getJwt());
            synchronized (this) { //perform all 401 in sync blocks, to avoid multiply token updates
                String currentJwt = tokenManager.getJwt(); //get currently stored token

                if(currentJwt != null && currentJwt.equals(jwt)) { //compare current token with token that was stored before, if it was not updated - do update

                    int code = refreshJwt() / 100; //refresh token
                    if(code != 2) { //if refresh token failed for some reason
                        if(code == 4) //only if response is 400, 500 might mean that token was not updated
                            if (delegate != null)
                                delegate.tokenExpired();
                        return response; //if token refresh failed - show error to user
                    }else {

                        BuyBuddyUtil.printD(TAG, "REFRESHED TOKEN: " + uuid.toString() + "\n" + "token" + tokenManager.getJwt());

                        setAuthHeader(builder);
                        request = builder.build();

                        response = chain.proceed(request);

                        BuyBuddyUtil.printD(TAG, "REFRESHED RESPONSE: " + response.code());

                        return response;
                    }
                }
            }
        }

        return response;
    }

    private void setAuthHeader(Request.Builder builder) {
        if (tokenManager.getJwt() != null) //Add Auth token to each request if authorized
        {
            builder.header("Authorization", String.format("Bearer %s", tokenManager.getJwt()));
        }
    }

    private int refreshJwt() {
        try {
            BuyBuddyApiObject<BuyBuddyJwt> jwt = BuyBuddy.getInstance().api.getJwt(tokenManager.getToken());

            if (jwt != null){
                if (jwt.getData() != null && jwt.getData().getJwt() != null)
                    tokenManager.setJwt(jwt.getData().getJwt());
                return 200;
            }
            return 400;

        } catch (BuyBuddyApiError buyBuddyApiError) {
            buyBuddyApiError.printStackTrace();
            return 400;
        } catch (IOException e) {
            e.printStackTrace();
            return 400;
        }
    }

}
