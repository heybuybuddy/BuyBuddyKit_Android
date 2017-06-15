package co.buybuddy.android;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;

/**
 * Created by furkan on 6/12/17.
 * Gururla sunar. AHAHAHAHA Some spagetties
 */

public class BuyBuddy {
    private static Context mContext;
    private static BuyBuddy _instance;
    public BuyBuddyApi api;

    private BuyBuddy(){
        api = new BuyBuddyApi();
    }

    public static void setContext(Context context){
        mContext = context;
    }

    public static Context getContext(){
        return mContext;
    }

    public static BuyBuddy getInstance(){
        synchronized (Object.class){
            return _instance;
        }
    }

    public static void sdkInitialize(@NonNull Context context){
        if (_instance == null){
            mContext = context;
            _instance = new BuyBuddy();
        }

        mContext.startService(new Intent(getContext(), HitagScanService.class));
    }
}
