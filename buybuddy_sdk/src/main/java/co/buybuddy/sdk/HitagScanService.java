package co.buybuddy.sdk;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import co.buybuddy.sdk.ble.BuyBuddyBleUtils;
import co.buybuddy.sdk.ble.CollectedHitag;
import co.buybuddy.sdk.ble.CollectedHitagTS;
import co.buybuddy.sdk.ble.exception.HitagReleaserBleException;
import co.buybuddy.sdk.interfaces.BuyBuddyApiCallback;
import co.buybuddy.sdk.responses.BuyBuddyApiError;
import co.buybuddy.sdk.responses.BuyBuddyApiObject;
import co.buybuddy.sdk.responses.BuyBuddyBase;


/**
 * Created by Furkan Ençkü on 6/12/17.
 * This code written by buybuddy Android Team
 */

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
final public class HitagScanService extends Service {

    private Handler mBetweenHandler, mHitagReportHandler, mHandler;
    private AlarmManager hitagAlarm;
    private Context context;
    private BluetoothAdapter mBluetoothAdapter;
    private ScanCallback mLeScanCallback;
    private Toast toaster;
    private int reportCount = 0;

    private final static String TAG = "HitagScanService";

    private long lastHitagTimeStamp;
    private boolean hitagStateActive = true;
    private boolean mScanning = false;

    static Map<String, CollectedHitagTS> activeHitags;
    static Map<String, CollectedHitagTS> passiveHitags;
    static ArrayList<CollectedHitag> collectedHitags;

