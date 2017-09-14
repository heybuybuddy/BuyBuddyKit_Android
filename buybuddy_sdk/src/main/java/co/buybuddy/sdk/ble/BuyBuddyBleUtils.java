package co.buybuddy.sdk.ble;

import android.os.Build;
import android.util.Log;

import co.buybuddy.sdk.ble.exception.HitagReleaserBleException;

public class BuyBuddyBleUtils {

    public static final String MAIN_PREFIX = "0000beef";
    public static final String MAIN_POSTFIX = "-6275-7962-7564-647966656565";

    public static final String HITAG_TX = "00007373";
    public static final String HITAG_RX = "00007478";

    public static long HITAG_SCAN_INTERVAL_IDLE = 1000L;
    public static long HITAG_SCAN_BETWEEN_INTERVAL_IDLE = 30000L;

    public static long HITAG_SCAN_INTERVAL_ACTIVE = 1000L;
    public static long HITAG_SCAN_BETWEEN_INTERVAL_ACTIVE = 800L;

    public final static int HITAG_TYPE_CUSTOM = 6;
    public final static int HITAG_TYPE_BEACON = 3;

    public static void handBleScanExeption(HitagReleaserBleException bleScanException) {

        switch (bleScanException.getReason()) {
            case HitagReleaserBleException.BLUETOOTH_NOT_AVAILABLE:
                Log.d("ERROR", "Bluetooth is not available");
                break;
            case HitagReleaserBleException.BLUETOOTH_DISABLED:
                Log.d("ERROR", "Enable bluetooth and try again");
                break;
            case HitagReleaserBleException.LOCATION_PERMISSION_MISSING:
                Log.d("ERROR", "On Android 6.0 and above location permission is required");
                break;
            case HitagReleaserBleException.LOCATION_SERVICES_DISABLED:
                Log.d("ERROR", "Location services needs to be enabled on Android 6.0 and above");
                break;
            case HitagReleaserBleException.SCAN_FAILED_ALREADY_STARTED:
                Log.d("ERROR", "Scan with the same filters is already started");
                break;
            case HitagReleaserBleException.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED:
                Log.d("ERROR", "Failed to register application for bluetooth scan");
                break;
            case HitagReleaserBleException.SCAN_FAILED_FEATURE_UNSUPPORTED:
                Log.d("ERROR", "Scan with specified parameters is not supported");
                break;
            case HitagReleaserBleException.SCAN_FAILED_INTERNAL_ERROR:
                Log.d("ERROR", "Scan failed due to internal error");
                break;
            case HitagReleaserBleException.SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES:
                Log.d("ERROR", "Scan cannot start due to limited hardware resources");
                break;
            case HitagReleaserBleException.UNKNOWN_ERROR_CODE:
            case HitagReleaserBleException.BLUETOOTH_CANNOT_START:
            default:
                Log.d("ERROR", "Unable to start scanning");
                break;
        }
    }

    public static int getMaximumConnectableDeviceCount() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            return 7;
        }else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            return 4;
        }

        return 0;
    }

    public static byte[] parseHexBinary(String s) {
        final int len = s.length();

        // "111" is not a valid hex encoding.
        if( len%2 != 0 )
            return null; //throw new IllegalArgumentException("hexBinary needs to be even-length: "+s);

        byte[] out = new byte[len/2];

        for( int i=0; i<len; i+=2 ) {
            int h = hexToBin(s.charAt(i  ));
            int l = hexToBin(s.charAt(i+1));
            if( h==-1 || l==-1 )
                return null; //throw new IllegalArgumentException("contains illegal character for hexBinary: "+s);

            out[i/2] = (byte)(h*16+l);
        }

        return out;
    }

    private static int hexToBin( char ch ) {
        if( '0'<=ch && ch<='9' )    return ch-'0';
        if( 'A'<=ch && ch<='F' )    return ch-'A'+10;
        if( 'a'<=ch && ch<='f' )    return ch-'a'+10;
        return -1;
    }

    private static final char[] hexCode = "0123456789ABCDEF".toCharArray();

    public static String printHexBinary(byte[] data) {
        StringBuilder r = new StringBuilder(data.length*2);
        for ( byte b : data) {
            r.append(hexCode[(b >> 4) & 0xF]);
            r.append(hexCode[(b & 0xF)]);
        }
        return r.toString();
    }
}