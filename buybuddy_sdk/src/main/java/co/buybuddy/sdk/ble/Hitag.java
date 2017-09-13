package co.buybuddy.sdk.ble;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.os.Handler;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;

import co.buybuddy.sdk.BuyBuddyUtil;
import co.buybuddy.sdk.model.HitagPasswordPayload;

import static android.bluetooth.BluetoothGatt.GATT_SUCCESS;
import static android.bluetooth.BluetoothProfile.STATE_CONNECTED;
import static co.buybuddy.sdk.ble.Hitag.Characteristic.PASSWORD;
import static co.buybuddy.sdk.ble.Hitag.State.PASSWORD_FIRST_PAYLOAD;
import static co.buybuddy.sdk.ble.Hitag.State.PASSWORD_IN_PROGRESS;
import static co.buybuddy.sdk.ble.Hitag.State.PASSWORD_SECOND_PAYLOAD;

/**
 * Created by Furkan Ençkü on 8/22/17.
 * This code written by buybuddy Android Team
 */

class Hitag {

    private final String TAG = "Hitag";

    private Queue<BluetoothGattDescriptor> descriptorWriteQueue = new LinkedList<>();
    private Queue<BluetoothGattCharacteristic> characteristicReadQueue = new LinkedList<>();

    private BluetoothGatt hitagGatt;
    private HashMap<Characteristic, BluetoothGattCharacteristic> htgCharacters;
    private Delegate hitagDelegate;
    private String hitagId;
    private HitagPasswordPayload password;
    private boolean isPasswordSend = false;
    private long startedAt = 0;

    private Handler connectionTimeoutHandler;
    private Handler notifyTimeoutHandler;

    public long getStartedAt() {
        return startedAt;
    }

    private int connectionState = BluetoothGatt.STATE_DISCONNECTED;
    private State currentState = State.INITIALIZING;

    public int getConnectionState() {
        return connectionState;
    }

    public Hitag(Context ctx, BluetoothDevice device) {
        connectionTimeoutHandler = new Handler();
        notifyTimeoutHandler = new Handler();

        hitagGatt = device.connectGatt(ctx, false, mCallBack);
        htgCharacters = new HashMap<>();

        connectionTimeoutHandler.postDelayed(timeOutRunnable, 10000);
        startedAt = System.currentTimeMillis();
    }

    private Runnable timeOutRunnable = new Runnable() {
        @Override
        public void run() {
            if (hitagDelegate != null)
                hitagDelegate.connectionStateChanged(hitagId, 99);

            BuyBuddyUtil.printD(TAG, "Hitag Id: " + hitagId + " Connection Timeout");

            hitagGatt.disconnect();
        }
    };

    private Runnable notifyRunnable = new Runnable() {
        @Override
        public void run() {

            switch (currentState) {
                case PASSWORD_IN_PROGRESS:

                    break;

                case PASSWORD_FIRST_PAYLOAD:

                    break;

                case PASSWORD_SECOND_PAYLOAD:

                    break;

                case PASSWORD_THIRD_PAYLOAD:

                    break;

                case STATE_UNLOCKING:

                    break;
            }

            if (hitagDelegate != null)
                hitagDelegate.onDeviceStuck(hitagId, currentState);

            hitagGatt.disconnect();
        }
    };

    public void connect(){
        hitagGatt.connect();
        connectionTimeoutHandler.removeCallbacks(timeOutRunnable);
        notifyTimeoutHandler.removeCallbacks(notifyRunnable);

        connectionTimeoutHandler.postDelayed(timeOutRunnable, 10000);
        startedAt = System.currentTimeMillis();
    }

    public Hitag setHitagId(String hitagId) {
        this.hitagId = hitagId;
        return this;
    }

    public State getCurrentState() {
        return currentState;
    }

    public void disconnect() {
        hitagGatt.disconnect();
    }

    public boolean releaseHitag(HitagPasswordPayload password) {
        this.password = password;
        this.currentState = PASSWORD_IN_PROGRESS;

        if (htgCharacters.get(PASSWORD) != null) {

            htgCharacters.get(PASSWORD).setValue(BuyBuddyBleUtils.parseHexBinary(password.getFirst()));

            boolean wrote = hitagGatt.writeCharacteristic(htgCharacters.get(PASSWORD));
            isPasswordSend = true;

        }else {
            return false;
        }

        return connectionState == STATE_CONNECTED;
    }

    Hitag setHitagDelegate(Delegate hitagDelegate) {
        this.hitagDelegate = hitagDelegate;
        return this;
    }

