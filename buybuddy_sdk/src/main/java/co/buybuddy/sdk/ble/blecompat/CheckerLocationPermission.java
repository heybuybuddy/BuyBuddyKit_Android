package co.buybuddy.sdk.ble.blecompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Process;

/**
 * Created by furkan on 10/2/17.
 * Gururla sunar. AHAHAHAHA Some spagetties
 */

public class CheckerLocationPermission {

    private final Context context;

    public CheckerLocationPermission(Context context) {
        this.context = context;
    }

    boolean isLocationPermissionGranted() {
        return isPermissionGranted(Manifest.permission.ACCESS_COARSE_LOCATION)
                || isPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION);
    }

    /**
     * Copied from android.support.v4.content.ContextCompat for backwards compatibility
     * @param permission the permission to check
     * @return true is granted
     */
    private boolean isPermissionGranted(String permission) {
        if (permission == null) {
            throw new IllegalArgumentException("permission is null");
        }

        return context.checkPermission(permission, Process.myPid(), Process.myUid()) == PackageManager.PERMISSION_GRANTED;
    }
}