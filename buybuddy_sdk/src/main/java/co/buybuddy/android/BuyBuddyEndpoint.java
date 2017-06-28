package co.buybuddy.android;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by furkan on 6/13/17.
 * Gururla sunar. AHAHAHAHA Some spagetties
 */

class BuyBuddyEndpoint {

    static final String QrHitag          = "GET /iot/scan/<hitag_id>";
    static final String ScanHitag        = "POST /iot/scan_record";
    static final String Jwt              = "POST /iam/users/tokens";
    static final String OrderDelegate    = "POST /order/delegate";
    static final String HitagCompletion  = "PUT /order/overview/<sale_id>/hitag_completion/<compile_id>";
    static final String OrderCompletion  = "POST /order/delegate/<sale_id>/hitag_release";
    static final String HitagIncomplete  = "GET /order/uncompleted";
    static final String OrderDetail      = "GET /order/overview/<sale_id>/detail";

    private static final String sandBoxPrefix = "sandbox-api";
    private static final String productionPrefix = "api";


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
                case "GET" : currentMethod = HttpMethodBB.GET; break;
                case "POST" : currentMethod = HttpMethodBB.POST; break;
                case "PUT" : currentMethod = HttpMethodBB.PUT; break;
                default: currentMethod = HttpMethodBB.GET;
            }
        }

        String parsedEndpoint = endpoint.replaceFirst(currentType + " ", "");

        try {
            if (params != null){
                Map<String, Object> placeholders = new HashMap<>();
                Map<String, Object> jsonBody;
                Map<String, Object> parameters = params.getMap();
                jsonBody = parameters;

                if (params.getMap() != null){

                    Iterator<String> iter = jsonBody.keySet().iterator();

                    while (iter.hasNext()) {
                        String key = iter.next();

                        String pattern = "<" + key + ">";
                        String replacement = parameters.get(key) + "";

                        if (parsedEndpoint.contains(pattern)){
                            parsedEndpoint = parsedEndpoint.replace(pattern, replacement);
                            placeholders.put(key, parameters.get(key));
                            iter.remove();
                        }
                    }

                    JSONObject json = new JSONObject(jsonBody);
                    jsonString = json.toString();
                }
            }
        }catch (Exception ex) {
            ex.printStackTrace();
        }



        return new BuyBuddyHttpModel(getBaseUrl() + parsedEndpoint, jsonString, currentMethod);
    }

    public static String getBaseUrl() {

        return "https://" +
                (BuyBuddy.getInstance().api.isSandBoxMode() ?
                        sandBoxPrefix : productionPrefix) + ".buybuddy.co";

    }
}

