package co.buybuddy.sdk.ble;

import co.buybuddy.sdk.BuyBuddyUtil;

public class CollectedHitag {
    private String id; // 0100000001
    private int rssi;
    private int txPower;
    private int battery;
    private boolean isVibrating = false;
    private int pinState = 5;

    public String getId() {
        return id.toUpperCase();
    }

    public CollectedHitag setId(String id) {
        this.id = id;
        return this;
    }

    int getRssi() {
        return rssi;
    }

    int getTxPower() {
        return txPower;
    }

    int getBattery() {
        return battery;
    }

    CollectedHitag(int rssi){
        this.rssi = rssi;
    }

    public int getPinState() {
        return pinState;
    }

    public boolean isVibrating() {
        return isVibrating;
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

    public CollectedHitag setVibration(boolean isVibrating) {
        this.isVibrating = isVibrating;
        return this;
    }

    public CollectedHitag setPinState(int pinState) {
        this.pinState = pinState;
        return this;
    }

    @Override
    public String toString() {
        return "HitagId   : " + BuyBuddyUtil.w(id)      + "\n"  +
               "Rssi : "      + BuyBuddyUtil.w(rssi)    + "\n"  +
               "TxPower : "   + BuyBuddyUtil.w(txPower) + "\n"  +
               "Battery : "   + BuyBuddyUtil.w(battery);
    }
}
