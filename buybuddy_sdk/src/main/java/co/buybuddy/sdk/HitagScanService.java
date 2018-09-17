package co.buybuddy.sdk;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import co.buybuddy.sdk.ble.BuyBuddyBleUtils;
import co.buybuddy.sdk.ble.CollectedHitag;
import co.buybuddy.sdk.ble.CollectedHitagTS;
import co.buybuddy.sdk.ble.blecompat.BluetoothLeScannerCompat;
import co.buybuddy.sdk.ble.blecompat.ScanCallbackCompat;
import co.buybuddy.sdk.ble.blecompat.ScanResultCompat;
import co.buybuddy.sdk.ble.blecompat.ScanSettingsCompat;
import co.buybuddy.sdk.interfaces.BuyBuddyApiCallback;
import co.buybuddy.sdk.interfaces.BuyBuddyUserTokenExpiredDelegate;
import co.buybuddy.sdk.interfaces.HitagScanServiceCallBack;
import co.buybuddy.sdk.responses.BuyBuddyApiError;
import co.buybuddy.sdk.responses.BuyBuddyApiObject;
import co.buybuddy.sdk.responses.BuyBuddyBase;

/**
 * Created by Furkan Ençkü on 6/12/17.
 * This code written by buybuddy Android Team
 */

final public class HitagScanService extends Service  {

    private static Handler mBetweenHandler, mHitagReportHandler, mHandler;
    private AlarmManager hitagAlarm;
    private BluetoothAdapter mBluetoothAdapter;
    private int reportCount = 0;
    private BluetoothLeScannerCompat mBleScanner;
    private ScanCallbackCompat scanCallback;
    private Boolean regionActive = false;

    private final static String TAG = "HitagScanService";

    private long lastHitagTimeStamp;
    private long lastReportingLoopTimeStamp = -1;
    private boolean hitagStateActive = true;

    static Map<String, CollectedHitagTS> activeHitags = new HashMap<>();
    static Map<String, CollectedHitagTS> passiveHitags = new HashMap<>();
    static ArrayList<CollectedHitag> collectedHitags = new ArrayList<>();

    private void initStartBluetoothScan() {

        startScanWithHandler();

        if (lastReportingLoopTimeStamp == -1 || System.currentTimeMillis() - lastHitagTimeStamp > 10000) {
            if (mHitagReportHandler != null) {
                mHitagReportHandler.removeCallbacks(reportRunnable);
                mHitagReportHandler.postDelayed(reportRunnable, 1000);
            }
        }

        startAlarmManager();
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

        mBleScanner = new BluetoothLeScannerCompat(this);

        scanCallback = new ScanCallbackCompat() {
            @Override
            public void onScanResult(int callbackType, ScanResultCompat result) {
                super.onScanResult(callbackType, result);

                BuyBuddyUtil.printD(TAG, "scanResult");

                if (result.getScanRecord() != null) {

                    CollectedHitagTS hitag = CollectedHitagTS.getHitag(result.getDevice(),
                                                                       result.getScanRecord().getBytes(),
                                                                       result.getRssi());

                    if (hitag != null) {
                        BuyBuddy.getInstance().getStoreInfoProvider().getLocation(hitag.getId());

                        regionActive = true;

                        BuyBuddy.getInstance()
                                .getStoreInfoProvider()
                                .getLocation(hitag.getId());

                        lastHitagTimeStamp = System.currentTimeMillis();
                        hitag.setLastSeen(lastHitagTimeStamp);
                        hitag.setDevice(result.getDevice());

                        if (activeHitags.containsKey(hitag.getId())) {
                            CollectedHitagTS currentHitag = activeHitags.get(hitag.getId());

                            if (hitag.isBeacon()) {
                                currentHitag.setRssi(hitag.getRssi());
                                currentHitag.setLastSeen(hitag.getLastSeen());
                                activeHitags.put(hitag.getId(), currentHitag);
                            } else {
                                activeHitags.put(hitag.getId(), hitag);
                            }

                        } else if (!hitag.isBeacon()){
                            activeHitags.put(hitag.getId(), hitag);
                        }

                        if (!hitagStateActive) {
                            hitagStateActive = true;
                            stopScanHandler();
                        }

                        if(EventBus.getDefault().hasSubscriberForEvent(CollectedHitagTS.class)) {
                            EventBus.getDefault().post(hitag);
                        }

                    }
                }
            }

            @Override
            public void onScanFailed(int errorCode) {
                super.onScanFailed(errorCode);
            }
        };

        initStartBluetoothScan();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        initStartBluetoothScan();

        BuyBuddyUtil.printD(TAG, "onStart");

        return START_STICKY;
    }

