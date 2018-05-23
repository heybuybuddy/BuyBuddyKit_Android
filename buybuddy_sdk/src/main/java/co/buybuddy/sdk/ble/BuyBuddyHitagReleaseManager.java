package co.buybuddy.sdk.ble;

import android.content.Intent;


import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import co.buybuddy.sdk.BuyBuddy;
import co.buybuddy.sdk.BuyBuddyUtil;
import co.buybuddy.sdk.ble.blecompat.BluetoothLeCompatException;
import co.buybuddy.sdk.ble.exception.HitagReleaserException;

public final class BuyBuddyHitagReleaseManager {

    private BuyBuddyHitagReleaserDelegate delegate;
    private static Intent serviceIntent = new Intent(BuyBuddy.getContext(), BuyBuddyHitagReleaser.class);

    private boolean didFinish = false;

    public BuyBuddyHitagReleaseManager() {
        EventBus.getDefault().register(this);

        BuyBuddyUtil.printD("BuyBuddyHitagReleaseManager", "starting");
    }

    public BuyBuddyHitagReleaseManager startReleasing(long orderId) {

        BuyBuddy.getContext().stopService(serviceIntent);

        serviceIntent.removeExtra("is_retry");
        serviceIntent.putExtra("orderId", orderId);
        didFinish = false;

        BuyBuddy.getContext().startService(serviceIntent);

        return this;
    }

    public BuyBuddyHitagReleaseManager retryReleasing() {

        BuyBuddy.getContext().stopService(serviceIntent);

        serviceIntent.removeExtra("orderId");
        serviceIntent.putExtra("is_retry", true);
        didFinish = false;

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

    @Subscribe(threadMode = ThreadMode.ASYNC)
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
                    if (!didFinish) {
                        didFinish = true;
                        delegate.didFinish();
                    }

                    BuyBuddy.getContext().stopService(serviceIntent);
                    break;

                case 3:
                    delegate.onHitagReleased(hitagEvent.hitagId);
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onBleScanException(BluetoothLeCompatException exception) {
        if (delegate != null)
            delegate.onExceptionThrown(exception);
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onSaleException(HitagReleaserException exception) {
        if (delegate != null)
            delegate.onExceptionThrown(exception);
    }
}
