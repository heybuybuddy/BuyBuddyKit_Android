package co.buybuddy.sdk.ble;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.ParcelUuid;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

import co.buybuddy.sdk.BuyBuddy;
import co.buybuddy.sdk.BuyBuddyBluetoothState;
import co.buybuddy.sdk.BuyBuddyUtil;
import co.buybuddy.sdk.HitagScanService;
import co.buybuddy.sdk.ble.blecompat.BluetoothLeCompatException;
import co.buybuddy.sdk.ble.blecompat.BluetoothLeScannerCompat;
import co.buybuddy.sdk.ble.blecompat.ScanCallbackCompat;
import co.buybuddy.sdk.ble.blecompat.ScanResultCompat;
import co.buybuddy.sdk.ble.exception.HitagReleaserException;
import co.buybuddy.sdk.interfaces.BuyBuddyApiCallback;
import co.buybuddy.sdk.model.HitagPasswordPayload;
import co.buybuddy.sdk.responses.BuyBuddyApiError;
import co.buybuddy.sdk.responses.BuyBuddyApiObject;
import co.buybuddy.sdk.responses.BuyBuddyBase;
import co.buybuddy.sdk.responses.IncompleteSale;
import co.buybuddy.sdk.responses.OrderDelegateDetail;

import static android.bluetooth.BluetoothProfile.STATE_CONNECTED;
import static android.bluetooth.BluetoothProfile.STATE_DISCONNECTED;

/**
 * Created by Furkan Ençkü on 8/22/17.
 * This code written by buybuddy Android Team
 */

public final class BuyBuddyHitagReleaser extends Service implements Hitag.Delegate {


    private static long HITAG_DEFAULT_SCAN_TIMEOUT = 10000L;
    BluetoothAdapter mBluetoothAdapter;
    BluetoothManager mBluetoothManager;
    private BluetoothLeScannerCompat mBleScanner;
    private ScanCallbackCompat mScanCallback;
    Handler mHandler;
    boolean isDestroyed = false;
    long currentOrderId = -1;

    HashSet<String> hitagList;
    HashSet<String> willOpenDevices;
    HashMap<String, Integer> tryCountForDevices;
    HashSet<String> tryingDevices;
    HashSet<String> openedDevices;
    HashMap<String, Integer> failedDevices;
    HashSet<String> foundHitags;
    int scanRestartCount = 0;

    HashMap<String, Hitag> deviceMap;
    HashMap<String, BluetoothDevice> hitagRestoredDevice;



    final String TAG = "*_hr_* HitagReleaser";
    final String instanceID = UUID.randomUUID().toString();

    Handler mWatcher;
    Runnable mWatcherRunnable = new Runnable() {
        @Override
        public void run() {

            try {
                Log.d("*x**", "WILL OPEN COUNT" + willOpenDevices.size());
                String print = "";
                for (String hitagId : willOpenDevices) {
                    print += " " + hitagId;
                }

                Log.d("*x**", "TRYING COUNT" + tryingDevices.size());
                print = "";
                for (String hitagId : willOpenDevices) {
                    print += " " + hitagId;
                }

                Log.d("*x**", "FAILED COUNT" + failedDevices.size());
                print = "";
                for (String hitagId : failedDevices.keySet()) {
                    print += " " + hitagId;
                }

                Log.d("*x**", "FOUND HITAGS: " + foundHitags.size());

            }catch (Exception ex){
                ex.printStackTrace();
            }

            if (mWatcher != null)
                mWatcher.postDelayed(this, 1500);
        }
    };

