package co.buybuddy.sdk;

import android.support.annotation.Nullable;

import com.polidea.rxandroidble.RxBleDevice;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by furkan on 6/14/17.
 * Gururla sunar. AHAHAHAHA Some spagetties
 */

public final class CollectedHitagTS extends CollectedHitag {
    private long lastSeen;

    public long getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(long lastSeen) {
        this.lastSeen = lastSeen;
    }

    private static final int MANUFACTURER_DATA = -1;
    private static final int TX_POWER = 10;
    private RxBleDevice device;

    CollectedHitag getWithoutTS(){
        return new CollectedHitag(this.getRssi())
                .setBattery(getBattery())
                .setId(getId())
                .setTxPower(getTxPower())
                .setValidationCode(getValidationCode());
    }

    CollectedHitagTS(int rssi) {
        super(rssi);
    }

    public RxBleDevice getDevice() {
        return device;
    }

    @Override
    public CollectedHitagTS setBattery(int battery) {
        super.setBattery(battery);
        return this;
    }

    public CollectedHitagTS setId(String id) {
        super.setId(id);
        return this;
    }

    @Override
    public CollectedHitagTS setTxPower(int txPower) {
        super.setTxPower(txPower);
        return this;
    }

    public CollectedHitagTS setDevice(RxBleDevice device) {
        this.device = device;
        return this;
    }

    @Override
    public CollectedHitagTS setValidationCode(int validationCode) {
        super.setValidationCode(validationCode);
        return this;
    }

    @Nullable
    public static CollectedHitagTS getHitag(RxBleDevice device, byte scanRecord[], int rssi) {

        if (scanRecord == null)
            return null;

        String manufacturerData, deviceID;
        CollectedHitagTS hitag;


        Map<Integer, String> recordMap = parseRecord(scanRecord);
        if (recordMap != null)
            if (recordMap.size() == 3 || recordMap.size() == 6) {

                switch (recordMap.size()) {
                    case 3:
                        manufacturerData = recordMap.get(MANUFACTURER_DATA);
                        deviceID = null;

                        if (manufacturerData != null) {
                            if (manufacturerData.length() == 50) {
                                String devicePostfix = manufacturerData.substring(2, 10);
                                String devicePrefix = manufacturerData.substring(41, 43);
                                String reOrderedPostfix = "";

                                for (int i = 8; i >= 2; i -= 2) {
                                    reOrderedPostfix += devicePostfix.substring(i-2, i);
                                }

                                deviceID = devicePrefix + reOrderedPostfix;
                            }
                        }

                        if (deviceID != null) {
                            hitag =  new CollectedHitagTS(rssi)
                                            .setDevice(device)
                                            .setId(deviceID);

                            return hitag;
                        }

                        break;

                    case 6:

                        manufacturerData = recordMap.get(MANUFACTURER_DATA);
                        int txPower =  recordMap.get(TX_POWER)
                                                != null ? (256 - Integer.parseInt(recordMap.get(TX_POWER), 16)) : -90; //TX POWER NORMAL VALUE -91
                        int battery = 0;
                        int validationCode = 0;

                        deviceID = null;

                        if (manufacturerData != null) { // 02442C5A540507000000010059
                            if (manufacturerData.length() == 26) {
                                String devicePostfix = manufacturerData.substring(12, 20);
                                String devicePrefix = manufacturerData.substring(20, 22);
                                String validation = manufacturerData.substring(10, 12) + manufacturerData.substring(8, 10);
                                validationCode = Integer.parseInt(validation, 16);
                                String reOrderedPostfix = "";

                                for (int i = 8; i >= 2; i -= 2) {
                                    reOrderedPostfix += devicePostfix.substring(i-2, i);
                                }

                                deviceID = devicePrefix + reOrderedPostfix;
                            }
                        }

                        if (deviceID != null) {
                            hitag = new CollectedHitagTS(rssi)
                                            .setDevice(device)
                                            .setValidationCode(validationCode)
                                            .setId(deviceID)
                                            .setTxPower(txPower)
                                            .setBattery(battery);

                            return hitag;
                        }


                        break;
                }

            }

        return null;
    }

    public CollectedHitagTS updateSeen(long lastSeen) {
        this.lastSeen = lastSeen;
        return this;
    }

    static public Map<Integer,String> parseRecord(byte[] scanRecord){
        Map <Integer,String> ret = new HashMap<>();
        int index = 0;
        while (index < scanRecord.length) {
            int length = scanRecord[index++];
            //Zero value indicates that we are done with the record now
            if (length == 0) break;

            int type = scanRecord[index];
            //if the type is zero, then we are pass the significant section of the data,
            // and we are thud done
            if (type == 0) break;

            byte[] data = Arrays.copyOfRange(scanRecord, index + 1, index + length);
            if(data != null && data.length > 0) {
                StringBuilder hex = new StringBuilder(data.length * 2);
                // the data appears to be there backwards
                for (int bb = data.length- 1; bb >= 0; bb--){
                    hex.append(String.format("%02X", data[bb]));
                }
                ret.put(type,hex.toString());
            }
            index += length;
        }

        return ret;
    }
}