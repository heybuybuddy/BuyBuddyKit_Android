package co.buybuddy.sdk;

import android.arch.lifecycle.AndroidViewModel;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Build;
import android.support.annotation.NonNull;

import org.greenrobot.eventbus.EventBus;

import co.buybuddy.sdk.ble.BuyBuddyHitagReleaser;
import co.buybuddy.sdk.util.BuyBuddyError;
import co.buybuddy.sdk.util.CheckerLocationPermission;
import co.buybuddy.sdk.util.CheckerLocationProvider;
import co.buybuddy.sdk.util.LocationServicesStatus;

/**
 * Created by Furkan Ençkü on 6/12/17.
 */

public class BuyBuddy {
    private static Context mContext;
    private static BuyBuddy _instance;
    public final BuyBuddyApi api;
    public final BuyBuddyShoppingCartManager shoppingCart;
    private LocationServicesStatus locationServicesStatus;
    private final BuyBuddyStoreInfoProvider storeInfoProvider;

    public BuyBuddyStoreInfoProvider getStoreInfoProvider() {
        return storeInfoProvider;
    }

    private BuyBuddy(){
        api = new BuyBuddyApi();
        shoppingCart = new BuyBuddyShoppingCartManager();

        locationServicesStatus = new LocationServicesStatus( new CheckerLocationProvider( (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE)),
                                                             new CheckerLocationPermission(getContext()), android.os.Build.VERSION.SDK_INT, getContext().getApplicationInfo().targetSdkVersion, false);

        storeInfoProvider = new BuyBuddyStoreInfoProvider();
    }

    public LocationServicesStatus getLocationServicesStatus() {
        return locationServicesStatus;
    }

    public static void setContext(Context context){
        mContext = context;
    }

    public static Context getContext(){
        return mContext;
    }

    public static BuyBuddy getInstance(){

        if (mContext != null){
            synchronized (Object.class){
                return _instance;
            }
        } else {
            return null;
        }
    }

    public static void sdkInitialize(@NonNull Context context){
        if (_instance == null){
            mContext = context;
            _instance = new BuyBuddy();
        }

        try {
            EventBus.builder().throwSubscriberException(false).installDefaultEventBus();
        }catch (RuntimeException ex) {
            ex.printStackTrace();
        }

        if (!(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2)){
            // BuyBuddyScanner can not work properly under android version 4.3
            return;
        }else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            mContext.startService(new Intent(getContext(), HitagScanService.class));
            mContext.stopService(new Intent(getContext(), BuyBuddyHitagReleaser.class));
        }
    }
}
