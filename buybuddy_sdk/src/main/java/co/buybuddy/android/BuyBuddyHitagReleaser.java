package co.buybuddy.android;

import android.content.Context;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;

import com.polidea.rxandroidble.RxBleClient;
import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.exceptions.BleScanException;
import com.polidea.rxandroidble.scan.ScanFilter;
import com.polidea.rxandroidble.scan.ScanResult;
import com.polidea.rxandroidble.scan.ScanSettings;
import com.polidea.rxandroidble.utils.ConnectionSharingAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import co.buybuddy.android.interfaces.BuyBuddyApiCallback;
import co.buybuddy.android.responses.BuyBuddyApiError;
import co.buybuddy.android.responses.BuyBuddyApiObject;
import co.buybuddy.android.responses.OrderDelegateDetail;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.subjects.PublishSubject;

import static co.buybuddy.android.BuyBuddyBleUtils.HITAG_RX;
import static co.buybuddy.android.BuyBuddyBleUtils.HITAG_TX;
import static co.buybuddy.android.BuyBuddyBleUtils.MAIN_POSTFIX;
import static co.buybuddy.android.BuyBuddyBleUtils.MAIN_PREFIX;
import static com.polidea.rxandroidble.RxBleConnection.*;
import static com.polidea.rxandroidble.scan.ScanSettings.SCAN_MODE_BALANCED;
import static com.polidea.rxandroidble.scan.ScanSettings.SCAN_MODE_LOW_LATENCY;
import static com.polidea.rxandroidble.scan.ScanSettings.SCAN_MODE_OPPORTUNISTIC;

/**
 * Created by furkan on 6/14/17.
 * Gururla sunar. AHAHAHAHA Some spagetties
 */


public class BuyBuddyHitagReleaser {

    public enum Status {
        CONNECTED("CONNECTED"),
        CONNECTING("CONNECTING"),
        COMPLETED("COMPLETED"),
        NOT_FOUND("Hitag is missing"),
        VALIDATION_ERROR("Passwords are doesnt match"),
        CORRUPTED_PASSWORD("Password is not valid"),
        UNKNOWN_ERROR("ERROR");

        Status(String s) {
            rawValue = s;
        }

        public String getRaw() {
            return rawValue;
        }

        String rawValue;
    }

    private enum Response {
        SUCCESS(new byte[]{1, 1}),
        VALIDATION_SUCCESS(new byte[]{1, 2}),
        STARTING(new byte[]{1, 3}),
        ERROR(new byte[]{(byte) 255, (byte) 255}),
        UNKNOWN(new byte[]{-100, -100});


        byte[] rawValue;

        Response(byte[] b) {
            rawValue = b;
        }

        static Response getResponse(byte[] b) {
            if (Arrays.equals(SUCCESS.getRaw(), b)) {
                return SUCCESS;
            }else if (Arrays.equals(VALIDATION_SUCCESS.getRaw(), b)) {
                return VALIDATION_SUCCESS;
            }else if (Arrays.equals(STARTING.getRaw(), b)) {
                return STARTING;
            }else if (Arrays.equals(ERROR.getRaw(), b)) {
                return ERROR;
            }

            return UNKNOWN;
        }

        public byte[] getRaw() {
            return rawValue;
        }
    }

    public interface Delegate {
        void statusUpdate(String hitagId, Status hitagStatus);
        void error(BuyBuddyApiError error);
    }

    private Delegate delegate;
    private final static long CONNECTION_TIMEOUT = 5000;
    private final static long PROCESS_TIMEOUT = 5000;
    private final static long HITAG_RESPONSE_TIMEOUT = 2500;
    private final static long HITAG_SCAN_TIMEOUT = 10000;

    private HashSet<String> willOpenHitags;
    private HashSet<String> completedHitags;
    private HashSet<String> incompletedHitags;
    volatile private String currentHitagId;
    volatile boolean connecting = false;

    private HashMap<String, Integer> hitagTryCount;

    private RxBleClient rxBleClient;
    private Subscription scanSubscription, connectionStateSubscription, notificationStateSubscribtion, comminicationSubscription;
    private ArrayList<Subscription> subscriptionsList;
    private PublishSubject<Void> disconnectTriggerSubject = PublishSubject.create();

    private Context context;
    private RxBleConnectionState connectionState;
    private long orderId;

    private Observable<RxBleConnection> connectionObservable;


    public BuyBuddyHitagReleaser(long orderId, Context context) {

        completedHitags = new HashSet<>();
        incompletedHitags = new HashSet<>();
        willOpenHitags = new HashSet<>();
        this.context = context;
        rxBleClient = RxBleClient.create(context);
        hitagTryCount = new HashMap<>();
        this.orderId = orderId;

        BuyBuddy.getInstance().api.getOrderDetail(orderId, new BuyBuddyApiCallback<OrderDelegateDetail>() {
            @Override
            public void success(BuyBuddyApiObject<OrderDelegateDetail> response) {
                Collections.addAll(willOpenHitags, response.getData().getHitagIds());
                if (willOpenHitags.size() > 0) {
                    startHitagReleasing();
                }
            }

            @Override
            public void error(BuyBuddyApiError error) {
                if (delegate != null)
                    delegate.error(error);
            }
        });

        hitagResponseTimeoutHandler = new Handler();
        hitagScanTimeoutHandler  = new Handler();
        connectionTimeoutHandler = new Handler();
        processTimeoutHandler = new Handler();


        subscriptionsList = new ArrayList<>();
        subscriptionsList.add(connectionStateSubscription);
        subscriptionsList.add(notificationStateSubscribtion);
        subscriptionsList.add(comminicationSubscription);
    }

