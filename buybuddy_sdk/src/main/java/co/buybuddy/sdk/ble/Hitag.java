package co.buybuddy.sdk.ble;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;

import co.buybuddy.sdk.BuyBuddy;
import co.buybuddy.sdk.BuyBuddyUtil;
import co.buybuddy.sdk.ble.btleasyncgatt.BtleAsyncGattManager;
import co.buybuddy.sdk.model.HitagPasswordPayload;

import static android.bluetooth.BluetoothGatt.GATT_SUCCESS;
import static android.bluetooth.BluetoothProfile.STATE_CONNECTED;
import static android.bluetooth.BluetoothProfile.STATE_DISCONNECTED;
import static co.buybuddy.sdk.ble.Hitag.Characteristic.PASSWORD;
import static co.buybuddy.sdk.ble.HitagState.DISCOVERING;

/**
 * Created by Furkan Ençkü on 8/22/17.
 * This code written by buybuddy Android Team
 */

class Hitag {

    @SuppressWarnings("FieldCanBeLocal")
    private long TIMEOUT_CONNECTING = 12000L;
    @SuppressWarnings("FieldCanBeLocal")
    private long TIMEOUT_DISCOVERING = 12000L;
    @SuppressWarnings("FieldCanBeLocal")
    private long TIMEOUT_PAYLOAD_FIRST = 4000L;
    @SuppressWarnings("FieldCanBeLocal")
    private long TIMEOUT_PAYLOAD_SECOND = 4000L;
    @SuppressWarnings("FieldCanBeLocal")
    private long TIMEOUT_UNLOCKING = 12000L;

    private HandlerThread handlerThread = new HandlerThread("HitagThread");
    private final String TAG = "HitagBLE";

    private Queue<BluetoothGattDescriptor> descriptorWriteQueue = new LinkedList<>();
    private Queue<BluetoothGattCharacteristic> characteristicReadQueue = new LinkedList<>();

    private BtleAsyncGattManager manager;

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
    private HitagState currentState = HitagState.INITIALIZING;

     int getConnectionState() {
        return connectionState;
    }

     Hitag(Context ctx, BluetoothDevice device) {

        handlerThread.start();
        connectionTimeoutHandler = new Handler(handlerThread.getLooper());
        notifyTimeoutHandler = new Handler(handlerThread.getLooper());

        if (Build.VERSION.SDK_INT >= 23) {
            hitagGatt = device.connectGatt(ctx, false, mCallBack, BluetoothDevice.TRANSPORT_LE);
        } else {
            hitagGatt = device.connectGatt(ctx, false, mCallBack);
        }


        htgCharacters = new HashMap<>();

        connectionTimeoutHandler.postDelayed(timeOutRunnable, TIMEOUT_CONNECTING);
        startedAt = System.currentTimeMillis();

        BuyBuddyUtil.printD(TAG,"tt- 1");
    }

