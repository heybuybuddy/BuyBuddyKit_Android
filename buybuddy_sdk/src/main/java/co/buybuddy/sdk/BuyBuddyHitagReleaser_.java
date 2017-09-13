package co.buybuddy.sdk;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.ParcelUuid;

import com.polidea.rxandroidble.RxBleClient;
import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.exceptions.BleScanException;
import com.polidea.rxandroidble.scan.ScanFilter;
import com.polidea.rxandroidble.scan.ScanResult;
import com.polidea.rxandroidble.scan.ScanSettings;
import com.polidea.rxandroidble.utils.ConnectionSharingAdapter;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TimerTask;
import java.util.UUID;

import co.buybuddy.sdk.ble.BuyBuddyBleUtils;
import co.buybuddy.sdk.ble.CollectedHitagTS;
import co.buybuddy.sdk.interfaces.BuyBuddyApiCallback;
import co.buybuddy.sdk.responses.BuyBuddyApiError;
import co.buybuddy.sdk.responses.BuyBuddyApiObject;
import co.buybuddy.sdk.responses.BuyBuddyBase;
import co.buybuddy.sdk.responses.OrderDelegateDetail;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.subjects.PublishSubject;

import static com.polidea.rxandroidble.RxBleConnection.*;
import static com.polidea.rxandroidble.scan.ScanSettings.SCAN_MODE_LOW_LATENCY;

/**
 * Created by Furkan Ençkü on 6/14/17.
 * This code written by buybuddy Android Team
 */


public class BuyBuddyHitagReleaser_ {

    private static final String TAG = "HitagReleaser";

    public static void startReleasing(long orderId, Delegate delegate) {

        //Intent extras = new Intent(BuyBuddy.getContext(), BuyBuddyHitagReleaser.class);
        //extras.putExtra("orderId", orderId);

        //BuyBuddy.getContext().startService(extras);
    }


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

