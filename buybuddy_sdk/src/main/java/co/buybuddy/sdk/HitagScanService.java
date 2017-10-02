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

import com.forkingcode.bluetoothcompat.BluetoothLeScannerCompat;
import com.forkingcode.bluetoothcompat.ScanCallbackCompat;
import com.forkingcode.bluetoothcompat.ScanResultCompat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
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
final public class HitagScanService extends Service  {

    private static Handler mBetweenHandler, mHitagReportHandler, mHandler;
    private AlarmManager hitagAlarm;
    private BluetoothAdapter mBluetoothAdapter;
    private int reportCount = 0;
    private BluetoothLeScannerCompat mBleScanner;
    private ScanCallbackCompat scanCallback;

    private final static String TAG = "HitagScanService";

    private long lastHitagTimeStamp;
    private boolean hitagStateActive = true;

    static Map<String, CollectedHitagTS> activeHitags;
    static Map<String, CollectedHitagTS> passiveHitags;
    static ArrayList<CollectedHitag> collectedHitags;

    private void initStartBluetoothScan() {

        startScanning();
        doReport();
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

        activeHitags = new HashMap<>();
        passiveHitags = new HashMap<>();
        collectedHitags = new ArrayList<>();

        mBleScanner = new BluetoothLeScannerCompat(this);

        scanCallback = new ScanCallbackCompat() {
            @Override
            public void onScanResult(int callbackType, ScanResultCompat result) {
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

        return START_STICKY;
    }

    private void startScanning() {
        mBleScanner.stopScan(mBluetoothAdapter, scanCallback);
        mBleScanner.startScan(mBluetoothAdapter, scanCallback);
    }

    private void stopScanning() {
        mBleScanner.stopScan(mBluetoothAdapter, scanCallback);
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

    private void doReport() {
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
                doReport();

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
