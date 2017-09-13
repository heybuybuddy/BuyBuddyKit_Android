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
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import co.buybuddy.sdk.BuyBuddy;
import co.buybuddy.sdk.BuyBuddyBluetoothState;
import co.buybuddy.sdk.BuyBuddyUtil;
import co.buybuddy.sdk.ble.exception.BleScanException;
import co.buybuddy.sdk.interfaces.BuyBuddyApiCallback;
import co.buybuddy.sdk.model.HitagPasswordPayload;
import co.buybuddy.sdk.responses.BuyBuddyApiError;
import co.buybuddy.sdk.responses.BuyBuddyApiObject;
import co.buybuddy.sdk.responses.BuyBuddyBase;
import co.buybuddy.sdk.responses.OrderDelegateDetail;

import static android.bluetooth.BluetoothProfile.STATE_CONNECTED;
import static android.bluetooth.BluetoothProfile.STATE_DISCONNECTED;

/**
 * Created by Furkan Ençkü on 8/22/17.
 * This code written by buybuddy Android Team
 */

class BuyBuddyHitagReleaser extends Service implements Hitag.Delegate {


    private static long HITAG_DEFAULT_SCAN_TIMEOUT = 10000L;
    BluetoothAdapter mBluetoothAdapter;
    BluetoothManager mBluetoothManager;
    private ScanCallback mLeScanCallback;
    Handler mHandler;
    boolean mScanning = false;
    long currentOrderId = -1;

    HashSet<String> willOpenDevices;
    HashMap<String, Integer> tryCountForDevices;
    HashSet<String> tryingDevices;
    HashSet<String> openedDevices;
    HashMap<String, Integer> failedDevices;

    HashMap<String, Hitag> deviceMap;

    final String TAG = "BBuddyHRS";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mHandler = new Handler();

        mBluetoothManager = (BluetoothManager) this.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        EventBus.getDefault().register(this);

