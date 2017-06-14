package co.buybuddy.android.http;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import co.buybuddy.android.BuyBuddy;

/**
 * Created by furkan on 6/12/17.
 * Gururla sunar. AHAHAHAHA Some spagetties
 */

final class BuyBuddyUtil {
    private static final String BUYBUDDY_SP_PREFIX = "buybuddy_sharedpref_and";

    static final String TOKEN_KEY = "token";
    static final String JWT_KEY = "jwt";

    public static String getFromSP(String key){

        return "";
    }

    static SharedPreferences getSP(){
        return BuyBuddy.getSharedInstance().getContext()
                .getSharedPreferences(BUYBUDDY_SP_PREFIX, Context.MODE_PRIVATE);
    }

    public static final long HITAG_MANAGER_ALARM_INTERVAL = 60000;
    public static final long HITAG_BLE_SCAN_INTERVAL = 800;
    private static boolean DEBUG = true;
    public static void printD(String tag, String message){
        if (DEBUG){
            Log.d(tag, message);
        }
    }

    public static void setDEBUG(boolean debug){
        DEBUG = debug;
    }
}
