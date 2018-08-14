package co.buybuddy.sdk;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import co.buybuddy.android.BuildConfig;

/**
 * Created by Furkan Ençkü on 6/12/17.
 * This code written by buybuddy Android Team
 */

public final class BuyBuddyUtil {
    private static final String BUYBUDDY_SP_PREFIX = "buybuddy_sharedpref_and";

    static final String TOKEN_KEY = "token";
    static final String JWT_KEY = "jwt";
    static final String USER_ID = "user_id";
    static final String USER_UUID = "uuid";
    static final String API_KEY = "api_key";
    static final String API_USER = "api_user";
    public static final String BLE_CAPABILITY = "ble_capability";
    public static final String BLE_CAPABILITY_ASKED = "ble_capability_asked";

    public static String getFromSP(String key){

        return "";
    }

    public static SharedPreferences getSP(){
        return BuyBuddy.getContext()
                .getSharedPreferences(BUYBUDDY_SP_PREFIX, Context.MODE_PRIVATE);
    }

    static final long HITAG_MANAGER_ALARM_INTERVAL = 60000;
    public static final long HITAG_BLE_SCAN_INTERVAL = 800;
    private volatile static boolean DEBUG = false;

    public static String getSDKVersion() {
        return BuildConfig.VERSION_NAME;
    }

     public static void printD(String tag, String message){
        if (DEBUG && message != null && tag != null){
            if (tag.length() > 13){
                tag = tag.substring(0, 13);
            }
            Log.d("*bbsdk* " + tag, message);
        }
    }

    public static String isValidPatternForHitag(String hitagId){

        String regexStr = "([A-Za-z]{4})([0-9]{5})";

        if(hitagId.matches(regexStr)){
            return hitagId.replace("-","");
        }

        return null;
    }

    public static void setDEBUG(boolean debug){
        DEBUG = debug;
    }

    public static String w(Object obj) { // Write if object is not null
        return obj != null ? obj.toString() : "__NULL__";
    }
}