    private final BluetoothGattCallback mCallBack = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);

            if (hitagDelegate != null)  {
                hitagDelegate.connectionStateChanged(hitagId, newState);
            }

            connectionState = newState;

            if (newState == STATE_CONNECTED) {
                connectionTimeoutHandler.removeCallbacks(timeOutRunnable);
                notifyTimeoutHandler.postDelayed(notifyRunnable, 10000);
                hitagGatt.discoverServices();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);

            if (status == GATT_SUCCESS) {
                for (BluetoothGattService service : hitagGatt.getServices()) {
                    if (Characteristic.compare(service.getUuid(), Characteristic.MAIN_SERVICE)) {

                        for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                            Characteristic foundChar =  Characteristic.find(characteristic.getUuid());
                            if (foundChar != Characteristic.UNKNOWN) {
                                htgCharacters.put(foundChar, characteristic);

                                BuyBuddyUtil.printD("Hitag", foundChar.name());

                                if (foundChar == Characteristic.PASSWORD_VERSION) {

                                    readCharacteristic(characteristic);

                                } else if (foundChar == Characteristic.NOTIFIER){

                                    BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
                                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                                    writeDescriptor(descriptor);

                                }
                            }
                        }
                        break;
                    }
                }
            }
        }

        private void writeDescriptor(BluetoothGattDescriptor d){
            descriptorWriteQueue.add(d);

            if(descriptorWriteQueue.size() == 1 && characteristicReadQueue.size() == 0){
                hitagGatt.writeDescriptor(descriptorWriteQueue.element());
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);

            descriptorWriteQueue.remove();  //pop the item that we just finishing writing
            gatt.setCharacteristicNotification(htgCharacters.get(Characteristic.NOTIFIER), true);
            //if there is more to write, do it!
            if(descriptorWriteQueue.size() > 0)
                hitagGatt.writeDescriptor(descriptorWriteQueue.element());
            else if(characteristicReadQueue.size() > 0)
                hitagGatt.readCharacteristic(characteristicReadQueue.element());
        }

        private void readCharacteristic(BluetoothGattCharacteristic c) {
            characteristicReadQueue.add(c);
            if((characteristicReadQueue.size() == 1) && (descriptorWriteQueue.size() == 0))
                hitagGatt.readCharacteristic(c);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);

            if (status == GATT_SUCCESS) {
                if (hitagDelegate != null) {
                    Characteristic htgCharacteristic = Characteristic.find(characteristic.getUuid());
                    if (htgCharacteristic != Characteristic.UNKNOWN) {
                        hitagDelegate.onCharacteristicRead(hitagId,
                                htgCharacteristic,
                                characteristic.getValue());
                    }
                }
            }

            characteristicReadQueue.remove();

            if(characteristicReadQueue.size() > 0) {
                hitagGatt.readCharacteristic(characteristicReadQueue.element());
            }

        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);

            BuyBuddyUtil.printD("Hitag", "UPDATE !");

            if (hitagDelegate != null) {
                Characteristic htgCharacteristic = Characteristic.find(characteristic.getUuid());

                if (htgCharacteristic != Characteristic.UNKNOWN) {

                    hitagDelegate.onCharacteristicUpdate(hitagId,
                            htgCharacteristic,
                            characteristic.getValue());
                }

                if (htgCharacteristic == Characteristic.NOTIFIER && isPasswordSend) {

                    final byte value[] = characteristic.getValue();

                    if (State.compare(PASSWORD_FIRST_PAYLOAD, value)) {

                        notifyTimeoutHandler.removeCallbacks(notifyRunnable);
                        notifyTimeoutHandler.postDelayed(notifyRunnable, 3000);

                        currentState = PASSWORD_FIRST_PAYLOAD;

                        htgCharacters.get(PASSWORD).setValue(BuyBuddyBleUtils.parseHexBinary(password.getSecond()));

                        hitagGatt.writeCharacteristic(htgCharacters.get(PASSWORD));

                    }else if (State.compare(PASSWORD_SECOND_PAYLOAD, value)) {

                        notifyTimeoutHandler.removeCallbacks(notifyRunnable);
                        notifyTimeoutHandler.postDelayed(notifyRunnable, 3000);

                        currentState = PASSWORD_SECOND_PAYLOAD;

                        htgCharacters.get(PASSWORD).setValue(BuyBuddyBleUtils.parseHexBinary(password.getThird()));

                        hitagGatt.writeCharacteristic(htgCharacters.get(PASSWORD));

                    }else if (State.compare(State.RELEASE_PROCESS_STARTING, value)) {

                    }else if (State.compare(State.STATE_UNLOCKING, value)) {

                        currentState = State.STATE_UNLOCKING;

                        notifyTimeoutHandler.removeCallbacks(notifyRunnable);
                        notifyTimeoutHandler.postDelayed(notifyRunnable, 10000);

                    }else if (State.compare(State.STATE_UNLOCKED, value)) {

                        currentState = State.STATE_UNLOCKED;
                        notifyTimeoutHandler.removeCallbacks(notifyRunnable);
                    }
                }
            }
        }
    };

    public enum State {

        PASSWORD_IN_PROGRESS("IN_PROGRESS"),
        PASSWORD_FIRST_PAYLOAD("01EE"),
        PASSWORD_SECOND_PAYLOAD("02EE"),
        PASSWORD_THIRD_PAYLOAD("03EE"),

        STATE_LOCKED("10"),
        STATE_LOCKING("11"),
        STATE_UNLOCKED("20"),
        STATE_UNLOCKING("21"),

        PASSWORD_OLD("04"),
        PASSWORD_WRONG("05"),

        RELEASE_PROCESS_STARTING("00"),
        RELEASE_VALIDATION_SUCCESS("01"),
        RELEASE_VALIDATION_FAILED("02"),

        PIN_WAITING_FOR_UNLOCK("31"),
        PIN_WAITING_FOR_LOCK("30"),

        STATE_BUGGY("FF"),
        STATE_NONE("FE"),
        STATE_PIN_INSIDE("FD"),
        STATE_PIN_OUTSIDE("FC"),

        FLASH_ERROR("F1"),
        FLASH_ERROR2("F2"),

        ERROR("FFFF"),
        UNKNOWN(""),

        CONNECTED("CONNECTED"),
        DISCONNECTED("DISCONNECTED"),

        INITIALIZING("init"),

        NOT_FOUND("NOT_FOUND"),
        CONNECTION_FAILED("CONNECTION_FAILED");

        State(String response) {
            this.byteArray = BuyBuddyBleUtils.parseHexBinary(response);
        }

        private byte[] byteArray;

        public static boolean compare(State response, byte[] value) {
            return Arrays.equals(response.byteArray, value);
        }
    }

    enum Characteristic {

        PASSWORD_VERSION("0004"),

        FIRMWARE_VERSION("2A26"),
        HARDWARE_VERSION("2A27"),

        MAIN_SERVICE("BABA"),
        DFU_TRIGGER("000F"),

        PASSWORD("0001"),
        NOTIFIER("0002"),

        BATTERY("2A19"),
        STATE("0006"),
        ID("0007"),

        UNKNOWN("");

        Characteristic(String prefix) {

            this.prefix = prefix;
            this.setUuid(UUID.fromString("0000" + prefix + postfix));
        }

        public String  prefix;
        private UUID uuid;

        public static final String postfix = "-6275-7962-7564-647966656565";

        public Characteristic setUuid(UUID uuid) {
            this.uuid = uuid;
            return this;
        }

        public static boolean compare(UUID left, Characteristic right) {
            return left.equals(right.uuid);
        }

        public UUID getUuid() {
            return uuid;
        }

        public static Characteristic find(UUID uuid) {
            if (compare(uuid, PASSWORD)) {
                return PASSWORD;
            }else if (compare(uuid, NOTIFIER)) {
                return NOTIFIER;
            } else if (compare(uuid, PASSWORD_VERSION)) {
                return PASSWORD_VERSION;
            } else if (compare(uuid, BATTERY)) {
                return BATTERY;
            } else if (compare(uuid, STATE)) {
                return STATE;
            } else if (compare(uuid, ID)) {
                return ID;
            } else if (compare(uuid, DFU_TRIGGER)) {
                return DFU_TRIGGER;
            } else if (compare(uuid, FIRMWARE_VERSION)) {
                return FIRMWARE_VERSION;
            } else if (compare(uuid, HARDWARE_VERSION)) {
                return HARDWARE_VERSION;
            }

            return UNKNOWN;
        }


    }

    public interface Delegate {
        void connectionStateChanged(String hitagId, int state);
        void onCharacteristicUpdate(String hitagId, Hitag.Characteristic chars, byte[] value);
        void onCharacteristicRead(String hitagId, Hitag.Characteristic chars, byte[] value);
        void onDeviceStuck(String hitagId, State state);
    }

}
