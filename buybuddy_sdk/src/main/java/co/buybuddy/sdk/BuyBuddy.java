package co.buybuddy.sdk;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;

import com.polidea.rxandroidble.RxBleClient;
import co.buybuddy.sdk.util.BuyBuddyError;

/**
 * Created by furkan on 6/12/17.
 */

public class BuyBuddy {
    private static Context mContext;
    private static BuyBuddy _instance;
    public final BuyBuddyApi api;
    public final RxBleClient client;
    public final BuyBuddyShoppingCartManager shoppingCart;

    private BuyBuddy(){
        api = new BuyBuddyApi();
        client = RxBleClient.create(getContext());
        shoppingCart = new BuyBuddyShoppingCartManager();
    }

    public static void setContext(Context context){
        mContext = context;
    }

    public static Context getContext(){
        return mContext;
    }

    public static BuyBuddy getInstance(){

        if (mContext == null)
            throw new RuntimeException(new BuyBuddyError());

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

        long periodSecs = 30L; // the task should be executed every 30 seconds
        long flexSecs = 15L; // the task can run as early as -15 seconds from the scheduled time
    }
}
