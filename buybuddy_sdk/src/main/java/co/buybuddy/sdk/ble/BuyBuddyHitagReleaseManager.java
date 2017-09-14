package co.buybuddy.sdk.ble;

import android.content.Intent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import co.buybuddy.sdk.BuyBuddy;
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
    void onExceptionThrown(HitagReleaserException exception);
    void didFinish();
}

public final class BuyBuddyHitagReleaseManager {

    private BuyBuddyHitagReleaserDelegate delegate;
    private Intent serviceIntent;

    public BuyBuddyHitagReleaseManager() {
        EventBus.getDefault().register(this);
    }

    public BuyBuddyHitagReleaseManager startReleasing(long orderId) {

        serviceIntent = new Intent(BuyBuddy.getContext(), BuyBuddyHitagReleaser.class);
        serviceIntent.putExtra("orderId", orderId);

        BuyBuddy.getContext().startService(serviceIntent);

        return this;
    }

    public BuyBuddyHitagReleaseManager retryReleasing() {

        serviceIntent = new Intent(BuyBuddy.getContext(), BuyBuddyHitagReleaser.class);
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
    void onHitagEventFromService(HitagEventFromService hitagEvent) {
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
    void onBleScanException(HitagReleaserException exception) {
        if (delegate != null)
            delegate.onExceptionThrown(exception);
    }
}
