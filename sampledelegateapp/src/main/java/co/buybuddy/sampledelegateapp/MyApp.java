package co.buybuddy.sampledelegateapp;

import android.app.Application;

import co.buybuddy.sdk.BuyBuddy;

/**
 * Created by furkan on 9/13/17.
 * Gururla sunar. AHAHAHAHA Some spagetties
 */

public class MyApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        BuyBuddy.sdkInitialize(this);
    }
}