        deviceMap = new HashMap<>();
        willOpenDevices = new HashSet<>();
        failedDevices = new HashMap<>();
        openedDevices = new HashSet<>();
        tryingDevices = new HashSet<>();
        tryCountForDevices = new HashMap<>();
    }

    private boolean checkAllConditions() {
        PackageManager pm = getPackageManager();
        boolean hasBLE = pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);

        try {
            if (mBluetoothAdapter == null) {
                throw new BleScanException(BleScanException.BLUETOOTH_NOT_AVAILABLE);
            } else if (!mBluetoothAdapter.isEnabled()) {
                throw new BleScanException(BleScanException.BLUETOOTH_DISABLED);
            } else if (!BuyBuddy.getInstance().getLocationServicesStatus().isLocationPermissionOk()) {
                throw new BleScanException(BleScanException.LOCATION_PERMISSION_MISSING);
            } else if (!BuyBuddy.getInstance().getLocationServicesStatus().isLocationProviderOk()) {
                throw new BleScanException(BleScanException.LOCATION_SERVICES_DISABLED);
            } else if (!hasBLE) {
                throw new BleScanException(BleScanException.BLUETOOTH_LE_NOT_AVAILABLE);
            }
        } catch (BleScanException ex) {
            EventBus.getDefault().post(ex);
            return false;
        }

        return true;
    }

    @RequiresApi(21)
    private void scanLeDevice21(final boolean enable) {

        if (mLeScanCallback == null)
            mLeScanCallback = new ScanCallback() {

                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    super.onScanResult(callbackType, result);

                    if (willOpenDevices.size() == 0 && tryingDevices.size() == 0) {
                        stopScanning();
                        EventBus.getDefault().post(new HitagEventFromService(null, null).setEventType(2));
                        stopSelf();
                    }

                    if (result != null) {
                        if (result.getScanRecord() != null) {
                           if (result.getScanRecord().getBytes() != null) {

                                CollectedHitagTS hitag = CollectedHitagTS.getHitag(null, result.getScanRecord().getBytes(), result.getRssi());

                                if (hitag != null) {

                                    BuyBuddyUtil.printD(TAG, "Hitag Id: " + hitag.getId());

                                    if (willOpenDevices.contains(hitag.getId())){
                                        willOpenDevices.remove(hitag.getId());

                                        BuyBuddyUtil.printD(TAG, "Connectable Device Count :" +
                                                (BuyBuddyBleUtils.getMaximumConnectableDeviceCount() - getConnectedDeviceCount()));

                                        if (BuyBuddyBleUtils.getMaximumConnectableDeviceCount() - (getConnectedDeviceCount() + tryingDevices.size()) > 1) {
                                            tryingDevices.add(hitag.getId());
                                            tryCountForDevices.put(hitag.getId(), 0);

                                            Hitag willConnectHitag = new Hitag(BuyBuddyHitagReleaser.this, result.getDevice())
                                                                                    .setHitagDelegate(BuyBuddyHitagReleaser.this)
                                                                                    .setHitagId(hitag.getId());

                                            deviceMap.put(hitag.getId(), willConnectHitag);

                                            BuyBuddyUtil.printD(TAG, "Connection or Connected or Finished");
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                @Override
                public void onBatchScanResults(List<ScanResult> results) {
                    super.onBatchScanResults(results);
                }

                @Override
                public void onScanFailed(int errorCode) {
                    super.onScanFailed(errorCode);
                }
            };

        final BluetoothLeScanner bluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();

        if (enable) {

            ScanSettings settings = new ScanSettings.Builder()
                    .setScanMode(android.bluetooth.le.ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build();

            bluetoothLeScanner.startScan(new ArrayList<ScanFilter>(), settings, mLeScanCallback);

            mScanning = true;

        } else {
            mScanning = false;
            bluetoothLeScanner.stopScan(mLeScanCallback);
        }
    }

    private int getConnectedDeviceCount() {
        return mBluetoothManager.
                getConnectedDevices(BluetoothProfile.GATT).size();
    }

    private void scanLeDevice18(boolean enable) {

        final BluetoothAdapter.LeScanCallback mLeScanCallback =
                new BluetoothAdapter.LeScanCallback() {
                    @Override
                    public void onLeScan(final BluetoothDevice bluetoothDevice, int rssi, byte[] scanRecord) {



                    }
                };
        if (enable) {

            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                }
            }, HITAG_DEFAULT_SCAN_TIMEOUT);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onBluetoothStateEvent(BuyBuddyBluetoothState state) {
        BuyBuddyUtil.printD(TAG, state.name());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        Bundle extras = intent.getExtras();

        if (extras != null) {

            long orderId = extras.getLong("orderId");
            currentOrderId = orderId;

            BuyBuddy.getInstance().api.getOrderDetail(orderId, new BuyBuddyApiCallback<OrderDelegateDetail>() {

                @Override
                public void success(BuyBuddyApiObject<OrderDelegateDetail> response) {

                    willOpenDevices.addAll(Arrays.asList(response.getData().getHitagIds()));

                    if (willOpenDevices.size() > 0) {
                        if (checkAllConditions()) {
                            startScanning();
                        }
                    }
                }

                @Override
                public void error(BuyBuddyApiError error) {

                }
            });
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
        currentOrderId = -1;
    }

    private void startScanning() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            scanLeDevice21(true);
        } else {
            scanLeDevice18(true);
        }
    }

    private void stopScanning() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            scanLeDevice21(false);
        } else {
            scanLeDevice18(false);
        }
    }

    @Override
    public void connectionStateChanged(String hitagId, int state) {

        switch (state) {
            case STATE_CONNECTED:
                BuyBuddyUtil.printD(TAG, "Hitag Id: " + hitagId + " connected");
                EventBus.getDefault().post(new HitagEventFromService(hitagId, Hitag.State.CONNECTED).setEventType(0));
                break;

            case STATE_DISCONNECTED:
                BuyBuddyUtil.printD(TAG, "Hitag Id: " + hitagId + " disconnected");
                EventBus.getDefault().post(new HitagEventFromService(hitagId, Hitag.State.DISCONNECTED).setEventType(0));

                break;

            case 99: //CONNECTION UNSUCCESSFUL

                if (tryingDevices.contains(hitagId) && deviceMap.get(hitagId).getCurrentState() == Hitag.State.INITIALIZING) {
                    tryCountForDevices.put(hitagId, tryCountForDevices.get(hitagId) + 1);

                    if (tryCountForDevices.get(hitagId) > 2) {
                        tryingDevices.remove(hitagId);
                        failedDevices.put(hitagId, Hitag.State.CONNECTION_FAILED.ordinal());
                        EventBus.getDefault().post(new HitagEventFromService(hitagId, Hitag.State.CONNECTION_FAILED).setEventType(1));
                    }else {
                        if (deviceMap.get(hitagId) != null) {
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

            if (Hitag.State.compare(Hitag.State.STATE_UNLOCKED, value)) {

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

                EventBus.getDefault().post(new HitagEventFromService(hitagId, Hitag.State.STATE_UNLOCKED).setEventType(3));
                deviceMap.get(hitagId).disconnect();

            }else if (Hitag.State.compare(Hitag.State.PASSWORD_OLD, value)) {

                tryingDevices.remove(hitagId);
                failedDevices.put(hitagId, Hitag.State.PASSWORD_OLD.ordinal());
                EventBus.getDefault().post(new HitagEventFromService(hitagId, Hitag.State.PASSWORD_OLD).setEventType(1));
                deviceMap.get(hitagId).disconnect();

            }else if (Hitag.State.compare(Hitag.State.PASSWORD_WRONG, value) || Hitag.State.compare(Hitag.State.RELEASE_VALIDATION_FAILED, value)) {

                tryingDevices.remove(hitagId);
                failedDevices.put(hitagId, Hitag.State.PASSWORD_WRONG.ordinal());
                EventBus.getDefault().post(new HitagEventFromService(hitagId, Hitag.State.PASSWORD_WRONG).setEventType(1));
                deviceMap.get(hitagId).disconnect();


            }else if (Hitag.State.compare(Hitag.State.RELEASE_VALIDATION_SUCCESS, value)) {


            }else if (Hitag.State.compare(Hitag.State.STATE_UNLOCKING, value)) {
                EventBus.getDefault().post(new HitagEventFromService(hitagId, Hitag.State.STATE_UNLOCKING).setEventType(0));
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
                    EventBus.getDefault().post(new HitagEventFromService(hitagId, Hitag.State.PASSWORD_WRONG).setEventType(1));
                    if (tryingDevices.contains(hitagId)) {
                        failedDevices.put(hitagId, Hitag.State.PASSWORD_WRONG.ordinal());
                        tryingDevices.remove(hitagId);
                        deviceMap.get(hitagId).disconnect();
                    }
                }
            });

        }
    }

    @Override
    public void onDeviceStuck(String hitagId, Hitag.State state) {

        if (tryingDevices.contains(hitagId)) {
            tryingDevices.remove(hitagId);
            failedDevices.put(hitagId, Hitag.State.STATE_BUGGY.ordinal());

            if (deviceMap.get(hitagId) != null)
                deviceMap.get(hitagId).disconnect();
        }

    }
}
