package com.forkingcode.bluetoothcompat;

import android.os.Build;

/**
 * Created by furkan on 10/2/17.
 * Gururla sunar. AHAHAHAHA Some spagetties
 */

public class LocationServicesStatus {

    private final CheckerLocationProvider checkerLocationProvider;
    private final CheckerLocationPermission checkerLocationPermission;
    private final boolean isAndroidWear;
    private final int deviceSdk;
    private final int targetSdk;

    public LocationServicesStatus(
            CheckerLocationProvider checkerLocationProvider,
            CheckerLocationPermission checkerLocationPermission,
            int deviceSdk,
            int targetSdk,
            boolean isAndroidWear
    ) {
        this.checkerLocationProvider = checkerLocationProvider;
        this.checkerLocationPermission = checkerLocationPermission;
        this.deviceSdk = deviceSdk;
        this.targetSdk = targetSdk;
        this.isAndroidWear = isAndroidWear;
    }

    public boolean isLocationPermissionOk() {
        return !isLocationPermissionGrantedRequired() || checkerLocationPermission.isLocationPermissionGranted();
    }

    public boolean isLocationProviderOk() {
        return !isLocationProviderEnabledRequired() || checkerLocationProvider.isLocationProviderEnabled();
    }

    private boolean isLocationPermissionGrantedRequired() {
        return deviceSdk >= Build.VERSION_CODES.M;
    }

    /**
     * A function that returns true if the location services may be needed to be turned ON. Since there are no official guidelines
     * for Android Wear check is disabled.
     *
     * @see <a href="https://code.google.com/p/android/issues/detail?id=189090">Google Groups Discussion</a>
     * @return true if Location Services need to be turned ON
     */
    private boolean isLocationProviderEnabledRequired() {
        return !isAndroidWear
                && targetSdk >= Build.VERSION_CODES.M
                && deviceSdk >= Build.VERSION_CODES.M;
    }
}