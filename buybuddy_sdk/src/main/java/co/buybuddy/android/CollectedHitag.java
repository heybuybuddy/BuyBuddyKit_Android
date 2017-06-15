package co.buybuddy.android;

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
    private int battery;

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

    public int getBattery() {
        return battery;
    }

    CollectedHitag(String id, int rssi){
        this.id = id;
        this.rssi = rssi;
    }

    public CollectedHitag setBattery(int battery) {
        this.battery = battery;
        return this;
    }

    public CollectedHitag setRssi(int rssi) {
        this.rssi = rssi;
        return this;
    }

    public CollectedHitag setTxPower(int txPower) {
        this.txPower = txPower;
        return this;
    }

    public CollectedHitag setValidationCode(int validationCode) {
        this.validationCode = validationCode;
        return this;
    }

    @Override
    public String toString() {
        return "HitagId   : " + BuyBuddyUtil.w(id) + "\n"  +
               "Timestamp : " + BuyBuddyUtil.w(timestamp)  + "\n"  +
               "Rssi : "      + BuyBuddyUtil.w(rssi)       + "\n"  +
               "TxPower : "   + BuyBuddyUtil.w(txPower)    + "\n"  +
               "Battery : "   + BuyBuddyUtil.w(battery)    + "\n"  +
               "ValidCode : " + BuyBuddyUtil.w(validationCode);
    }
}
