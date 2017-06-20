package co.buybuddy.android;

import android.content.Context;
import android.os.Handler;
import android.os.ParcelUuid;

import com.polidea.rxandroidble.RxBleClient;
import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.RxBleDevice;
import com.polidea.rxandroidble.exceptions.BleScanException;
import com.polidea.rxandroidble.scan.ScanFilter;
import com.polidea.rxandroidble.scan.ScanResult;
import com.polidea.rxandroidble.scan.ScanSettings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import co.buybuddy.android.interfaces.BuyBuddyApiCallback;
import co.buybuddy.android.responses.BuyBuddyApiError;
import co.buybuddy.android.responses.BuyBuddyApiObject;
import co.buybuddy.android.responses.BuyBuddyBase;
import co.buybuddy.android.responses.OrderDelegateDetail;
import rx.Emitter;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func1;

import static co.buybuddy.android.BuyBuddyBleUtils.HITAG_RX;
import static co.buybuddy.android.BuyBuddyBleUtils.HITAG_TX;
import static co.buybuddy.android.BuyBuddyBleUtils.MAIN_POSTFIX;
import static co.buybuddy.android.BuyBuddyBleUtils.MAIN_PREFIX;
import static com.polidea.rxandroidble.RxBleConnection.*;
import static com.polidea.rxandroidble.scan.ScanSettings.SCAN_MODE_LOW_POWER;

/**
 * Created by furkan on 6/14/17.
 * Gururla sunar. AHAHAHAHA Some spagetties
 */


public class BuyBuddyHitagReleaser {

    private final static long CONNECTION_TIMEOUT = 5000;
    private final static long PROCESS_TIMEOUT = 5000;

    private Set<String> willOpenHitags;
    private Set<String> completedHitags;
    private Set<String> incompletedHitags;
    private String currentHitagId;

    private HashMap<String, Integer> hitagTryCount;

    private RxBleClient rxBleClient;
    private Subscription scanSubscription, connectionStateSubscription, notificationStateSubscribtion, comminicationSubscribtion;
    private ArrayList<Subscription> subscriptionsList;

    private Context context;
    private RxBleConnectionState connectionState;


    public BuyBuddyHitagReleaser(long orderId, Context context) {

        completedHitags = new HashSet<>();
        incompletedHitags = new HashSet<>();
        willOpenHitags = new HashSet<>();
        this.context = context;
        rxBleClient = RxBleClient.create(context);
        hitagTryCount = new HashMap<>();

        BuyBuddy.getInstance().api.getOrderDetail(orderId, new BuyBuddyApiCallback<OrderDelegateDetail>() {
            @Override
            public void success(BuyBuddyApiObject<OrderDelegateDetail> response) {
                Collections.addAll(willOpenHitags, response.getData().getHitagIds());
            }

            @Override
            public void error(BuyBuddyApiError error) {

            }
        });

        connectionTimeoutHandler = new Handler();
        processTimeoutHandler = new Handler();

        subscriptionsList = new ArrayList<>();
        subscriptionsList.add(connectionStateSubscription);
        subscriptionsList.add(notificationStateSubscribtion);
        subscriptionsList.add(comminicationSubscribtion);
    }