    private static BuyBuddyHitagReleaser_ mInstance;

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
        void completed(String hitagID);
        void error(HitagReleaserError error);
    }

    private Delegate delegate;
    private final static long CONNECTION_TIMEOUT = 10000;
    private final static long PROCESS_TIMEOUT = 10000;
    private final static long HITAG_RESPONSE_TIMEOUT = 2500;
    private final static long HITAG_SCAN_TIMEOUT = 10000;

    private volatile HashSet<String> willOpenHitags;
    private HashSet<String> completedHitags;
    private HashSet<String> incompletedHitags;
    private volatile String currentHitagId;
    private volatile boolean connecting = false;

    private HashMap<String, Integer> hitagTryCount;

    private RxBleClient rxBleClient;
    private Subscription scanSubscription, connectionStateSubscription, notificationStateSubscribtion, comminicationSubscription;
    private PublishSubject<Void> disconnectTriggerSubject = PublishSubject.create();
    private PublishSubject<Void> scanUnsubscriber = PublishSubject.create();
    private PublishSubject<Void> cancelListener = PublishSubject.create();

    private Context context;
    private RxBleConnectionState connectionState;
    private long orderId;

    private Observable<RxBleConnection> connectionObservable;

    public static void startReleasing(final long order_id, final Context ctx, Delegate delegate) {
        if (mInstance != null) {
            mInstance.doUnSubscribe();
            mInstance.cancelProcesses();
            mInstance.cancelListeningCharacteristic();
        }
        mInstance = new BuyBuddyHitagReleaser_();
        mInstance.delegate = delegate;

        mInstance.completedHitags = new HashSet<>();
        mInstance.incompletedHitags = new HashSet<>();
        mInstance.willOpenHitags = new HashSet<>();
        mInstance.context = ctx;
        mInstance.rxBleClient = BuyBuddy.getInstance().client;
        mInstance.hitagTryCount = new HashMap<>();
        mInstance.orderId = order_id;

        BuyBuddyUtil.printD(TAG, "STARTING");
        BuyBuddy.getInstance().api.getOrderDetail(mInstance.orderId, new BuyBuddyApiCallback<OrderDelegateDetail>() {
            @Override
            public void success(BuyBuddyApiObject<OrderDelegateDetail> response) {
                Collections.addAll(mInstance.willOpenHitags, response.getData().getHitagIds());
                if (mInstance.willOpenHitags.size() > 0) {
                    BuyBuddyUtil.printD(TAG, "HITAG State");
                    mInstance.startHitagReleasing(response.getData().getHitagIds()[0]);
                }else{
                    mInstance.delegate.error(new HitagReleaserError("Empty Hitag List", 103));
                }
            }

            @Override
            public void error(BuyBuddyApiError error) {
                if (mInstance.delegate != null) {

                    if (error.getResponseCode() == 404) {
                        mInstance.delegate.error(new HitagReleaserError("Order is not found", 101));
                    }else if (error.getResponseCode() == 422) {
                        mInstance.delegate.error(new HitagReleaserError("Parameter Error", 102));
                    }else {
                        mInstance.delegate.error(new HitagReleaserError("Unknown Error", 900));
                    }
                }
            }
        });

        mInstance.hitagResponseTimeoutHandler = new Handler();
        mInstance.hitagScanTimeoutHandler  = new Handler();
        mInstance.connectionTimeoutHandler = new Handler();
        mInstance.processTimeoutHandler = new Handler();
    }


    public BuyBuddyHitagReleaser_ setDelegate(Delegate delegate) {
        this.delegate = delegate;
        return this;
    }

    private void startHitagReleasing(String hitagId) {
        if (scanSubscription != null)
            if (scanSubscription.isUnsubscribed())
                scanSubscription.unsubscribe();

        if (connectionObservable != null)
            triggerDisconnect();

        currentHitagId = hitagId;

        startScanHandler();
        BuyBuddyUtil.printD(TAG, "Scan Starting Device Count : " + willOpenHitags.size());

        scanSubscription = rxBleClient.scanBleDevices(
                new ScanSettings.Builder().setScanMode(SCAN_MODE_LOW_LATENCY).build(),
                new ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString(BuyBuddyBleUtils.MAIN_PREFIX + BuyBuddyBleUtils.MAIN_POSTFIX)).build()
               ).takeUntil(scanUnsubscriber)
                //.subscribeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<ScanResult>() {
            @Override
            public void call(ScanResult scanResult) {

                CollectedHitagTS hitag = CollectedHitagTS.getHitag(scanResult.getBleDevice(), scanResult.getScanRecord().getBytes(), scanResult.getRssi());

                if (hitag != null) {
                    BuyBuddyUtil.printD(TAG, hitag.getId());
                }

                if (hitag != null && hitag.getValidationCode() != -1) {
                    if (willOpenHitags.contains(currentHitagId)) {

                        if (!connecting){
                            connectDevice(hitag);
                            unSubscribeScan();
                        }
                    }
                }
            }
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {
                if (throwable instanceof BleScanException) {
                    BuyBuddyBleUtils.handBleScanExeption((BleScanException) throwable);
                    if(delegate != null){
                        BleScanException error = (BleScanException) throwable;
                        delegate.error(new HitagReleaserError(error.toString(), error.getReason()));
                    }
                }
                BuyBuddyUtil.printD(TAG, "BleScanExceptionStart");
                throwable.printStackTrace();
                BuyBuddyUtil.printD(TAG, "BleScanExceptionFinish");
            }
        });
    }

    private void connectDevice(final CollectedHitagTS hitag) {
        connecting = true;

        startCTHandler();
        cancelScanHandler();
        connectionStateSubscription = hitag.getDevice().observeConnectionStateChanges()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<RxBleConnection.RxBleConnectionState>() {
                @Override
                public void call(RxBleConnectionState rxBleConnectionState) {

                    switch (rxBleConnectionState) {
                        case CONNECTING:
                            BuyBuddyUtil.printD(TAG, hitag.getId() + " Conneting");
                            connectionState = RxBleConnectionState.CONNECTING;
                            if (delegate != null) {
                                delegate.statusUpdate(hitag.getId(), Status.CONNECTING);
                            }
                            break;

                        case CONNECTED:
                            BuyBuddyUtil.printD(TAG, hitag.getId() + " Connected");

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
                                    BuyBuddyUtil.printD(TAG, hitag.getId() + " Password Sending");
                                }

                                @Override
                                public void error(BuyBuddyApiError error) {
                                    BuyBuddyUtil.printD(TAG, hitag.getId() + " PasswordApi Error: " + error.getResponseCode() );

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
                            BuyBuddyUtil.printD(TAG, hitag.getId() + " Disconnected");
                            break;

                        case DISCONNECTING:
                            connecting = false;
                            connectionState = RxBleConnectionState.DISCONNECTING;
                            BuyBuddyUtil.printD(TAG, hitag.getId() + " Disconnecting");
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
                    return rxBleConnection.setupNotification(UUID.fromString(BuyBuddyBleUtils.HITAG_RX + BuyBuddyBleUtils.MAIN_POSTFIX));
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

                        BuyBuddyUtil.printD(TAG, hitag.getId() + " Hitag Opening Operation Successfully");
                        cancelResponseHandler();
                        connectionStateSubscription.unsubscribe();
                        connectionObservable.doOnNext(null);
                        nextDevice(false);
                        hitagReleasingCompleted(hitag.getId());


                    } else if (hitagResponse == Response.ERROR) {
                        if (delegate != null)
                            delegate.statusUpdate(hitag.getId(), Status.VALIDATION_ERROR);

                        BuyBuddyUtil.printD(TAG, hitag.getId() + " Hitag Opening Operation Incompleted : " + Response.ERROR);
                        connectionStateSubscription.unsubscribe();
                        notificationStateSubscribtion.unsubscribe();
                        connectionObservable.doOnNext(null);
                        cancelResponseHandler();
                        nextDevice(true, true);
                    }else if (hitagResponse == Response.STARTING){
                        BuyBuddyUtil.printD(TAG, hitag.getId() + " Hitag Password Process Starting");
                    }
                }
            }, new Action1<Throwable>() {
                @Override
                public void call(Throwable throwable) {
                    BuyBuddyUtil.printD(TAG, hitag.getId() + " Notification Observable Error");
                }
            });
    }

    private void hitagReleasingCompleted(final String hitagId) {

        BuyBuddy.getInstance().api.completeOrder(orderId, hitagId, 1, new BuyBuddyApiCallback<BuyBuddyBase>() {
            @Override
            public void success(BuyBuddyApiObject<BuyBuddyBase> response) {
                BuyBuddyUtil.printD(TAG, hitagId + " Send Completed Server Success");
                delegate.completed(hitagId);
            }

            @Override
            public void error(BuyBuddyApiError error) {
                BuyBuddyUtil.printD(TAG, hitagId + " Send Completed Server Failed");
            }
        });
    }

    private void triggerDisconnect() {
        disconnectTriggerSubject.onNext(null);
    }

    private void unSubscribeScan() {
        scanUnsubscriber.onNext(null);
    }

    private void cancelListeningCharacteristic() {
        cancelListener.onNext(null);
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
                    return rxBleConnection.writeCharacteristic(UUID.fromString(BuyBuddyBleUtils.HITAG_TX + BuyBuddyBleUtils.MAIN_POSTFIX), passwordByte);
                }
            }).subscribe(new Action1<byte[]>() {
                @Override
                public void call(byte[] bytes) {
                    BuyBuddyUtil.printD(TAG, "Write Operation Success");
                }
            }, new Action1<Throwable>() {
                @Override
                public void call(Throwable throwable) {
                    BuyBuddyUtil.printD(TAG, "Writing Error");
                }
            });
    }

    private void doUnSubscribe() {

        if (scanSubscription != null)
            scanSubscription.unsubscribe();

        if (connectionObservable != null)
            connectionObservable.doOnNext(null);

        if (connectionStateSubscription != null)
            connectionStateSubscription.unsubscribe();

        if (notificationStateSubscribtion != null)
            notificationStateSubscribtion.unsubscribe();

        if (comminicationSubscription != null)
            comminicationSubscription.unsubscribe();

        triggerDisconnect();
    }

    private void nextDevice(boolean withError, boolean forceStop) {

        cancelPTHandler();
        cancelCTHandler();
        cancelScanHandler();
        cancelResponseHandler();
        cancelListeningCharacteristic();
        doUnSubscribe();

        connecting = false;

        String hitagId = currentHitagId;

        int tryCount = hitagTryCount.get(hitagId) == null ? 0 : hitagTryCount.get(hitagId);
        hitagTryCount.put(hitagId, ++tryCount);

        BuyBuddyUtil.printD(TAG, hitagId + " TryCount: " + tryCount + " Hitag Try Count");


        // İşlemi sonlandır komutu gelirse direk tamamlanamayan cihazlar listesine ekleniyor.
        if (forceStop) {
            willOpenHitags.remove(hitagId);
            incompletedHitags.add(hitagId);
        }else { // İşlemi direk sonlandır yoksa hataya bakılır.
            if (withError) { // <- HATA kontrolü
                if (tryCount < 3) { // Tekrar sayısı 3 den küçükse akış devam eder.
                    if (willOpenHitags.size() > 1) { // Açılacak cihaz 1 den fazla ise o an denenen cihaz yerine başka cihaz aranır.
                        for (String hId : willOpenHitags) {
                            if (!hId.equals(hitagId)) {
                                startHitagReleasing(hitagId);
                                return; // <- DENENECEK CİHAZ akışı tekrar başlar.
                            }
                        }
                    }else{ // Sadece bu cihaz kaldıysa tekrar denenir.
                        startHitagReleasing(hitagId);
                        return; // <- DENENECEK CİHAZ akışı tekrar başlar.
                    }
                }else { // 3 kere denendiyse tamamlanamayan cihaz olarak listeye eklenir.
                    willOpenHitags.remove(hitagId);
                    incompletedHitags.add(hitagId);
                }
            }else { // Hata yoksa tamamlanan cihaz listesine eklenir.
                willOpenHitags.remove(hitagId);
                completedHitags.add(hitagId);
            }
        }


        if (willOpenHitags.size() > 0) { // Açılacak cihaz kaldı mı diye kontrol edilir.
            if (willOpenHitags.size() > 1) { // 1 den fazla cihaz varsa o an denenen cihaz yerine başka cihaz aranır.
                for (String hId : willOpenHitags) {
                    if (!hId.equals(hitagId)) {
                        startHitagReleasing(hitagId); // Başka cihaz bulununca onunla başlanır.
                        break; // <- DENENECEK CİHAZ akışı tekrar başlar.
                    }
                }
            }else { // Tek cihaz kaldıysa tekrar o denenir.
                startHitagReleasing((String) willOpenHitags.toArray()[0]);
            }
        }else{
            BuyBuddyUtil.printD(TAG, "DONE: Device list is empty !!");
            cancelProcesses();
        }

    }

    private void nextDevice(boolean withError) {
        nextDevice(withError, false);
    }

    private void cancelProcesses() {
        cancelPTHandler();
        cancelCTHandler();
        cancelScanHandler();
        cancelResponseHandler();

        cancelListeningCharacteristic();
        unSubscribeScan();
        doUnSubscribe();
        triggerDisconnect();

        connecting = false;
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
    private TimerTask hitagScanTimeoutRunnable = new TimerTask() {
        @Override
        public void run() {
            BuyBuddyUtil.printD(TAG, currentHitagId + " Timeout: Hitag Scan");
            nextDevice(true);
        }
    };


    private Handler hitagResponseTimeoutHandler;
    private TimerTask hitagResponseTimeoutRunnable = new TimerTask() {
        @Override
        public void run() {
            BuyBuddyUtil.printD(TAG, currentHitagId + " Timeout: Hitag State");
            nextDevice(true);
        }
    };

    private Handler connectionTimeoutHandler;
    private TimerTask connectionTimeoutRunnable = new TimerTask() {
        @Override
        public void run() {
            BuyBuddyUtil.printD(TAG, currentHitagId + " Timeout: Hitag Connection");
            nextDevice(true);
        }
    };

    private Handler processTimeoutHandler;
    private TimerTask processTimeoutRunnable = new TimerTask() {
        @Override
        public void run() {
            nextDevice(true);
        }
    };

    public static class HitagReleaserError {
        private String detail;
        private int code;

        public String getDetail() {
            return detail;
        }

        public int getCode() {
            return code;
        }

        public HitagReleaserError(String detail, int code) {
            this.detail = detail;
            this.code = code;
        }
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


}
