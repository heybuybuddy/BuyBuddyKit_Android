package co.buybuddy.android.model;

/**
 * Created by furkan on 6/14/17.
 * Gururla sunar. AHAHAHAHA Some spagetties
 */

public final class CollectedHitag {
    private String id; // 0100000001
    private long timestamp;
    private int rssi;
    private int txPower;
    private int validationCode;

    public String getId() {
        return id;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public int getRssi() {
        return rssi;
    }

    public int getTxPower() {
        return txPower;
    }

    public int getValidationCode() {
        return validationCode;
    }

    @Override
    public String toString() {
        return "HitagId   : " + id         + "\n"  +
               "Timestamp : " + timestamp  + "\n"  +
               "Rssi : "      + rssi       + "\n"  +
               "TxPower : "   + txPower    + "\n"  +
               "ValidCode : " + validationCode;
    }
}
