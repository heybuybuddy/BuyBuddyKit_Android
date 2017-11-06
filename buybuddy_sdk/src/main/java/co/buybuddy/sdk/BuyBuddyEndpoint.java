package co.buybuddy.sdk;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.gson.Gson;

import org.json.JSONObject;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by Furkan Ençkü on 6/13/17.
 * This code written by buybuddy Android Team
 */

class BuyBuddyEndpoint {

    static final String QrHitag              = "GET /iot/scan/<hitag_id>";
    static final String ScanHitag            = "POST /iot/scan_record";
    static final String Jwt                  = "POST /iam/users/tokens";
    static final String OrderDelegate        = "POST /order/delegate";
    static final String HitagCompletion      = "PUT /order/overview/<sale_id>/hitag_completion/<compile_id>";
    static final String OrderCompletion      = "POST /order/delegate/<sale_id>/hitag_release";
    static final String HitagIncomplete      = "GET /order/uncompleted";
    static final String OrderDetail          = "GET /order/overview/<sale_id>/detail";
    static final String HitagPasswordPayload = "POST /order/delegate/<sale_id>/hitag_release";
    static final String GetCampaings         = "GET /sales/current_campaign?{hitag_ids}&{basket}";

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

                        String queryParemeterPattern = "{" + key + "}";

                        if (parsedEndpoint.contains(pattern)){
                            parsedEndpoint = parsedEndpoint.replace(pattern, replacement);
                            placeholders.put(key, parameters.get(key));
                            iter.remove();
                        }else if (parsedEndpoint.contains(queryParemeterPattern)) {

                            if (parameters.get(key) != null) {
                                if (parameters.get(key) instanceof int[]) {

                                    int[] objArray = (int[]) parameters.get(key);
                                    String objArrayReplacement = "";
                                    for (int i = 0; i < objArray.length; i++) {
                                        objArrayReplacement += key + "[]=" + objArray[i];

                                        if (i < objArray.length - 1 ){
                                            objArrayReplacement += "&";
                                        }
                                    }
                                    parsedEndpoint = parsedEndpoint.replace(queryParemeterPattern, objArrayReplacement) + "&";

                                } else {

                                    String objReplecement = key + "=" + parameters.get(key);
                                    parsedEndpoint = parsedEndpoint.replace(queryParemeterPattern, objReplecement) + "&";
                                }
                            }
                        }
                    }

                    JSONObject json = new JSONObject(new Gson().toJson(jsonBody));
                    jsonString = json.toString();

                }
            }
        }catch (Exception ex) {
            ex.printStackTrace();
        }

        return new BuyBuddyHttpModel(getBaseUrl() + parsedEndpoint, jsonString, currentMethod);
    }

    public static String getBaseUrl() {
        return "https://" + (BuyBuddy.getInstance().api.isSandBoxMode() ?
                sandBoxPrefix : productionPrefix) + ".buybuddy.co";
    }
}