    Handler notFoundHitagHandler;
    Runnable notFoundHitagRunnable = new Runnable() {
        @Override
        public void run() {

            scanRestartCount++;

            if (scanRestartCount > 2) {
                stopScanning();

                for (String hitagId : tryingDevices) {
                    failedDevices.put(hitagId, HitagState.NOT_FOUND.ordinal());
                    EventBus.getDefault().post(new HitagEventFromService(hitagId, HitagState.NOT_FOUND).setEventType(1));
                }

                for (String hitagId : willOpenDevices) {
                    failedDevices.put(hitagId, HitagState.NOT_FOUND.ordinal());
                    EventBus.getDefault().post(new HitagEventFromService(hitagId, HitagState.NOT_FOUND).setEventType(1));
                }

                BuyBuddyUtil.printD(TAG, "Hitags not found");
                EventBus.getDefault().post(new HitagEventFromService(null, null).setEventType(2));
                stopSelf();

            } else {
                Log.d("*x*", " Bulunan cihaz sayısı : " + foundHitags.size());
                notFoundHitagHandler.postDelayed(this, 15000);
            }
        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mHandler = new Handler();
        mWatcher = new Handler();
        notFoundHitagHandler = new Handler();

        mBluetoothManager = (BluetoothManager) this.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mBleScanner = new BluetoothLeScannerCompat(this);

        mScanCallback = new ScanCallbackCompat() {
            @Override
            public void onScanResult(int callbackType, final ScanResultCompat result) {
                super.onScanResult(callbackType, result);

                if (willOpenDevices.size() == 0 && tryingDevices.size() == 0) {
                    stopScanning();
                    EventBus.getDefault().post(new HitagEventFromService(null, null).setEventType(2));
                    EventBus.getDefault().unregister(BuyBuddyHitagReleaser.this);
                    stopSelf();
                }

                if (result != null) {
                    if (result.getScanRecord() != null) {
                        if (result.getScanRecord().getBytes() != null) {

                            final CollectedHitagTS hitag = CollectedHitagTS.getHitag(null, result.getScanRecord().getBytes(), result.getRssi());

                            if (hitag != null) {

                                if (!foundHitags.contains(hitag.getId()) && hitagList.contains(hitag.getId()))
                                    foundHitags.add(hitag.getId());

                                BuyBuddyUtil.printD(TAG, "Hitag Id: " + hitag.getId());

                                if (willOpenDevices.contains(hitag.getId())){

                                    if (BuyBuddyBleUtils.getMaximumConnectableDeviceCount() - (getConnectedDeviceCount() + tryingDevices.size()) > 1) {
                                        willOpenDevices.remove(hitag.getId());
                                        tryingDevices.add(hitag.getId());

                                        if (tryCountForDevices.get(hitag.getId()) != null) {
                                            tryCountForDevices.put(hitag.getId(), tryCountForDevices.get(hitag.getId()) + 1);
                                        } else {
                                            tryCountForDevices.put(hitag.getId(), 0);
                                        }

                                        Hitag willConnectHitag = new Hitag(BuyBuddyHitagReleaser.this, result.getDevice())
                                                .setHitagDelegate(BuyBuddyHitagReleaser.this)
                                                .setHitagId(hitag.getId());

                                        deviceMap.put(hitag.getId(), willConnectHitag);

                                        BuyBuddyUtil.printD(TAG, "Hitag Id: " + hitag.getId() + " will connect");
                                    }
                                }
                            }
                        }
                    }
                }
            }

            @Override
            public void onScanFailed(int errorCode) {
                super.onScanFailed(errorCode);
            }
        };

        BuyBuddyUtil.printD(TAG, "Releaser : " + instanceID);
        mWatcher.postDelayed(mWatcherRunnable, 1500);
    }

    private boolean checkAllConditions() {

        try {
            mBleScanner.isBleScannable(mBluetoothAdapter);
        } catch (BluetoothLeCompatException ex) {
            EventBus.getDefault().post(ex);
            EventBus.getDefault().unregister(this);
            stopSelf();
            return false;
        }

        return true;
    }

    private int getConnectedDeviceCount() {
        return mBluetoothManager.
                getConnectedDevices(BluetoothProfile.GATT).size();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onBluetoothStateEvent(BuyBuddyBluetoothState state) {
        BuyBuddyUtil.printD(TAG, state.name());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        deviceMap = new HashMap<>();
        willOpenDevices = new HashSet<>();
        hitagList = new HashSet<>();
        failedDevices = new HashMap<>();
        openedDevices = new HashSet<>();
        tryingDevices = new HashSet<>();
        tryCountForDevices = new HashMap<>();
        foundHitags = new HashSet<>();
        hitagRestoredDevice = new HashMap<>();

        if (intent != null) {
            Bundle extras = intent.getExtras();

            if (extras != null) {

                boolean is_retry = extras.getBoolean("is_retry");

                if (is_retry) {
                    BuyBuddy.getInstance().api.getIncompleteOrders(new BuyBuddyApiCallback<IncompleteSale>() {
                        @Override
                        public void success(BuyBuddyApiObject<IncompleteSale> response) {

                            willOpenDevices.addAll(Arrays.asList(response.getData().getHitagIds()));
                            hitagList.addAll(Arrays.asList(response.getData().getHitagIds()));

                            currentOrderId = response.getData().getSaleId();

                            if (willOpenDevices.size() > 0) {
                                if (checkAllConditions()) {
                                    startScanning();
                                    notFoundHitagHandler.postDelayed(notFoundHitagRunnable, 20000);
                                }
                            }
                        }

                        @Override
                        public void error(BuyBuddyApiError error) {
                            try {
                                if (error.getResponseCode() == 404) {
                                    throw new HitagReleaserException("Incompleted Order Not Found");
                                }
                            }
                            catch (HitagReleaserException ex) {
                                EventBus.getDefault().post(ex);
                                EventBus.getDefault().unregister(this);
                                stopSelf();
                            }
                        }
                    });
                } else {

                    long orderId = extras.getLong("orderId");
                    currentOrderId = orderId;

                    BuyBuddy.getInstance().api.getOrderDetail(orderId, new BuyBuddyApiCallback<OrderDelegateDetail>() {

                        @Override
                        public void success(BuyBuddyApiObject<OrderDelegateDetail> response) {

                            willOpenDevices.addAll(Arrays.asList(response.getData().getHitagIds()));

                            if (willOpenDevices.size() > 0) {
                                if (checkAllConditions()) {
                                    startScanning();
                                    notFoundHitagHandler.postDelayed(notFoundHitagRunnable, 20000);
                                }
                            }
                        }

                        @Override
                        public void error(BuyBuddyApiError error) {
                            try {
                                if (error.getResponseCode() == 404) {
                                    throw new HitagReleaserException("Order Not Found or Not Paid");
                                }
                            }
                            catch (HitagReleaserException ex) {
                                EventBus.getDefault().post(ex);
                                EventBus.getDefault().unregister(this);
                                stopSelf();
                            }
                        }
                    });
                }
            }

            return START_STICKY;
        }

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        isDestroyed = true;

        EventBus.getDefault().unregister(this);
        for (Hitag htg : deviceMap.values()) {
            htg.disconnect();
            htg.setHitagDelegate(null);
        }

        notFoundHitagHandler.removeCallbacks(notFoundHitagRunnable);
        mWatcher.removeCallbacks(mWatcherRunnable);
        currentOrderId = -1;
    }

    private void startScanning() {
        mBleScanner.stopScan(mBluetoothAdapter, mScanCallback);

        /*final int version = Build.VERSION.SDK_INT;
        if (version >= Build.VERSION_CODES.LOLLIPOP) {
            mBleScanner.startScan(mBluetoothAdapter, null, new ScanSettingsCompat.Builder()
                    .setScanMode(CALLBACK_TYPE_ALL_MATCHES)
                    .setReportDelay(0).build(), mScanCallback);
        }
        else {
            mBleScanner.startScan(mBluetoothAdapter, mScanCallback);
        }*/



        startService(new Intent(this, HitagScanService.class));
        EventBus.getDefault().register(this);
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onHitagEventFromService(final CollectedHitagTS hitag) {

        if (willOpenDevices.size() == 0 && tryingDevices.size() == 0) {
            stopScanning();
            EventBus.getDefault().post(new HitagEventFromService(null, null).setEventType(2));
            EventBus.getDefault().unregister(BuyBuddyHitagReleaser.this);
            stopSelf();
        }

        if (hitagRestoredDevice.get(hitag.getId()) == null)
            hitagRestoredDevice.put(hitag.getId(), hitag.getDevice());

        if (!foundHitags.contains(hitag.getId()) && hitagList.contains(hitag.getId()))
            foundHitags.add(hitag.getId());

        BuyBuddyUtil.printD(TAG, "Hitag Id: " + hitag.getId());

        if (willOpenDevices.contains(hitag.getId())){

            if (BuyBuddyBleUtils.getMaximumConnectableDeviceCount() - (getConnectedDeviceCount() + tryingDevices.size()) > 1) {
                willOpenDevices.remove(hitag.getId());
                tryingDevices.add(hitag.getId());

                if (tryCountForDevices.get(hitag.getId()) != null) {
                    tryCountForDevices.put(hitag.getId(), tryCountForDevices.get(hitag.getId()) + 1);
                } else {
                    tryCountForDevices.put(hitag.getId(), 0);
                }

                Thread hitagConnection = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Looper.prepare();

                        Hitag willConnectHitag = new Hitag(BuyBuddyHitagReleaser.this, hitag.getDevice())
                                .setHitagDelegate(BuyBuddyHitagReleaser.this)
                                .setHitagId(hitag.getId());

                        deviceMap.put(hitag.getId(), willConnectHitag);

                        BuyBuddyUtil.printD(TAG, "Hitag Id: " + hitag.getId() + " will connect");
                    }
                });

                hitagConnection.start();
            }
        }
    }

