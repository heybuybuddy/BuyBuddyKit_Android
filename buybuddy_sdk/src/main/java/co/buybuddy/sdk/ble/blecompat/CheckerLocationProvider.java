package co.buybuddy.sdk.ble.blecompat;

import android.location.LocationManager;

/**
 * Created by furkan on 10/2/17.
 * Gururla sunar. AHAHAHAHA Some spagetties
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