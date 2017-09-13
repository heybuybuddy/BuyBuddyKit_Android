package co.buybuddy.sdk.ble;

import android.content.Intent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import co.buybuddy.sdk.BuyBuddy;
import co.buybuddy.sdk.ble.exception.BleScanException;

/**
 * Created by Furkan Ençkü on 9/12/17.
 * This code written by buybuddy Android Team
 */

interface IBuyBuddyHitagReleaser {
    void onHitagFailed(String hitagId, Hitag.State event);
    void onHitagEvent(String hitagId, Hitag.State event);
    void onHitagReleased(String hitagId);
    void onExceptionThrown(BleScanException exception);
    void didFinish();
}

public class BuyBuddyHitagReleaseManager {

    private BuyBuddyHitagReleaserDelegate delegate;

    public BuyBuddyHitagReleaseManager() {
        EventBus.getDefault().register(this);
    }

    public BuyBuddyHitagReleaseManager startReleasing(long orderId) {

        Intent extras = new Intent(BuyBuddy.getContext(), BuyBuddyHitagReleaser.class);
        extras.putExtra("orderId", orderId);

        BuyBuddy.getContext().startService(extras);

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
                    break;

                case 3:
                    delegate.onHitagReleased(hitagEvent.hitagId);
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    void onBleScanException(BleScanException exception) {
        if (delegate != null)
            delegate.onExceptionThrown(exception);
    }
}