    private void stopScanning() {
        mBleScanner.stopScan(mBluetoothAdapter, mScanCallback);
    }

    @Override
    public void connectionStateChanged(String hitagId, int state) {

        switch (state) {
            case STATE_CONNECTED:
                BuyBuddyUtil.printD(TAG, "Hitag Id: " + hitagId + " connected");
                EventBus.getDefault().post(new HitagEventFromService(hitagId, HitagState.CONNECTED).setEventType(0));
                break;

            case STATE_DISCONNECTED:
                BuyBuddyUtil.printD(TAG, "Hitag Id: " + hitagId + " disconnected");
                EventBus.getDefault().post(new HitagEventFromService(hitagId, HitagState.DISCONNECTED).setEventType(0));

                if (tryingDevices.contains(hitagId) &&
                        (deviceMap.get(hitagId).getCurrentState() == HitagState.INITIALIZING
                                || deviceMap.get(hitagId).getCurrentState() == HitagState.DISCOVERING))  {

                    tryCountForDevices.put(hitagId, tryCountForDevices.get(hitagId) + 1);

                    if (tryCountForDevices.get(hitagId) > 4) {
                        tryingDevices.remove(hitagId);
                        failedDevices.put(hitagId, HitagState.CONNECTION_FAILED.ordinal());
                        EventBus.getDefault().post(new HitagEventFromService(hitagId, HitagState.CONNECTION_FAILED).setEventType(1));
                    }else {
                        if (deviceMap.get(hitagId) != null) {
                            deviceMap.get(hitagId).forceDisconnect();
                            deviceMap.remove(hitagId);
                            tryingDevices.remove(hitagId);
                            willOpenDevices.add(hitagId);
                        }
                    }
                }

                break;

            case 99: //CONNECTION UNSUCCESSFUL
                BuyBuddyUtil.printD(TAG, "Hitag Id: " + hitagId + " try again!");

                if (tryingDevices.contains(hitagId) && deviceMap.get(hitagId).getCurrentState() == HitagState.INITIALIZING) {
                    tryCountForDevices.put(hitagId, tryCountForDevices.get(hitagId) + 1);

                    if (tryCountForDevices.get(hitagId) > 4) {
                        tryingDevices.remove(hitagId);
                        failedDevices.put(hitagId, HitagState.CONNECTION_FAILED.ordinal());
                        EventBus.getDefault().post(new HitagEventFromService(hitagId, HitagState.CONNECTION_FAILED).setEventType(1));
                    }else {
                        if (deviceMap.get(hitagId) != null) {
                            deviceMap.get(hitagId).forceDisconnect();
                            deviceMap.remove(hitagId);
                            tryingDevices.remove(hitagId);
                            willOpenDevices.add(hitagId);
                        }
                    }
                }

                break;

            default:
                BuyBuddyUtil.printD(TAG, "Hitag Id: " + hitagId + " else " + state);
                break;
        }
    }

