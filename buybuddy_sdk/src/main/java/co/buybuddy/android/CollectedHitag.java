package co.buybuddy.android;

class CollectedHitag {
    private String id; // 0100000001
    private int rssi;
    private int txPower;
    private int validationCode = -1;
    private int battery;

    public String getId() {
        return id;
    }

    public CollectedHitag setId(String id) {
        this.id = id;
        return this;
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

    CollectedHitag(int rssi){
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
        return "HitagId   : " + BuyBuddyUtil.w(id)      + "\n"  +
               "Rssi : "      + BuyBuddyUtil.w(rssi)    + "\n"  +
               "TxPower : "   + BuyBuddyUtil.w(txPower) + "\n"  +
               "Battery : "   + BuyBuddyUtil.w(battery) + "\n"  +
               "ValidCode : " + BuyBuddyUtil.w(validationCode);
    }
}
