package co.buybuddy.android;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.polidea.rxandroidble.RxBleClient;
import com.polidea.rxandroidble.exceptions.BleScanException;
import com.polidea.rxandroidble.scan.ScanFilter;
import com.polidea.rxandroidble.scan.ScanResult;
import com.polidea.rxandroidble.scan.ScanSettings;


import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func1;

import static co.buybuddy.android.BuyBuddyBleUtils.HITAG_SCAN_BETWEEN_INTERVAL_ACTIVE;
import static co.buybuddy.android.BuyBuddyBleUtils.HITAG_SCAN_BETWEEN_INTERVAL_IDLE;
import static co.buybuddy.android.BuyBuddyBleUtils.HITAG_SCAN_INTERVAL_ACTIVE;
import static co.buybuddy.android.BuyBuddyBleUtils.HITAG_SCAN_INTERVAL_IDLE;
import static co.buybuddy.android.BuyBuddyBleUtils.HITAG_TYPE_BEACON;
import static co.buybuddy.android.BuyBuddyBleUtils.HITAG_TYPE_CUSTOM;
import static co.buybuddy.android.BuyBuddyBleUtils.MAIN_POSTFIX;
import static co.buybuddy.android.BuyBuddyBleUtils.MAIN_PREFIX;
import static com.polidea.rxandroidble.scan.ScanSettings.SCAN_MODE_LOW_POWER;


/**
 * Created by furkan on 6/12/17.
 * Gururla sunar. AHAHAHAHA Some spagetties
 */

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
final class HitagScanService extends Service{

    private Handler mBetweenHandler, mHitagReportHandler, mHandler;
    private AlarmManager hitagAlarm;
    private Context context;
    private Toast toaster;
    private int reportCount = 0;

    private long lastHitagTimeStamp;
    private boolean hitagStateActive = true;

    Subscription scanSubscription, flowSubscription;
    RxBleClient rxBleClient;

    private static Map<String, CollectedHitagTS> activeHitags;
    private static Map<String, CollectedHitagTS> passiveHitags;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        rxBleClient = RxBleClient.create(this);
        lastHitagTimeStamp = System.currentTimeMillis();
        mHandler = new Handler();
        mBetweenHandler = new Handler();
        mHitagReportHandler = new Handler();
        scanLeDevice(false);
        context = getApplicationContext();
        toaster = Toast.makeText(context, "YEP", Toast.LENGTH_SHORT);

        activeHitags = new HashMap<>();
        passiveHitags = new HashMap<>();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    private void scanLeDevice(boolean fast) {

        if (scanSubscription != null && scanSubscription.isUnsubscribed())
            scanSubscription.unsubscribe();

            startScanWithHandler();
    }

    private void startAlarmManager() {

        Intent intent = new Intent(this, HitagScanService.class);
        PendingIntent pintent = PendingIntent.getService(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        hitagAlarm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        hitagAlarm.setInexactRepeating(AlarmManager.RTC_WAKEUP,
                                       (System.currentTimeMillis() + BuyBuddyUtil.HITAG_MANAGER_ALARM_INTERVAL),
                                       BuyBuddyUtil.HITAG_MANAGER_ALARM_INTERVAL,
                                       pintent);

    }

    private void startReporter() {

        reportCount++;

        mHitagReportHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                startReporter();

            }
        }, 1000);
    }

    private void stopScanHandler() {

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!scanSubscription.isUnsubscribed())
                    scanSubscription.unsubscribe();

                startScanWithHandler();
            }
        }, hitagStateActive ? HITAG_SCAN_INTERVAL_ACTIVE : HITAG_SCAN_INTERVAL_IDLE);
    }

    private void subscribeScan() {
        scanSubscription = rxBleClient.scanBleDevices(
                new ScanSettings.Builder().setScanMode(SCAN_MODE_LOW_POWER).build(),
                new ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString(MAIN_PREFIX + MAIN_POSTFIX)).build()
        ).subscribe(new Action1<ScanResult>() {
            @Override
            public void call(ScanResult scanResult) {

                CollectedHitagTS hitag = CollectedHitagTS.getHitag(scanResult.getBleDevice(),
                                                                   scanResult.getScanRecord().getBytes(),
                                                                   scanResult.getRssi());

                if (hitag != null) {


                    toaster.setText("YEAP");
                    toaster.show();
                }






            }
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {
                if (throwable instanceof BleScanException) {
                    handleBleScanException((BleScanException) throwable);
                }
            }
        });
    }

    private void startScanWithHandler() {

        hitagStateActive = System.currentTimeMillis() - lastHitagTimeStamp <= 30000;

        mBetweenHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                subscribeScan();
                stopScanHandler();
            }
        }, hitagStateActive ? HITAG_SCAN_BETWEEN_INTERVAL_ACTIVE : HITAG_SCAN_BETWEEN_INTERVAL_IDLE);
    }

    private void subScribeBleFlow(){
        flowSubscription = rxBleClient.observeStateChanges()
                .switchMap(new Func1<RxBleClient.State, Observable<?>>() {
                    @Override
                    public Observable<?> call(RxBleClient.State state) {

                        switch (state) {
                            case READY:

                                break;
                            case BLUETOOTH_NOT_AVAILABLE:
                                break;
                            case BLUETOOTH_NOT_ENABLED:
                                break;
                            case LOCATION_PERMISSION_NOT_GRANTED:
                                break;
                            case LOCATION_SERVICES_NOT_ENABLED:
                                break;
                        }

                        return null;
                    }
                }).subscribe();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (flowSubscription != null && flowSubscription.isUnsubscribed()) {
            flowSubscription.unsubscribe();
        }

        if (scanSubscription != null && scanSubscription.isUnsubscribed()) {
            scanSubscription.unsubscribe();
        }

        if (activeHitags != null) {
            activeHitags.clear();
            activeHitags = null;
        }

        if (passiveHitags != null) {
            passiveHitags.clear();
            passiveHitags = null;
        }
    }

    private void handleBleScanException(BleScanException bleScanException) {

        switch (bleScanException.getReason()) {
            case BleScanException.BLUETOOTH_NOT_AVAILABLE:
                Log.d("ERROR", "Bluetooth is not available");
                break;
            case BleScanException.BLUETOOTH_DISABLED:
                Log.d("ERROR", "Enable bluetooth and try again");
                break;
            case BleScanException.LOCATION_PERMISSION_MISSING:
                Log.d("ERROR", "On Android 6.0 location permission is required");
                break;
            case BleScanException.LOCATION_SERVICES_DISABLED:
                Log.d("ERROR", "Location services needs to be enabled on Android 6.0");
                break;
            case BleScanException.SCAN_FAILED_ALREADY_STARTED:
                Log.d("ERROR", "Scan with the same filters is already started");
                break;
            case BleScanException.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED:
                Log.d("ERROR", "Failed to register application for bluetooth scan");
                break;
            case BleScanException.SCAN_FAILED_FEATURE_UNSUPPORTED:
                Log.d("ERROR", "Scan with specified parameters is not supported");
                break;
            case BleScanException.SCAN_FAILED_INTERNAL_ERROR:
                Log.d("ERROR", "Scan failed due to internal error");
                break;
            case BleScanException.SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES:
                Log.d("ERROR", "Scan cannot start due to limited hardware resources");
                break;
            case BleScanException.UNKNOWN_ERROR_CODE:
            case BleScanException.BLUETOOTH_CANNOT_START:
            default:
                Log.d("ERROR", "Unable to start scanning");
                break;
        }
    }
}
