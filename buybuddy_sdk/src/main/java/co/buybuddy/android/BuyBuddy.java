package co.buybuddy.android;

import android.content.Context;

/**
 * Created by furkan on 6/12/17.
 * Gururla sunar. AHAHAHAHA Some spagetties
 */

public class BuyBuddy {
    private Context mContext;

    private static BuyBuddy _instance;

    private BuyBuddy(){

    }

    public void setContext(Context context){
        mContext = context;
    }

    public Context getContext(){
        return mContext;
    }

    public static BuyBuddy getSharedInstance() throws RuntimeException{
        if (_instance == null){
            throw new RuntimeException("You should first initialize.");
        }
        return _instance;
    }

    public static void sdkInitialize(Context context){
        if (_instance == null){
            synchronized (Object.class) {
                _instance = new BuyBuddy();
            }
        }

        _instance.mContext = context;
    }
}
