package co.buybuddy.android;

/**
 * Created by furkan on 6/15/17.
 * Gururla sunar. AHAHAHAHA Some spagetties
 */

public class BuyBuddyBleUtils {
    static final String MAIN_PREFIX = "0000beef";
    static final String MAIN_POSTFIX = "-6275-7962-7564-647966656565";

    static final String HITAG_TX = "00007373";
    static final String HITAG_RX = "00007478";

    static long HITAG_SCAN_INTERVAL_IDLE = 500L;
    static long HITAG_SCAN_BETWEEN_INTERVAL_IDLE = 30000L;

    static long HITAG_SCAN_INTERVAL_ACTIVE = 4500L;
    static long HITAG_SCAN_BETWEEN_INTERVAL_ACTIVE = 300L;

    public final static int HITAG_TYPE_CUSTOM = 6;
    public final static int HITAG_TYPE_BEACON = 3;
}