    @Override
    public void onCharacteristicUpdate(String hitagId, Hitag.Characteristic chars, byte[] value) {

        BuyBuddyUtil.printD(TAG, hitagId + " : " +
                                 chars.name() + " : " +
                                 BuyBuddyBleUtils.printHexBinary(value));


        if (chars == Hitag.Characteristic.NOTIFIER && tryingDevices.contains(hitagId)) {

            if (HitagState.compare(HitagState.STATE_UNLOCKED, value)) {

                tryingDevices.remove(hitagId);
                openedDevices.add(hitagId);
                deviceMap.get(hitagId).disconnect();
                BuyBuddyUtil.printD(TAG, hitagId + " : completed");

                BuyBuddy.getInstance().api.completeOrder(currentOrderId, hitagId, 1, new BuyBuddyApiCallback<BuyBuddyBase>() {
                    @Override
                    public void success(BuyBuddyApiObject<BuyBuddyBase> response) {

                    }

                    @Override
                    public void error(BuyBuddyApiError error) {

                    }
                });

                EventBus.getDefault().post(new HitagEventFromService(hitagId, HitagState.STATE_UNLOCKED).setEventType(3));
                deviceMap.get(hitagId).disconnect();

            }else if (HitagState.compare(HitagState.PASSWORD_OLD, value)) {

                tryingDevices.remove(hitagId);
                failedDevices.put(hitagId, HitagState.PASSWORD_OLD.ordinal());
                EventBus.getDefault().post(new HitagEventFromService(hitagId, HitagState.PASSWORD_OLD).setEventType(1));
                deviceMap.get(hitagId).disconnect();

            }else if (HitagState.compare(HitagState.PASSWORD_WRONG, value) || HitagState.compare(HitagState.RELEASE_VALIDATION_FAILED, value)) {

                tryingDevices.remove(hitagId);
                failedDevices.put(hitagId, HitagState.PASSWORD_WRONG.ordinal());
                EventBus.getDefault().post(new HitagEventFromService(hitagId, HitagState.PASSWORD_WRONG).setEventType(1));
                deviceMap.get(hitagId).disconnect();


            }else if (HitagState.compare(HitagState.RELEASE_VALIDATION_SUCCESS, value)) {


            }else if (HitagState.compare(HitagState.STATE_UNLOCKING, value)) {
                EventBus.getDefault().post(new HitagEventFromService(hitagId, HitagState.STATE_UNLOCKING).setEventType(0));
            }
        }
    }