    public BuyBuddyHitagReleaser setDelegate(Delegate delegate) {
        this.delegate = delegate;
        return this;
    }

    private void startHitagReleasing() {
        //startScanHandler();

        scanSubscription = rxBleClient.scanBleDevices(
                new ScanSettings.Builder().setScanMode(SCAN_MODE_LOW_LATENCY).build(),
                new ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString(MAIN_PREFIX + MAIN_POSTFIX)).build()
               ).subscribeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<ScanResult>() {
            @Override
            public void call(ScanResult scanResult) {

                CollectedHitagTS hitag = CollectedHitagTS.getHitag(scanResult.getBleDevice(), scanResult.getScanRecord().getBytes(), scanResult.getRssi());

                if (hitag != null && hitag.getValidationCode() != -1) {
                    if (willOpenHitags.contains(hitag.getId())) {

                        scanSubscription.unsubscribe();
                        currentHitagId = hitag.getId();
                        if (!connecting)
                            connectDevice(hitag);
                    }
                }

            }
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {
                if (throwable instanceof BleScanException) {
                    BuyBuddyBleUtils.handBleScanExeption((BleScanException) throwable);
                }
                BuyBuddyUtil.printD("Scan", "ERROR");
            }
        });
    }

    private void connectDevice(final CollectedHitagTS hitag) {
        connecting = true;

        startCTHandler();
        cancelScanHandler();
        connectionStateSubscription = hitag.getDevice().observeConnectionStateChanges()
            .subscribe(new Action1<RxBleConnection.RxBleConnectionState>() {
                @Override
                public void call(RxBleConnectionState rxBleConnectionState) {

                    switch (rxBleConnectionState) {
                        case CONNECTING:
                            Log.d("**BuyBuddy** BLEState", "Connecting");
                            connectionState = RxBleConnectionState.CONNECTING;
                            if (delegate != null) {
                                delegate.statusUpdate(hitag.getId(), Status.CONNECTING);
                            }
                            break;

                        case CONNECTED:
                            Log.d("**BuyBuddy** BLEState", "Connected");

                            connectionState = RxBleConnectionState.CONNECTED;
                            if (delegate != null) {
                                delegate.statusUpdate(hitag.getId(), Status.CONNECTED);
                            }

                            Map<String, Integer> map = new HashMap<>();
                            map.put(hitag.getId(), hitag.getValidationCode());

                            startPTHandler();

                            BuyBuddy.getInstance().api.validateOrder(orderId, map, new BuyBuddyApiCallback<HitagPassword>() {
                                @Override
                                public void success(BuyBuddyApiObject<HitagPassword> response) {
                                    sendHitagPassword(hitag, response.getData().getHitagPass(hitag.getId()));
                                    BuyBuddyUtil.printD("HitagPass", "Sending");
                                }

                                @Override
                                public void error(BuyBuddyApiError error) {
                                    BuyBuddyUtil.printD("HitagPass", "ERROR");

                                    if (willOpenHitags.contains(hitag.getId())) {

                                        if (delegate != null)
                                            delegate.statusUpdate(hitag.getId(), Status.VALIDATION_ERROR);

                                        nextDevice(true);

                                    }
                                }
                            });

                            cancelCTHandler();
                            break;

                        case DISCONNECTED:
                            connecting = false;
                            connectionState = RxBleConnectionState.DISCONNECTED;
                            BuyBuddyUtil.printD("BLEState", "Disconnected");
                            break;

                        case DISCONNECTING:
                            connecting = false;
                            connectionState = RxBleConnectionState.DISCONNECTING;
                            Log.d("**BuyBuddy** BLEState", "Disconnecting");
                            break;
                    }

                }
            });

        connectionObservable = hitag.getDevice()
                                    .establishConnection(false)
                                    .takeUntil(disconnectTriggerSubject)
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .compose(new ConnectionSharingAdapter());

        notificationStateSubscribtion = connectionObservable
            .flatMap(new Func1<RxBleConnection, Observable<?>>() {
                @Override
                public Observable<?> call(RxBleConnection rxBleConnection) {
                    return rxBleConnection.setupNotification(UUID.fromString(HITAG_RX + MAIN_POSTFIX));
                }
            }).flatMap(new Func1<Object, Observable<byte[]>>() {
                @Override
                public Observable<byte[]> call(Object o) {
                        return (Observable<byte[]>) o;
                }
            }).subscribe(new Action1<byte[]>() {
                @Override
                public void call(byte[] bytes) {

                    Response hitagResponse = Response.getResponse(bytes);

                    if (hitagResponse == Response.SUCCESS) {
                        if (delegate != null)
                            delegate.statusUpdate(hitag.getId(), Status.COMPLETED);

                        BuyBuddyUtil.printD("HitagResponse", hitag.getId() + " HITAG PASS COMPLETED");

                        nextDevice(false);
                    } else if (hitagResponse == Response.ERROR) {
                        if (delegate != null)
                            delegate.statusUpdate(hitag.getId(), Status.VALIDATION_ERROR);

                        Log.d("**BuyBuddy** ERROR :", "HITAG PASS VALIDATION_ERROR");
                        nextDevice(false);
                    }
                }
            }, new Action1<Throwable>() {
                @Override
                public void call(Throwable throwable) {
                    BuyBuddyUtil.printD("HitagResponse", "ERROR INCOME");
                }
            });
    }

    private void triggerDisconnect() {
        disconnectTriggerSubject.onNext(null);
    }

    private void sendHitagPassword(final CollectedHitagTS hitag, String password) {

        final byte[] passwordByte = BuyBuddyBleUtils.parseHexBinary(password);

        if (passwordByte == null) {
            if (delegate != null)
                delegate.statusUpdate(hitag.getId(), Status.CORRUPTED_PASSWORD);

            return;
        }

        cancelCTHandler();
        startResponseHandler();

        comminicationSubscription = connectionObservable
            .flatMap(new Func1<RxBleConnection, Observable<byte[]>>() {
                @Override
                public Observable<byte[]> call(RxBleConnection rxBleConnection) {
                    return rxBleConnection.writeCharacteristic(UUID.fromString(HITAG_TX + MAIN_POSTFIX), passwordByte);
                }
            }).subscribe(new Action1<byte[]>() {
                @Override
                public void call(byte[] bytes) {
                    BuyBuddyUtil.printD("HitagPass", "wrote bytes" + bytes.toString());
                }
            }, new Action1<Throwable>() {
                @Override
                public void call(Throwable throwable) {
                    BuyBuddyUtil.printD("HitagPass", "ERROR INCOME");
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

    private void doUnSubscribe() {
        for (Subscription subscription : subscriptionsList) {
            if (subscription != null) {
                if (!subscription.isUnsubscribed())
                    subscription.unsubscribe();
            }
        }
    }

    private void nextDevice(boolean withError) {
        String hitagId = currentHitagId;


        if (withError) {
            incompletedHitags.add(hitagId);
            willOpenHitags.remove(hitagId);
        }else {
            completedHitags.add(hitagId);
            willOpenHitags.remove(hitagId);
        }

        cancelPTHandler();
        cancelCTHandler();
        cancelScanHandler();
        cancelResponseHandler();

        doUnSubscribe();
        triggerDisconnect();

        connecting = false;


        if (willOpenHitags.size() > 0) {
            startHitagReleasing();
        }else{
            Log.d("nextDevice **BuyBuddy**", "DEVICE LIST IS EMPTY");
        }
    }

    private void startResponseHandler() {
        hitagResponseTimeoutHandler.postDelayed(hitagResponseTimeoutRunnable, HITAG_RESPONSE_TIMEOUT);
    }

    private void startCTHandler() {
        connectionTimeoutHandler.postDelayed(connectionTimeoutRunnable, CONNECTION_TIMEOUT);
    }

    private void startScanHandler() {
        hitagScanTimeoutHandler.postDelayed(hitagScanTimeoutRunnable, HITAG_SCAN_TIMEOUT);
    }

    private void startPTHandler() {
        processTimeoutHandler.postDelayed(processTimeoutRunnable, PROCESS_TIMEOUT);
    }

    private void cancelResponseHandler() {
        hitagResponseTimeoutHandler.removeCallbacks(hitagResponseTimeoutRunnable);
    }

    private void cancelCTHandler() {
        connectionTimeoutHandler.removeCallbacks(connectionTimeoutRunnable);
    }

    private void cancelScanHandler() {
        hitagScanTimeoutHandler.removeCallbacks(hitagScanTimeoutRunnable);
    }

    private void cancelPTHandler() {
        processTimeoutHandler.removeCallbacks(processTimeoutRunnable);
    }

    private Handler hitagScanTimeoutHandler;
    private Runnable hitagScanTimeoutRunnable = new Runnable() {
        @Override
        public void run() {
            Log.d("**BuyBuddy** Timeout :", "SCAN TIMEOUT");
            nextDevice(true);
        }
    };

    private Handler hitagResponseTimeoutHandler;
    private Runnable hitagResponseTimeoutRunnable = new Runnable() {
        @Override
        public void run() {
            Log.d("**BuyBuddy** Timeout :", "HITAG RESPONSE");
            nextDevice(true);
        }
    };

    private Handler connectionTimeoutHandler;
    private Runnable connectionTimeoutRunnable = new Runnable() {
        @Override
        public void run() {
            Log.d("**BuyBuddy** Timeout :", "CONNECTION");
            nextDevice(true);
        }
    };

    private Handler processTimeoutHandler;
    private Runnable processTimeoutRunnable = new Runnable() {
        @Override
        public void run() {
            nextDevice(true);
        }
    };


}
