package co.buybuddy.sdk.util;

import android.location.LocationManager;


/**
 * Created by Furkan Ençkü on 9/13/17.
 * This code written by buybuddy Android Team
 */

public class CheckerLocationProvider {

    private final LocationManager locationManager;

    public CheckerLocationProvider(LocationManager locationManager) {
        this.locationManager = locationManager;
    }

    public boolean isLocationProviderEnabled() {
        return locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }
}