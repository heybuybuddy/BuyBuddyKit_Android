package co.buybuddy.sdk;

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
import android.widget.Toast;

import com.polidea.rxandroidble.RxBleClient;
import com.polidea.rxandroidble.exceptions.BleScanException;
import com.polidea.rxandroidble.scan.ScanFilter;
import com.polidea.rxandroidble.scan.ScanResult;
import com.polidea.rxandroidble.scan.ScanSettings;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import co.buybuddy.sdk.interfaces.BuyBuddyApiCallback;
import co.buybuddy.sdk.responses.BuyBuddyApiError;
import co.buybuddy.sdk.responses.BuyBuddyApiObject;
import co.buybuddy.sdk.responses.BuyBuddyBase;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func1;

import static com.polidea.rxandroidble.scan.ScanSettings.SCAN_MODE_BALANCED;


/**
 * Created by furkan on 6/12/17.
 * Gururla sunar. AHAHAHAHA Some spagetties
 */

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
final public class HitagScanService extends Service {

    private Handler mBetweenHandler, mHitagReportHandler, mHandler;
    private AlarmManager hitagAlarm;
    private Context context;
    private Toast toaster;
    private int reportCount = 0;

    private final static String TAG = "HitagScanService";

    private long lastHitagTimeStamp;
    private boolean hitagStateActive = true;

    Subscription scanSubscription, flowSubscription;
    RxBleClient rxBleClient;

    static Map<String, CollectedHitagTS> activeHitags;
    static Map<String, CollectedHitagTS> passiveHitags;
    static ArrayList<CollectedHitag> collectedHitags;


    @Override
    public void onCreate() {
        super.onCreate();

        BuyBuddyUtil.printD("HitagScanService", "onCreate");

        rxBleClient = RxBleClient.create(this);
        lastHitagTimeStamp = System.currentTimeMillis();
        mHandler = new Handler();
        mBetweenHandler = new Handler();
        mHitagReportHandler = new Handler();
        scanLeDevice(false);
        context = getApplicationContext();

        activeHitags = new HashMap<>();
        passiveHitags = new HashMap<>();
        collectedHitags = new ArrayList<>();

        startReporter();
        startAlarmManager();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        return START_STICKY;
    }

    private void scanLeDevice(boolean fast) {

        if (scanSubscription != null && scanSubscription.isUnsubscribed())
            scanSubscription.unsubscribe();

            startScanWithHandler();
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
                activeHitags.remove(hitagId);
            }

        }

        reportCount++;
        if (reportCount == 3) {

            Iterator<CollectedHitagTS> it = activeHitags.values().iterator();

            while (it.hasNext()){

                collectedHitags.add(it.next().getWithoutTS());
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

        if (!scanSubscription.isUnsubscribed())
            scanSubscription.unsubscribe();

        BuyBuddyUtil.printD("Scan Service", "stop scan");
        mHandler.postDelayed(startScanRunnable,
                             hitagStateActive ? BuyBuddyBleUtils.HITAG_SCAN_BETWEEN_INTERVAL_ACTIVE : BuyBuddyBleUtils.HITAG_SCAN_BETWEEN_INTERVAL_IDLE);
    }

    private void subscribeScan() {
        BuyBuddyUtil.printD("Scan Service", "start Scan");

        scanSubscription = rxBleClient.scanBleDevices(
                new ScanSettings.Builder().setScanMode(SCAN_MODE_BALANCED).build(),
                new ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString(BuyBuddyBleUtils.MAIN_PREFIX + BuyBuddyBleUtils.MAIN_POSTFIX)).build()
        ).subscribe(new Action1<ScanResult>() {
            @Override
            public void call(ScanResult scanResult) {

                CollectedHitagTS hitag = CollectedHitagTS.getHitag(scanResult.getBleDevice(),
                                                                   scanResult.getScanRecord().getBytes(),
                                                                   scanResult.getRssi());

                if (hitag != null) {
                    //BuyBuddyUtil.printD(TAG, "Hitag: " + hitag.getId());
                    lastHitagTimeStamp = System.currentTimeMillis();
                    hitag.setLastSeen(lastHitagTimeStamp);
                    activeHitags.put(hitag.getId(), hitag);

                    if (!hitagStateActive) {
                        hitagStateActive = true;
                        stopScanHandler();
                    }
                }


            }
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {
                if (throwable instanceof BleScanException) {
                    BuyBuddyBleUtils.handBleScanExeption((BleScanException) throwable);
                }
            }
        });
    }

    private void startScanWithHandler() {
        hitagStateActive = System.currentTimeMillis() - lastHitagTimeStamp <= 31500;

        subscribeScan();
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

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
