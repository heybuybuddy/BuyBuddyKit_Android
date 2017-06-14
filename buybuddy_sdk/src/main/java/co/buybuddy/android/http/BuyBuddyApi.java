package co.buybuddy.android.http;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;

import co.buybuddy.android.http.model.BuyBuddyApiCallback;
import co.buybuddy.android.http.model.BuyBuddyApiError;
import co.buybuddy.android.http.model.BuyBuddyApiObject;
import co.buybuddy.android.http.model.BuyBuddyBase;
import co.buybuddy.android.http.model.IncompleteSale;
import co.buybuddy.android.http.model.OrderDelegate;
import co.buybuddy.android.http.model.OrderDelegateDetail;
import co.buybuddy.android.model.BuyBuddyItem;
import co.buybuddy.android.model.CollectedHitag;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by furkan on 6/13/17.
 * Gururla sunar. AHAHAHAHA Some spagetties
 */

public final class BuyBuddyApi {

    private static BuyBuddyApi _instance;
    private boolean isSandBoxMode = false;
    private MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    final OkHttpClient client;
    private final BuyBuddyAuthorization authorization;

    BuyBuddyApi(){
        authorization = new BuyBuddyAuthorization();
        client = new OkHttpClient.Builder()
                .addInterceptor(authorization)
                .build();
    }

    public static BuyBuddyApi getSharedInstance() {
        if (_instance == null) {
            _instance = new BuyBuddyApi();
            return _instance;
        } else {
            return _instance;
        }
    }


    public BuyBuddyApi setSandBoxMode(boolean isSandBoxMode){
        this.isSandBoxMode = isSandBoxMode;
        return this;
    }

    public BuyBuddyApi setUserToken(String token){
        authorization.updateToken(token);
        return this;
    }

    public boolean isSandBoxMode(){
        return isSandBoxMode;
    }

    private <T> void call(final Class<T> clazz, BuyBuddyHttpModel requestModel, final BuyBuddyApiCallback<T> delegate){

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
            public void onResponse(Call call, Response response) throws IOException {
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
                        delegate.success(responseObject);
                    } catch (JsonSyntaxException ex) {
                        delegate.error(new BuyBuddyApiError("-0000", "JsonSyntaxException", response.code()));
                    }

                } else{

                    try{
                        BuyBuddyBase base = new Gson().fromJson(responseStr, BuyBuddyBase.class);
                        delegate.error(base.getErrors());
                    }catch(JsonSyntaxException ex){
                        delegate.error(new BuyBuddyApiError("-0001", "JsonSyntaxException", response.code()));
                    }
                }
            }
        });
    }

    private <T> T call(Class<T> clazz, BuyBuddyHttpModel requestModel) throws BuyBuddyApiError, IOException {

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

        Response response = new OkHttpClient().newCall(builder.build()).execute();
        String responseStr = null;

        if (response.body() != null){
            responseStr = response.body().string();
        }else {
            throw new BuyBuddyApiError("-0002", "Response Body Null", response.code());
        }

        if (response.isSuccessful()){

            try{
                GsonBuilder gsonBuilder = new GsonBuilder();
                BuyBuddyApiObject<T> responseObject = gsonBuilder.create().fromJson(responseStr, getType(BuyBuddyApiObject.class, clazz));
                return responseObject.getData();
            } catch (Exception ex) {
                throw new BuyBuddyApiError("-0003", "JsonSyntaxException", response.code());
            }

        } else{

            try{
                BuyBuddyBase base = new Gson().fromJson(responseStr, BuyBuddyBase.class);
                throw new BuyBuddyApiError(base.getErrors().getTraceCode() + "", base.getErrors().getTraceMessage(), response.code());
            }catch(JsonSyntaxException ex){
                throw new BuyBuddyApiError("-0004", "JsonSyntaxException", response.code());
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

    void getJwt(BuyBuddyApiCallback<BuyBuddyJwt> delegate) {
        call(BuyBuddyJwt.class,
             BuyBuddyEndpoint.endPointCreator(BuyBuddyEndpoint.Jwt, null),
             delegate);
    }

    public void postScanRecord(CollectedHitag[] hitags, BuyBuddyApiCallback<BuyBuddyBase> delegate) {
        call(BuyBuddyBase.class,
             BuyBuddyEndpoint.endPointCreator(BuyBuddyEndpoint.ScanHitag, new ParameterMap().add("scan_record", hitags)),
             delegate);
    }

    public void getProductWithHitagId(String hitagId, BuyBuddyApiCallback<BuyBuddyItem> delegate) {
        call(BuyBuddyItem.class,
             BuyBuddyEndpoint.endPointCreator(BuyBuddyEndpoint.QrHitag, new ParameterMap().add("hitag_id", hitagId)),
             delegate);
    }

    public void getOrderDetail(long saleId, BuyBuddyApiCallback<OrderDelegateDetail> delegate) {
        call(OrderDelegateDetail.class,
                BuyBuddyEndpoint.endPointCreator(BuyBuddyEndpoint.OrderDetail,
                        new ParameterMap().add("sale_id", saleId)),
                delegate);
    }

    void completeOrder(long saleId, String hitagId, int status, BuyBuddyApiCallback<BuyBuddyBase> delegate){
        call(BuyBuddyBase.class,
                BuyBuddyEndpoint.endPointCreator(BuyBuddyEndpoint.HitagCompletion,
                        new ParameterMap().add("sale_id", saleId)
                                          .add("compile_id", hitagId)
                                          .add("hitag_completion", new ParameterMap().add("status", status).getMap())),
                delegate);
    }

    public void getIncompleteOrders(BuyBuddyApiCallback<IncompleteSale> delegate){
        call(IncompleteSale.class,
                BuyBuddyEndpoint.endPointCreator(BuyBuddyEndpoint.HitagIncomplete, null),
                delegate);
    }

    void validateOrder(long saleId, Map<String, Integer> hitagValidations, BuyBuddyApiCallback<HitagPassword> delegate){
        call(HitagPassword.class,
                BuyBuddyEndpoint.endPointCreator(BuyBuddyEndpoint.OrderCompletion,
                                                 new ParameterMap().add("sale_id", saleId)
                                                                   .add("hitag_release_params", new ParameterMap().add("hitags", hitagValidations).getMap())),
                delegate);
    }

    public void createOrder(String hitagIds, float sub_total, BuyBuddyApiCallback<OrderDelegate> delegate){
        call(OrderDelegate.class,
             BuyBuddyEndpoint.endPointCreator(BuyBuddyEndpoint.OrderDelegate, new ParameterMap().add("order_delegate", new ParameterMap()
                                                                                                            .add("hitags", hitagIds)
                                                                                                            .add("sub_total", sub_total).getMap())),
             delegate);
    }

    BuyBuddyJwt getJwt(String token) throws BuyBuddyApiError, IOException {
        return call(BuyBuddyJwt.class,
                    BuyBuddyEndpoint.endPointCreator(BuyBuddyEndpoint.Jwt, new ParameterMap().add("passphrase_submission", new ParameterMap()
                                                                                                                                    .add("passkey", token).getMap())));
    }
}
