package co.buybuddy.android.http;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;


import static co.buybuddy.android.http.HttpMethodBB.GET;
import static co.buybuddy.android.http.HttpMethodBB.POST;
import static co.buybuddy.android.http.HttpMethodBB.PUT;

/**
 * Created by furkan on 6/13/17.
 * Gururla sunar. AHAHAHAHA Some spagetties
 */

class BuyBuddyEndpoint {

    public static final String QrHitag          = "GET /iot/scan/<hitag_id>";
    public static final String ScanHitag        = "POST /iot/scan_record";
    public static final String Jwt              = "POST /iam/users/tokens";
    public static final String OrderDelegate    = "POST /order/delegate";
    public static final String HitagCompletion  = "PUT /order/overview/<sale_id>/hitag_completion/<compile_id>";
    public static final String OrderCompletion  = "POST /order/delegate/<sale_id>/hitag_release";
    public static final String HitagIncomplete  = "GET /order/uncompleted";
    public static final String OrderDetail      = "GET /order/overview/<sale_id>/detail";

    public static final String sandBoxPrefix = "sandbox-api";
    public static final String productionPrefix = "api";


    private static final String startingTypes[] = {"GET", "POST", "PUT"};

    public static BuyBuddyHttpModel endPointCreator(@NonNull String endpoint,
                                                    @Nullable ParameterMap params){

        String currentType = null;
        HttpMethodBB currentMethod = null;
        String jsonString = null;

        for (String type : startingTypes){
            if (endpoint.contains(type)){
                currentType = type;
                break;
            }
        }

        if (currentType == null) {
            return null;
        } else {
            switch (currentType){
                case "GET" : currentMethod = GET; break;
                case "POST" : currentMethod = POST; break;
                case "PUT" : currentMethod = PUT; break;
                default: currentMethod = GET;
            }
        }

        String parsedEndpoint = endpoint.replaceFirst(currentType + " ", "");

        if (params != null){
            Map<String, Object> placeholders = new HashMap<>();
            Map<String, Object> parameters = params.getMap();

            if (params.getMap() != null){
                for (String key : parameters.keySet()){
                    String pattern = "<" + key + ">";
                    String replacement = parameters.get(key) + "";

                    if (parsedEndpoint.contains(pattern)){
                        parsedEndpoint = parsedEndpoint.replace(pattern, replacement);
                        placeholders.put(key, parameters.get(key));
                        parameters.remove(key);
                    }
                }

                try{
                    JSONObject json = new JSONObject(parameters);
                    jsonString = json.toString();
                }catch (Exception ex){
                    ex.printStackTrace();
                }
            }
        }

        return new BuyBuddyHttpModel(getBaseUrl() + parsedEndpoint, jsonString, currentMethod);
    }

    public static String getBaseUrl() {

        return "https://" +
                (BuyBuddyApi.getSharedInstance().isSandBoxMode() ?
                        sandBoxPrefix : productionPrefix) + ".buybuddy.co";

    }
}