    private void startScanning() {
        ScanSettingsCompat.Builder scanSettingsBuilder = new ScanSettingsCompat.Builder();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            scanSettingsBuilder.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);
        }
        scanSettingsBuilder.setReportDelay(0);

        mBleScanner.startScan(mBluetoothAdapter, null, scanSettingsBuilder.build(), scanCallback);
        BuyBuddyUtil.printD(TAG, "BLE SCAN ON");
    }

    private void stopScanning() {
        mBleScanner.stopScan(mBluetoothAdapter, scanCallback);
        BuyBuddyUtil.printD(TAG, "BLE SCAN OFF");
    }

    public static boolean validateActiveHitag(String hitagId) {

        if (activeHitags == null)
            return false;


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

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            hitagAlarm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(this, HitagScanService.class);
            PendingIntent pintent = PendingIntent.getService(this, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            hitagAlarm.setInexactRepeating(AlarmManager.RTC_WAKEUP,
                    (System.currentTimeMillis() + BuyBuddyUtil.HITAG_MANAGER_ALARM_INTERVAL),
                    BuyBuddyUtil.HITAG_MANAGER_ALARM_INTERVAL,
                    pintent);
        }
    }

    private void doReport() {
        long currentTime = System.currentTimeMillis();

        lastReportingLoopTimeStamp = currentTime;

        Iterator<String> iter = activeHitags.keySet().iterator();

        while (iter.hasNext()) {
            String hitagId = iter.next();

            if (currentTime - activeHitags.get(hitagId).getLastSeen() > 5000) {
                passiveHitags.put(hitagId, activeHitags.get(hitagId));
                iter.remove();
            }
        }

        if (activeHitags.isEmpty() && regionActive){
            regionActive = false;
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


            } else {
                BuyBuddyUtil.printD(TAG, "Reporter: Hitag not found.");
            }
            collectedHitags.clear();
            reportCount = 0;
        }

        mHitagReportHandler.postDelayed(reportRunnable, 1000);
    }

    private void stopScanHandler() {

        BuyBuddyUtil.printD(TAG, "stopScanHandler");

        mHandler.removeCallbacks(startScanRunnable);
        mBetweenHandler.removeCallbacks(stopScanRunnable);

        stopScanning();
        mHandler.postDelayed(startScanRunnable,
                             hitagStateActive ? BuyBuddyBleUtils.HITAG_SCAN_BETWEEN_INTERVAL_ACTIVE : BuyBuddyBleUtils.HITAG_SCAN_BETWEEN_INTERVAL_IDLE);
    }

    private void startScanWithHandler() {

        BuyBuddyUtil.printD(TAG, "startScanWithHandler");

        hitagStateActive = System.currentTimeMillis() - lastHitagTimeStamp <= 31500;

        startScanning();
        mBetweenHandler.postDelayed(stopScanRunnable,
                                    hitagStateActive ? BuyBuddyBleUtils.HITAG_SCAN_INTERVAL_ACTIVE : BuyBuddyBleUtils.HITAG_SCAN_INTERVAL_IDLE);
    }

    Runnable reportRunnable = new Runnable() {
        @Override
        public void run() {
            doReport();
        }
    };

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
        }

        if (passiveHitags != null) {
            passiveHitags.clear();
        }

        stopScanning();
        mHandler.removeCallbacks(startScanRunnable);
        mBetweenHandler.removeCallbacks(stopScanRunnable);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
