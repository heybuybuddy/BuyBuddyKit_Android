package co.buybuddy.sdk;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v4.net.ConnectivityManagerCompat;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Map;

import co.buybuddy.sdk.ble.CollectedHitag;
import co.buybuddy.sdk.interfaces.BuyBuddyApiCallback;
import co.buybuddy.sdk.interfaces.BuyBuddyUserTokenExpiredDelegate;
import co.buybuddy.sdk.location.BuyBuddyStore;
import co.buybuddy.sdk.model.Address;
import co.buybuddy.sdk.model.BuyBuddyBasketCampaign;
import co.buybuddy.sdk.model.HitagPasswordPayload;
import co.buybuddy.sdk.responses.BuyBuddyApiError;
import co.buybuddy.sdk.responses.BuyBuddyApiObject;
import co.buybuddy.sdk.responses.BuyBuddyBase;
import co.buybuddy.sdk.responses.IncompleteSale;
import co.buybuddy.sdk.responses.OrderDelegate;
import co.buybuddy.sdk.responses.OrderDelegateDetail;
import co.buybuddy.sdk.model.BuyBuddyItem;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.CertificatePinner;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by Furkan Ençkü on 6/13/17.
 * This code written by buybuddy Android Team
 */

public final class BuyBuddyApi {

    private boolean isSandBoxMode = false;
    private MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    final OkHttpClient client;
    private final BuyBuddyAuthorization authorization;
    private CertificatePinner certificatePinner;

    BuyBuddyApi(){
        authorization = new BuyBuddyAuthorization();


        // Renewed until 08/2019
        certificatePinner = new CertificatePinner.Builder()
                .add("buybuddy.co", "sha256/Pr78qk12sCmHlBm9p2NC8k9h3qN0q+Yx5/Zf8QwCP7I=")
                .add("buybuddy.co", "sha256/JSMzqOOrtyOT1kmau6zKhgT676hGgczD5VMdRMyJZFA=")
                .add("buybuddy.co", "sha256/++MBgDH5WGvL9Bcn5Be30cRcL0f5O+NyoXuWtQdX1aI=")
                .add("buybuddy.co", "sha256/KwccWaCgrnaw6tsrrSO61FgLacNgG2MMLq8GE6+oP5I=")
                .build();

        client = new OkHttpClient.Builder()
                .certificatePinner(certificatePinner)
                .addInterceptor(authorization)
                .build();
    }

    public BuyBuddyApi setInvalidationTokenDelegate(BuyBuddyUserTokenExpiredDelegate delegate) {
        authorization.setUserTokenExpiredDelegate(delegate);
        return this;
    }

    public BuyBuddyApi setSandBoxMode(boolean isSandBoxMode){
        this.isSandBoxMode = isSandBoxMode;
        return this;
    }

    public BuyBuddyApi setPublicAuthParameters(String apiKey, String apiUser) {
        authorization.setApiKeyApiUser(apiKey, apiUser);
        return this;
    }

    public BuyBuddyApi setUserToken(String token){
        authorization.updateToken(token);
        return this;
    }

    private boolean isConnected() {

        ConnectivityManager cm = (ConnectivityManager) BuyBuddy.getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

        if (activeNetwork != null) {
            return activeNetwork.isConnectedOrConnecting();
        }

        return false;
    }

    public boolean isSandBoxMode(){
        return isSandBoxMode;
    }