    @Override
    public void onCharacteristicRead(final String hitagId, Hitag.Characteristic chars, byte[] value) {

        if (deviceMap.get(hitagId).getConnectionState() == STATE_CONNECTED
                && tryingDevices.contains(hitagId) && chars == Hitag.Characteristic.PASSWORD_VERSION) {

            BuyBuddyUtil.printD(TAG, Integer.parseInt(BuyBuddyBleUtils.printHexBinary(value), 16) + "");

            BuyBuddy.getInstance().api.getHitagPassword(hitagId, currentOrderId, Integer.parseInt(BuyBuddyBleUtils.printHexBinary(value), 16),
            new BuyBuddyApiCallback<HitagPasswordPayload>() {
                @Override
                public void success(BuyBuddyApiObject<HitagPasswordPayload> response) {
                    deviceMap.get(hitagId).releaseHitag(response.getData());
                }

                @Override
                public void error(BuyBuddyApiError error) {
                    Log.d("*x**", error.getResponseCode() + "");
                    Log.d("*x**", "WRONG PASS VERSION");

                    if (error.getResponseCode() == 401) {

                    } else {

                    }

                    EventBus.getDefault().post(new HitagEventFromService(hitagId, HitagState.RELEASE_VALIDATION_FAILED).setEventType(1));
                    if (tryingDevices.contains(hitagId)) {
                        failedDevices.put(hitagId, HitagState.RELEASE_VALIDATION_FAILED.ordinal());
                        tryingDevices.remove(hitagId);
                        deviceMap.get(hitagId).disconnect();
                    }
                }
            });
        }
    }

    @Override
    public void onDeviceStuck(String hitagId, HitagState state) {

        EventBus.getDefault().post(new HitagEventFromService(hitagId, HitagState.STATE_BUGGY).setEventType(1));
        if (tryingDevices.contains(hitagId)) {
            tryingDevices.remove(hitagId);
            failedDevices.put(hitagId, HitagState.STATE_BUGGY.ordinal());

            if (deviceMap.get(hitagId) != null)
                deviceMap.get(hitagId).disconnect();
        }
    }
}
