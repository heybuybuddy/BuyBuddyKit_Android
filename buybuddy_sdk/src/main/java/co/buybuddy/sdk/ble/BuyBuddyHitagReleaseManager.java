package co.buybuddy.sdk.ble;

import android.content.Intent;

import com.forkingcode.bluetoothcompat.BluetoothLeCompatException;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import co.buybuddy.sdk.BuyBuddy;
import co.buybuddy.sdk.BuyBuddyUtil;
import co.buybuddy.sdk.ble.exception.HitagReleaserBleException;
import co.buybuddy.sdk.ble.exception.HitagReleaserException;

/**
 * Created by Furkan Ençkü on 9/12/17.
 * This code written by buybuddy Android Team
 */

interface IBuyBuddyHitagReleaser {
    void onHitagFailed(String hitagId, HitagState event);
    void onHitagEvent(String hitagId, HitagState event);
    void onHitagReleased(String hitagId);
    void onExceptionThrown(BluetoothLeCompatException exception);
    void didFinish();
}

public final class BuyBuddyHitagReleaseManager {

    private BuyBuddyHitagReleaserDelegate delegate;
    private static Intent serviceIntent = new Intent(BuyBuddy.getContext(), BuyBuddyHitagReleaser.class);

    public BuyBuddyHitagReleaseManager() {
        EventBus.getDefault().register(this);

        BuyBuddyUtil.printD("BuyBuddyHitagReleaseManager", "starting");
    }

    public BuyBuddyHitagReleaseManager startReleasing(long orderId) {

        BuyBuddy.getContext().stopService(serviceIntent);

        serviceIntent.removeExtra("is_retry");
        serviceIntent.putExtra("orderId", orderId);

        BuyBuddy.getContext().startService(serviceIntent);

        return this;
    }

    public BuyBuddyHitagReleaseManager retryReleasing() {

        BuyBuddy.getContext().stopService(serviceIntent);

        serviceIntent.removeExtra("orderId");
        serviceIntent.putExtra("is_retry", true);

        BuyBuddy.getContext().startService(serviceIntent);

        return this;
    }

    public BuyBuddyHitagReleaseManager subscribeForHitagEvents(BuyBuddyHitagReleaserDelegate delegate) {
        this.delegate = delegate;
        return this;
    }

    public BuyBuddyHitagReleaseManager unSubscribeForHitagEvents() {
        this.delegate = null;
        EventBus.getDefault().unregister(this);
        return this;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onHitagEventFromService(HitagEventFromService hitagEvent) {
        if (delegate != null) {

            switch (hitagEvent.eventType) {
                case 0:
                    delegate.onHitagEvent(hitagEvent.hitagId, hitagEvent.event);
                    break;

                case 1:
                    delegate.onHitagFailed(hitagEvent.hitagId, hitagEvent.event);
                    break;

                case 2:
                    delegate.didFinish();
                    BuyBuddy.getContext().stopService(serviceIntent);
                    break;

                case 3:
                    delegate.onHitagReleased(hitagEvent.hitagId);
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onBleScanException(BluetoothLeCompatException exception) {
        if (delegate != null)
            delegate.onExceptionThrown(exception);
    }
}