    private void startHitagReleasing() {
        scanSubscription = rxBleClient.scanBleDevices(
                new ScanSettings.Builder().setScanMode(SCAN_MODE_LOW_POWER).build(),
                new ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString(MAIN_PREFIX + MAIN_POSTFIX)).build()
        ).subscribe(new Action1<ScanResult>() {
            @Override
            public void call(ScanResult scanResult) {

                CollectedHitagTS hitag = CollectedHitagTS.getHitag(scanResult.getBleDevice(), scanResult.getScanRecord().getBytes(), scanResult.getRssi());

                if (hitag != null && hitag.getValidationCode() != -1) {
                    if (willOpenHitags.contains(hitag.getId())) {

                        scanSubscription.unsubscribe();

                        currentHitagId = hitag.getId();
                    }
                }

            }
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {
                if (throwable instanceof BleScanException) {
                    //handleBleScanException((BleScanException) throwable);
                }
            }
        });
    }

    private void connectDevice(final CollectedHitagTS hitag) {

        startCTHandler();
        connectionStateSubscription = hitag.getDevice().observeConnectionStateChanges()
            .subscribe(new Action1<RxBleConnection.RxBleConnectionState>() {
                @Override
                public void call(RxBleConnectionState rxBleConnectionState) {

                    switch (rxBleConnectionState) {
                        case CONNECTING:
                            connectionState = RxBleConnectionState.CONNECTING;
                            break;

                        case CONNECTED:
                            connectionState = RxBleConnectionState.CONNECTED;

                            Map<String, Integer> map = new HashMap<>();
                            map.put(hitag.getId(), hitag.getValidationCode());

                            BuyBuddy.getInstance().api.validateOrder(1, map, new BuyBuddyApiCallback<HitagPassword>() {
                                @Override
                                public void success(BuyBuddyApiObject<HitagPassword> response) {
                                    sendHitagPassword(hitag, response.getData().getHitagPass(hitag.getId()));
                                }

                                @Override
                                public void error(BuyBuddyApiError error) {

                                }
                            });

                            cancelCTHandler();
                            break;

                        case DISCONNECTED:
                            connectionState = RxBleConnectionState.DISCONNECTED;
                            break;

                        case DISCONNECTING:
                            connectionState = RxBleConnectionState.DISCONNECTING;
                            break;
                    }

                }
            });

        notificationStateSubscribtion = hitag.getDevice().establishConnection(false)
            .flatMap(new Func1<RxBleConnection, Observable<?>>() {
                @Override
                public Observable<?> call(RxBleConnection rxBleConnection) {
                    return rxBleConnection.setupNotification(UUID.fromString(HITAG_RX + MAIN_POSTFIX));
                }
            }).doOnNext(new Action1<Object>() {
                @Override
                public void call(Object o) {

                }
            }).flatMap(new Func1<Object, Observable<byte[]>>() {
                @Override
                public Observable<byte[]> call(Object o) {
                    return null;
                }
            })
            .subscribe(new Action1<byte[]>() {
                @Override
                public void call(byte[] o) {

                }
            }, new Action1<Throwable>() {
                @Override
                public void call(Throwable throwable) {

                }
            });
    }

    private void sendHitagPassword(CollectedHitagTS hitag, String password) {

        final byte[] passwordByte = password.getBytes();

        comminicationSubscribtion = hitag.getDevice().establishConnection(false)
            .flatMap(new Func1<RxBleConnection, Observable<byte[]>>() {
                @Override
                public Observable<byte[]> call(RxBleConnection rxBleConnection) {
                    return rxBleConnection.createNewLongWriteBuilder()
                            .setCharacteristicUuid(UUID.fromString(HITAG_TX + MAIN_POSTFIX))
                            .setBytes(passwordByte)
                            .build();
                }
            }).subscribe(new Action1<byte[]>() {
                @Override
                public void call(byte[] bytes) {

                }
            });
    }

    public class ReleaseInfo {
        Set<String> completedHitags;
        Set<String> incompletedHitags;

        ReleaseInfo(){
            completedHitags = new HashSet<>();
            incompletedHitags = new HashSet<>();
        }

        ReleaseInfo addCompleted(String hitagId) {
            this.completedHitags.add(hitagId);
            return this;
        }

        ReleaseInfo addIncompleted(String hitagId) {
            this.incompletedHitags.add(hitagId);
            return this;
        }

    }

    private void startCTHandler() {
        connectionTimeoutHandler.postDelayed(connectionTimeoutRunnable, CONNECTION_TIMEOUT);
    }

    private void startPTHandler() {
        processTimeoutHandler.postDelayed(processTimeoutRunnable, PROCESS_TIMEOUT);
    }

    private void cancelCTHandler() {
        connectionTimeoutHandler.removeCallbacks(connectionTimeoutRunnable);
    }

    private void cancelPTHandler() {
        processTimeoutHandler.removeCallbacks(processTimeoutRunnable);
    }

    private Handler connectionTimeoutHandler;
    private Runnable connectionTimeoutRunnable = new Runnable() {
        @Override
        public void run() {

        }
    };

    private Handler processTimeoutHandler;
    private Runnable processTimeoutRunnable = new Runnable() {
        @Override
        public void run() {

        }
    };


}