    private Runnable timeOutRunnable = new Runnable() {
        @Override
        public void run() {
            if (hitagDelegate != null)
                hitagDelegate.connectionStateChanged(hitagId, 99);

            BuyBuddyUtil.printD(TAG, "ID: " + hitagId + " Timeout");

            forceDisconnect();
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

                case DISCOVERING:
                    BuyBuddyUtil.printD(TAG, "ID: " + hitagId + " DC ");
                    break;
            }

            BuyBuddyUtil.printD(TAG, "ID: " + hitagId + " " + currentState.name());

            if (hitagDelegate != null)
                //hitagDelegate.onDeviceStuck(hitagId, currentState);

            forceDisconnect();
        }
    };

    public Hitag setHitagId(String hitagId) {
        this.hitagId = hitagId;
        return this;
    }

    public HitagState getCurrentState() {
        return currentState;
    }

    public void disconnect() {
        if (hitagGatt != null){
            hitagGatt.disconnect();
            handlerThread.quitSafely();
        }

    }

    public void forceDisconnect() {
        if (hitagGatt != null) {

            Method method = null;
            try {
                method = hitagGatt.getDevice().getClass().getMethod("removeBond", (Class[]) null);
                method.invoke(hitagGatt.getDevice(), (Object[]) null);
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (manager != null) {
                if (!manager.isInterrupted())
                    manager.cancel();
            }

            hitagGatt.disconnect();
            handlerThread.quitSafely();
        }
    }

    public boolean releaseHitag(HitagPasswordPayload password) {

        BuyBuddyUtil.printD(TAG, "tt- 4p2");

        this.password = password;
        this.currentState = HitagState.PASSWORD_IN_PROGRESS;

        if (htgCharacters.get(PASSWORD) != null) {

            BluetoothGattCharacteristic characteristic = htgCharacters.get(PASSWORD);
            characteristic.setValue(BuyBuddyBleUtils.parseHexBinary(password.getFirst()));

            manager.writeCharacteristic(characteristic);

            isPasswordSend = true;

            /*if (wrote) {
                BuyBuddyUtil.printD(TAG, "tt- 4p3");
            } else {
                BuyBuddyUtil.printD(TAG, "tt- 4p4");
            }*/

        }else {

            BuyBuddyUtil.printD(TAG, "tt- 4p5");
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

            BuyBuddyUtil.printD(TAG,"tt- 2" + newState);

            if (hitagDelegate != null)  {
                hitagDelegate.connectionStateChanged(hitagId, newState);
            }

            connectionState = newState;

            if (newState == STATE_CONNECTED) {

                manager = new BtleAsyncGattManager(gatt);
                manager.start();
                manager.enable();

                connectionTimeoutHandler.removeCallbacks(timeOutRunnable);
                notifyTimeoutHandler.postDelayed(notifyRunnable, TIMEOUT_DISCOVERING);
                currentState = DISCOVERING;
                hitagGatt.discoverServices();
            } else if (newState == STATE_DISCONNECTED){
                if (hitagGatt != null) {
                    connectionTimeoutHandler.removeCallbacks(timeOutRunnable);
                    notifyTimeoutHandler.removeCallbacks(notifyRunnable);
                    try {
                        hitagGatt.close();
                    }catch (Exception e) {
                        BuyBuddyUtil.printD(TAG, "tt- 10 " + e.getLocalizedMessage());
                    }
                }

                if (manager != null) {
                    if (!manager.isInterrupted())
                        manager.cancel();
                }
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);

            BuyBuddyUtil.printD(TAG,"tt- 3");

            if (status == GATT_SUCCESS) {
                for (BluetoothGattService service : hitagGatt.getServices()) {
                    if (Characteristic.compare(service.getUuid(), Characteristic.MAIN_SERVICE)) {

                        for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                            Characteristic foundChar =  Characteristic.find(characteristic.getUuid());
                            if (foundChar != Characteristic.UNKNOWN) {
                                htgCharacters.put(foundChar, characteristic);

                                if (foundChar == Characteristic.PASSWORD_VERSION) {

                                    //readCharacteristic(characteristic);

                                    manager.readCharacteristic(characteristic);

                                } else if (foundChar == Characteristic.NOTIFIER){

                                    final BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
                                    //descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);

                                    manager.writeDescriptor(descriptor);
                                }
                            }
                        }
                        break;
                    }
                }
            }
        }

        private void writeDescriptor(BluetoothGattDescriptor d){

            BuyBuddyUtil.printD(TAG,"tt- 7");

            descriptorWriteQueue.add(d);

            if(descriptorWriteQueue.size() == 1 && characteristicReadQueue.size() == 0){
                hitagGatt.writeDescriptor(descriptorWriteQueue.element());
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);

            BuyBuddyUtil.printD(TAG,"tt- 8");

            manager.writeCompleted();

            boolean notifier = gatt
                    .setCharacteristicNotification
                            (htgCharacters.get(Characteristic.NOTIFIER), true);

            BuyBuddyUtil.printD(TAG, "tt- 9 " + notifier);

            /*descriptorWriteQueue.remove();  //pop the item that we just finishing writing

            boolean notifier = gatt
                                .setCharacteristicNotification
                                        (htgCharacters.get(Characteristic.NOTIFIER), true);
            //if there is more to write, do it!
            if(descriptorWriteQueue.size() > 0)
                hitagGatt.writeDescriptor(descriptorWriteQueue.element());
            else if(characteristicReadQueue.size() > 0)
                hitagGatt.readCharacteristic(characteristicReadQueue.element()); */
        }

        private void readCharacteristic(BluetoothGattCharacteristic c) {

            BuyBuddyUtil.printD(TAG,"tt- 4a");

            characteristicReadQueue.add(c);
            if((characteristicReadQueue.size() == 1) && (descriptorWriteQueue.size() == 0))
                hitagGatt.readCharacteristic(c);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);

            BuyBuddyUtil.printD(TAG,"tt- 4b");

            manager.readCompleted();

            if (status == GATT_SUCCESS) {
                if (hitagDelegate != null) {
                    BuyBuddyUtil.printD(TAG,"tt- 4c");
                    Characteristic htgCharacteristic = Characteristic.find(characteristic.getUuid());
                    if (htgCharacteristic != Characteristic.UNKNOWN) {
                        hitagDelegate.onCharacteristicRead(hitagId,
                                htgCharacteristic,
                                characteristic.getValue());
                        BuyBuddyUtil.printD(TAG,"tt- 4d");
                    }
                }
            } else {
                BuyBuddyUtil.printD(TAG,"tt- 4e");
            }

            /*characteristicReadQueue.remove();

            new Thread(new Runnable() {
                @Override
                public void run() {
                    if(descriptorWriteQueue.size() > 0)
                        if (hitagGatt.writeDescriptor(descriptorWriteQueue.element())) {
                            BuyBuddyUtil.printD(TAG, "tt- 4w1");
                        } else {
                            BuyBuddyUtil.printD(TAG, "tt- 4w2");
                        }
                    else if(characteristicReadQueue.size() > 0)
                        if (hitagGatt.readCharacteristic(characteristicReadQueue.element())) {
                            BuyBuddyUtil.printD(TAG, "tt- 4w3");
                        } else {
                            BuyBuddyUtil.printD(TAG, "tt- 4w4");
                        };
                }
            }).start(); */

        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);

            manager.writeCompleted();
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);

            BuyBuddyUtil.printD(TAG,"tt- 5");
            BuyBuddyUtil.printD(TAG, Hitag.Characteristic.find(characteristic.getUuid()).toString());

            if (hitagDelegate != null) {
                Characteristic htgCharacteristic = Characteristic.find(characteristic.getUuid());

                if (htgCharacteristic != Characteristic.UNKNOWN) {

                    hitagDelegate.onCharacteristicUpdate(hitagId,
                            htgCharacteristic,
                            characteristic.getValue());
                }

                if (htgCharacteristic == Characteristic.NOTIFIER && isPasswordSend) {

                    final byte value[] = characteristic.getValue();

                    if (HitagState.compare(HitagState.PASSWORD_FIRST_PAYLOAD, value)) {

                        notifyTimeoutHandler.removeCallbacks(notifyRunnable);
                        notifyTimeoutHandler.postDelayed(notifyRunnable, TIMEOUT_PAYLOAD_FIRST);

                        BuyBuddyUtil.printD(TAG, "ID: " + hitagId + " PASSWORD_FIRST_PAYLOAD ");

                        currentState = HitagState.PASSWORD_FIRST_PAYLOAD;

                        BluetoothGattCharacteristic passwordCaracteristic = htgCharacters.get(PASSWORD);
                        passwordCaracteristic.setValue(BuyBuddyBleUtils.parseHexBinary(password.getSecond()));

                        manager.writeCharacteristic(passwordCaracteristic);

                        //hitagGatt.writeCharacteristic(htgCharacters.get(PASSWORD));

                    }else if (HitagState.compare(HitagState.PASSWORD_SECOND_PAYLOAD, value)) {

                        notifyTimeoutHandler.removeCallbacks(notifyRunnable);
                        notifyTimeoutHandler.postDelayed(notifyRunnable, TIMEOUT_PAYLOAD_SECOND);

                        BuyBuddyUtil.printD(TAG, "ID: " + hitagId + " PASSWORD_SECOND_PAYLOAD ");

                        currentState = HitagState.PASSWORD_SECOND_PAYLOAD;

                        BluetoothGattCharacteristic passwordCaracteristic = htgCharacters.get(PASSWORD);
                        passwordCaracteristic.setValue(BuyBuddyBleUtils.parseHexBinary(password.getThird()));

                        manager.writeCharacteristic(passwordCaracteristic);

                        //hitagGatt.writeCharacteristic(htgCharacters.get(PASSWORD));

                    }else if (HitagState.compare(HitagState.RELEASE_PROCESS_STARTING, value)) {

                    }else if (HitagState.compare(HitagState.STATE_UNLOCKING, value)) {

                        BuyBuddyUtil.printD(TAG, "ID: " + hitagId + " STATE_UNLOCKING ");

                        currentState = HitagState.STATE_UNLOCKING;

                        notifyTimeoutHandler.removeCallbacks(notifyRunnable);
                        notifyTimeoutHandler.postDelayed(notifyRunnable, TIMEOUT_UNLOCKING);

                    }else if (HitagState.compare(HitagState.STATE_UNLOCKED, value)) {

                        BuyBuddyUtil.printD(TAG, "ID: " + hitagId + " STATE_UNLOCKED ");

                        currentState = HitagState.STATE_UNLOCKED;
                        notifyTimeoutHandler.removeCallbacks(notifyRunnable);
                    }
                }
            }
        }
    };

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
        void onDeviceStuck(String hitagId, HitagState state);
    }
}