    private void initStartBluetoothScan() {

        if (isScannable(mBluetoothAdapter)){
            startScanning();

            startReporter();
            startAlarmManager();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        BuyBuddyUtil.printD("HitagScanService", "onCreate");

        lastHitagTimeStamp = System.currentTimeMillis();
        mHandler = new Handler();
        mBetweenHandler = new Handler();
        mHitagReportHandler = new Handler();
        context = getApplicationContext();

        activeHitags = new HashMap<>();
        passiveHitags = new HashMap<>();
        collectedHitags = new ArrayList<>();

        initStartBluetoothScan();
    }

    private boolean isScannable(BluetoothAdapter adapter) {

        PackageManager pm = getPackageManager();
        boolean hasBLE = pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);

        try {
            if (adapter == null) {
                throw new HitagReleaserBleException(HitagReleaserBleException.BLUETOOTH_NOT_AVAILABLE);
            } else if (!adapter.isEnabled()) {
                throw new HitagReleaserBleException(HitagReleaserBleException.BLUETOOTH_DISABLED);
            } else if (!BuyBuddy.getInstance().getLocationServicesStatus().isLocationPermissionOk()) {
                throw new HitagReleaserBleException(HitagReleaserBleException.LOCATION_PERMISSION_MISSING);
            } else if (!BuyBuddy.getInstance().getLocationServicesStatus().isLocationProviderOk()) {
                throw new HitagReleaserBleException(HitagReleaserBleException.LOCATION_SERVICES_DISABLED);
            } else if (!hasBLE) {
                throw new HitagReleaserBleException(HitagReleaserBleException.BLUETOOTH_LE_NOT_AVAILABLE);
            }

            return true;
        } catch (HitagReleaserBleException ex) {

            return false;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        initStartBluetoothScan();

        return START_STICKY;
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

    private void scanLeDevice18(boolean enable) {

        final BluetoothAdapter.LeScanCallback mLeScanCallback =
                new BluetoothAdapter.LeScanCallback() {
                    @Override
                    public void onLeScan(final BluetoothDevice bluetoothDevice, int rssi, byte[] scanRecord) {

                        CollectedHitagTS hitag = CollectedHitagTS.getHitag(bluetoothDevice,
                                scanRecord,
                                rssi);

                        if (hitag != null) {
                            lastHitagTimeStamp = System.currentTimeMillis();
                            hitag.setLastSeen(lastHitagTimeStamp);

                            if (activeHitags.containsKey(hitag.getId())) {
                                CollectedHitagTS currentHitag = activeHitags.get(hitag.getId());

                                if (hitag.isBeacon()) {
                                    currentHitag.setRssi(hitag.getRssi());
                                    currentHitag.setLastSeen(hitag.getLastSeen());

                                    //activeHitags.put(hitag.getId(), currentHitag);
                                } else {
                                    activeHitags.put(hitag.getId(), hitag);
                                }
                            } else {
                                if (!hitag.isBeacon())
                                    activeHitags.put(hitag.getId(), hitag);
                            }

                            activeHitags.put(hitag.getId(), hitag);

                            if (!hitagStateActive) {
                                hitagStateActive = true;
                                stopScanHandler();
                            }
                        }

                    }
                };
        if (enable) {
            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
    }

    private void scanLeDevice21(boolean enable) {
        if (mLeScanCallback == null) {
            mLeScanCallback = new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    super.onScanResult(callbackType, result);

                    if (result.getScanRecord() != null) {

                        CollectedHitagTS hitag = CollectedHitagTS.getHitag(result.getDevice(),
                                result.getScanRecord().getBytes(),
                                result.getRssi());

                        if (hitag != null) {

                            lastHitagTimeStamp = System.currentTimeMillis();
                            hitag.setLastSeen(lastHitagTimeStamp);

                            if (activeHitags.containsKey(hitag.getId())) {
                                CollectedHitagTS currentHitag = activeHitags.get(hitag.getId());

                                if (hitag.isBeacon()) {
                                    currentHitag.setRssi(hitag.getRssi());
                                    currentHitag.setLastSeen(hitag.getLastSeen());

                                    //activeHitags.put(currentHitag.getId(), hitag);
                                } else {
                                    activeHitags.put(hitag.getId(), hitag);
                                }
                            } else {
                                if (!hitag.isBeacon())
                                    activeHitags.put(hitag.getId(), hitag);
                            }

                            if (!hitagStateActive) {
                                hitagStateActive = true;
                                stopScanHandler();
                            }
                        }
                    }
                }

                @Override
                public void onScanFailed(int errorCode) {
                    super.onScanFailed(errorCode);
                }
            };
        }

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

    public static boolean validateActiveHitag(String hitagId) {

        Iterator<String> iter = activeHitags.keySet().iterator();

        while (iter.hasNext()) {
            String key = iter.next();

            if(hitagId.equals(key)){
                return true;
            }
        }
        return false;
    }

     void startAlarmManager() {

        hitagAlarm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, HitagScanService.class);
        PendingIntent pintent = PendingIntent.getService(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        hitagAlarm.setInexactRepeating(AlarmManager.RTC_WAKEUP,
                                       (System.currentTimeMillis() + BuyBuddyUtil.HITAG_MANAGER_ALARM_INTERVAL),
                                       BuyBuddyUtil.HITAG_MANAGER_ALARM_INTERVAL,
                                       pintent);
    }

    private void startReporter() {
        long currentTime = System.currentTimeMillis();

        Iterator<String> iter = activeHitags.keySet().iterator();

        while (iter.hasNext()) {
            String hitagId = iter.next();

            if (currentTime - activeHitags.get(hitagId).getLastSeen() > 5000) {
                passiveHitags.put(hitagId, activeHitags.get(hitagId));
                iter.remove();
            }
        }

        reportCount++;
        if (reportCount == 3) {

            for (CollectedHitagTS collectedHitagTS : activeHitags.values()) {
                collectedHitags.add(collectedHitagTS.getWithoutTS());
            }

            if (!collectedHitags.isEmpty()){

                BuyBuddy.getInstance().api.postScanRecord(collectedHitags, new BuyBuddyApiCallback<BuyBuddyBase>() {
                    @Override
                    public void success(BuyBuddyApiObject<BuyBuddyBase> response) {

                    }

                    @Override
                    public void error(BuyBuddyApiError error) {

                    }
                });


            }
            collectedHitags.clear();
            reportCount = 0;
        }


        mHitagReportHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                startReporter();

            }
        }, 1000);
    }

    private void stopScanHandler() {

        mHandler.removeCallbacks(startScanRunnable);
        mBetweenHandler.removeCallbacks(stopScanRunnable);

        stopScanning();
        mHandler.postDelayed(startScanRunnable,
                             hitagStateActive ? BuyBuddyBleUtils.HITAG_SCAN_BETWEEN_INTERVAL_ACTIVE : BuyBuddyBleUtils.HITAG_SCAN_BETWEEN_INTERVAL_IDLE);
    }

    private void startScanWithHandler() {
        hitagStateActive = System.currentTimeMillis() - lastHitagTimeStamp <= 31500;

        startScanning();
        mBetweenHandler.postDelayed(stopScanRunnable,
                                    hitagStateActive ? BuyBuddyBleUtils.HITAG_SCAN_INTERVAL_ACTIVE : BuyBuddyBleUtils.HITAG_SCAN_INTERVAL_IDLE);
    }

    Runnable stopScanRunnable = new Runnable() {
        @Override
        public void run() {
            stopScanHandler();
        }
    };

    Runnable startScanRunnable = new Runnable() {
        @Override
        public void run() {
            startScanWithHandler();
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (activeHitags != null) {
            activeHitags.clear();
            activeHitags = null;
        }

        if (passiveHitags != null) {
            passiveHitags.clear();
            passiveHitags = null;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
