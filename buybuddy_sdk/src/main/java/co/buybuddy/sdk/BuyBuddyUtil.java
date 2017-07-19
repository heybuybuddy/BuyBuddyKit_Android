package co.buybuddy.sdk;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * Created by furkan on 6/12/17.
 * Gururla sunar. AHAHAHAHA Some spagetties
 */

public final class BuyBuddyUtil {
    private static final String BUYBUDDY_SP_PREFIX = "buybuddy_sharedpref_and";

    static final String TOKEN_KEY = "token";
    static final String JWT_KEY = "jwt";

    public static String getFromSP(String key){

        return "";
    }

    static SharedPreferences getSP(){
        return BuyBuddy.getInstance().getContext()
                .getSharedPreferences(BUYBUDDY_SP_PREFIX, Context.MODE_PRIVATE);
    }

    static final long HITAG_MANAGER_ALARM_INTERVAL = 60000;
    public static final long HITAG_BLE_SCAN_INTERVAL = 800;
    private static boolean DEBUG = true;


     static void printD(String tag, String message){
        if (DEBUG){
            if (tag.length() > 10){
                tag = tag.substring(0, 10);
            }
            Log.d("*bbddysdk* " + tag, message);
        }
    }

    public static String isValidPatternForHitag(String hitagId){

        String regexStr = "([0-9A-Fa-f]{2})[-]([0-9A-Fa-f]{4})[-]([0-9A-Fa-f]{4})";

        if(hitagId.matches(regexStr)){
            return hitagId.replace("-","");
        }

        return null;
    }

    public static void setDEBUG(boolean debug){
        DEBUG = debug;
    }

    static String w(Object obj){ // Write if object is not null
        return obj != null ? obj.toString() : "__NULL__";
    }
}