    private <T> void call(final Class<T> clazz, BuyBuddyHttpModel requestModel, final BuyBuddyApiCallback<T> delegate){

        if (!isConnected()) {
            delegate.error(new BuyBuddyApiError("-1", "Network is not available.", -1));

            return;
        }

        Request.Builder builder = new Request.Builder()
                                 .url(requestModel.getUrl());

        switch (requestModel.getMethodType()){
            case POST:
                RequestBody requestBodyPost = RequestBody.create(JSON, requestModel.getJson());
                builder.post(requestBodyPost);
                break;
            case PUT:
                RequestBody requestBodyPUT = RequestBody.create(JSON, requestModel.getJson());
                builder.put(requestBodyPUT);
                break;
        }

        client.newCall(builder.build()).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) {
                String responseStr = null;
                boolean error = false;

                if (response.body() != null){
                    try {
                        responseStr = response.body().string();
                    }catch (IOException ex){
                        ex.printStackTrace();
                        error = true;
                    }
                }

                if (response.isSuccessful()){

                    try{
                        GsonBuilder gsonBuilder = new GsonBuilder();
                        BuyBuddyApiObject<T> responseObject = gsonBuilder.create().fromJson(responseStr, getType(BuyBuddyApiObject.class, clazz));
                        if (responseObject != null) {
                            responseObject.setStatusCode(response.code());
                        }

                        BuyBuddyUtil.printD("Api", responseStr);

                        if(delegate != null)
                            delegate.success(responseObject);
                    } catch (JsonSyntaxException ex) {
                        if(delegate != null)
                            delegate.error(new BuyBuddyApiError("-0000", "JsonSyntaxException", response.code()));
                    }

                } else{

                    try{
                        BuyBuddyBase base = new Gson().fromJson(responseStr, BuyBuddyBase.class);

                        BuyBuddyUtil.printD("Api", responseStr);

                        if (base != null) {
                            base.setStatusCode(response.code());
                            if(delegate != null)
                                delegate.error(base.getErrors());
                        }
                    }catch(JsonSyntaxException ex){
                        if(delegate != null)
                            delegate.error(new BuyBuddyApiError("-0001", "JsonSyntaxException", response.code()));
                    }
                }
            }
        });
    }

    private <T> BuyBuddyApiObject<T> call(Class<T> clazz, BuyBuddyHttpModel requestModel) throws BuyBuddyApiError, IOException {

        Request.Builder builder = new Request.Builder()
                .url(requestModel.getUrl());

        switch (requestModel.getMethodType()){
            case POST:
                RequestBody requestBodyPost = RequestBody.create(JSON, requestModel.getJson());
                builder.post(requestBodyPost);
                break;
            case PUT:
                RequestBody requestBodyPUT = RequestBody.create(JSON, requestModel.getJson());
                builder.put(requestBodyPUT);
                break;
        }

        OkHttpClient client = new OkHttpClient.Builder()
                                    .certificatePinner(certificatePinner)
                                    .build();

        Response response = client.newCall(builder.build()).execute();

        for (Certificate certificate : response.handshake().peerCertificates()) {
            try {
                System.out.println(CertificatePinner.pin(certificate));
            }catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        String responseStr = null;

        if (response.body() != null){
            responseStr = response.body().string();
        }else {
            throw new BuyBuddyApiError("-0002", "HitagState Body Null", response.code());
        }

        if (response.isSuccessful()){

            try{
                GsonBuilder gsonBuilder = new GsonBuilder();
                return gsonBuilder.create().fromJson(responseStr, getType(BuyBuddyApiObject.class, clazz));
            } catch (Exception ex) {
                //throw new BuyBuddyApiError("-0003", "JsonSyntaxException", response.code());
                return null;
            }

        } else{

            try{
                GsonBuilder gsonBuilder = new GsonBuilder();
                return gsonBuilder.create().fromJson(responseStr, getType(BuyBuddyApiObject.class, clazz));
            }catch(JsonSyntaxException ex){
                //throw new BuyBuddyApiError("-0004", "JsonSyntaxException", response.code());
                return null;
            }
        }
    }

    Type getType(final Class<?> rawClass, final Class<?> parameter) {
        return new ParameterizedType() {
            @Override
            public Type[] getActualTypeArguments() {
                return new Type[] {parameter};
            }
            @Override
            public Type getRawType() {
                return rawClass;
            }
            @Override
            public Type getOwnerType() {
                return null;
            }
        };
    }

    public void getHitagPassword(String hitagId, long saleId, int version, BuyBuddyApiCallback<HitagPasswordPayload> delegate) {
        call(HitagPasswordPayload.class,
             BuyBuddyEndpoint.endPointCreator(BuyBuddyEndpoint.HitagPasswordPayload,
                        new ParameterMap().add("sale_id", saleId)
                                          .add("hitag_release_params", new ParameterMap().add("hitags", new ParameterMap().add(hitagId, version)
                                                                                                                          .getMap())
                                                                                         .getMap())),
             delegate);
    }

    void getJwt(BuyBuddyApiCallback<BuyBuddyJwt> delegate) {
        call(BuyBuddyJwt.class,
             BuyBuddyEndpoint.endPointCreator(BuyBuddyEndpoint.Jwt, null),
             delegate);
    }

    void postScanRecord(ArrayList<CollectedHitag> hitags, BuyBuddyApiCallback<BuyBuddyBase> delegate) {
        call(BuyBuddyBase.class,
             BuyBuddyEndpoint.endPointCreator(BuyBuddyEndpoint.ScanHitag, new ParameterMap().add("scan_record", hitags)),
             delegate);
    }

    public void getProductWithHitagId(String hitagId, BuyBuddyApiCallback<BuyBuddyItem> delegate) {
        call(BuyBuddyItem.class,
             BuyBuddyEndpoint.endPointCreator(BuyBuddyEndpoint.QrHitag, new ParameterMap().add("hitag_id", hitagId)),
             delegate);
    }

    public void getOrderDetail(long orderId, BuyBuddyApiCallback<OrderDelegateDetail> delegate) {
        call(OrderDelegateDetail.class,
                BuyBuddyEndpoint.endPointCreator(BuyBuddyEndpoint.OrderDetail,
                        new ParameterMap().add("sale_id", orderId)),
                delegate);
    }

    public void getStoreInfo(String compiledIdentifier, BuyBuddyApiCallback<BuyBuddyStore> delegate) {
        call(BuyBuddyStore.class,
             BuyBuddyEndpoint.endPointCreator(BuyBuddyEndpoint.GetStoreInfo,
                                              new ParameterMap().add("hitag_id", compiledIdentifier)),
                                              delegate);
    }

    public void getUserAddress(BuyBuddyApiCallback<Address> delegate){

        call(Address.class,
                BuyBuddyEndpoint.endPointCreator(BuyBuddyEndpoint.UserAddressDetails,
                        new ParameterMap().add("user_id", BuyBuddyTokenManager.getCurrent().getUserId())),
                delegate);
    }

    public void deleteUserAddress(Integer addressId,BuyBuddyApiCallback<BuyBuddyBase> delegate){

        call(BuyBuddyBase.class,
                BuyBuddyEndpoint.endPointCreator(BuyBuddyEndpoint.DeleteUserAddress,
                        new ParameterMap().add("user_id",BuyBuddyTokenManager.getCurrent().getUserId())
                                          .add("address_id",addressId)),
                delegate);
    }

    public void createUserAddress(Address address,BuyBuddyApiCallback<Address> delegate){
        if (address.getCity() != null && address.getTitle() != null && address.getStreet() != null && address.getRegion() != null && address.getZipcode() != null && address.getDefinition() != null && address.getCountry() != null) {
        call(Address.class,
                BuyBuddyEndpoint.endPointCreator(BuyBuddyEndpoint.CreateUserAddress,
                        new ParameterMap().add("user_id", BuyBuddyTokenManager.getCurrent().getUserId())
                                          .add("address",new ParameterMap().add("name", address.getTitle())
                                                                           .add("street", address.getStreet())
                                                                           .add("region", address.getRegion())
                                                                           .add("city",address.getCity())
                                                                           .add("zip",address.getZipcode())
                                                                           .add("address",address.getDefinition())
                                                                           .add("country",address.getCountry()).getMap())),
                delegate);
        }
    }

    public void updateUserAddress(Address address,BuyBuddyApiCallback<Address> delegate){
        if (address.getCity() != null && address.getTitle() != null && address.getStreet() != null && address.getRegion() != null && address.getZipcode() != null && address.getDefinition() != null && address.getCountry() != null) {

            call(Address.class,
                BuyBuddyEndpoint.endPointCreator(BuyBuddyEndpoint.UpdateUserAddress,
                        new ParameterMap().add("user_id", BuyBuddyTokenManager.getCurrent().getUserId())
                                          .add("address",new ParameterMap()
                                                            .add("name", address.getTitle())
                                                            .add("street", address.getStreet())
                                                            .add("region", address.getRegion())
                                                            .add("city",address.getCity())
                                                            .add("zip",address.getZipcode())
                                                            .add("address",address.getDefinition())
                                                            .add("country",address.getCountry()).getMap())),
                delegate);
        }
    }

    public void setUserEmail(String email, BuyBuddyApiCallback<BuyBuddyBase> delegate) {
        call(BuyBuddyBase.class,
                BuyBuddyEndpoint.endPointCreator(BuyBuddyEndpoint.UserEmailAssignment,
                        new ParameterMap().add("user_id", BuyBuddyTokenManager.getCurrent().getUserId())
                                          .add("email_assignment", new ParameterMap().add("email", email).getMap())),
                delegate);
    }

    public void completeOrder(long orderId, String hitagId, int status, BuyBuddyApiCallback<BuyBuddyBase> delegate){
        call(BuyBuddyBase.class,
                BuyBuddyEndpoint.endPointCreator(BuyBuddyEndpoint.HitagCompletion,
                        new ParameterMap().add("sale_id", orderId)
                                          .add("compile_id", hitagId)
                                          .add("hitag_completion", new ParameterMap().add("status", status).getMap())),
                delegate);
    }

    public void getIncompleteOrders(BuyBuddyApiCallback<IncompleteSale> delegate){
        call(IncompleteSale.class,
                BuyBuddyEndpoint.endPointCreator(BuyBuddyEndpoint.HitagIncomplete, null),
                delegate);
    }

    void validateOrder(long orderId, Map<String, Integer> hitagValidations, BuyBuddyApiCallback<HitagPassword> delegate){
        call(HitagPassword.class,
                BuyBuddyEndpoint.endPointCreator(BuyBuddyEndpoint.OrderCompletion,
                                                 new ParameterMap().add("sale_id", orderId)
                                                                   .add("hitag_release_params", new ParameterMap().add("hitags", hitagValidations).getMap())),
                delegate);
    }

    void createOrder(int[] hitagIds, int[] campaingIds,float sub_total,Address address,String email,String governmentId,BuyBuddyApiCallback<OrderDelegate> delegate){

        call(OrderDelegate.class,
             BuyBuddyEndpoint.endPointCreator(BuyBuddyEndpoint.OrderDelegate, new ParameterMap().add("order_delegate", new ParameterMap()
                                                                                                            .add("campaigns", campaingIds)
                                                                                                            .add("hitags", hitagIds)
                                                                                                            .add("sub_total", sub_total)
                                                                                                            .add("address",new ParameterMap().add("name",address.getTitle())
                                                                                                                                             .add("street",address.getStreet())
                                                                                                                                             .add("region",address.getRegion())
                                                                                                                                             .add("city",address.getCity())
                                                                                                                                             .add("zipcode",address.getZipcode())
                                                                                                                                             .add("address",address.getDefinition())
                                                                                                                                             .add("country",address.getCountry()).getMap())
                                                                                                            .add("email",email)
                                                                                                            .add("gId",governmentId).getMap())),
             delegate);
    }

    void getCampaigns(int[] hitagIds, BuyBuddyApiCallback<BuyBuddyBasketCampaign> delegate) {

        BuyBuddyHttpModel model =
                BuyBuddyEndpoint.endPointCreator(BuyBuddyEndpoint.GetCampaings, new ParameterMap().add("hitag_ids", hitagIds).add("basket", false));

        call(BuyBuddyBasketCampaign.class, model, delegate);

    }

    BuyBuddyApiObject<BuyBuddyJwt> getPublicAuthJwt(String apiKey,
                                                    String apiUser,
                                                    String bundleIdentifier,
                                                    String UUID) throws BuyBuddyApiError, IOException {
        return call(BuyBuddyJwt.class,
                BuyBuddyEndpoint.endPointCreator(BuyBuddyEndpoint.Jwt, new ParameterMap().add("public_auth_submission", new ParameterMap()
                        .add("api_key", apiKey)
                        .add("api_user", apiUser)
                        .add("bundle_identifier", bundleIdentifier)
                        .add("uuid", UUID)
                        .getMap())));
    }

    BuyBuddyApiObject<BuyBuddyJwt> getJwt(String token) throws BuyBuddyApiError, IOException {
        return call(BuyBuddyJwt.class,
                    BuyBuddyEndpoint.endPointCreator(BuyBuddyEndpoint.Jwt, new ParameterMap().add("passphrase_submission", new ParameterMap()
                                                                                                                                    .add("passkey", token)
                                                                                                                                    .add("bundle_identifier", BuyBuddy.getContext().getPackageName())
                            .getMap())));
    }
}
