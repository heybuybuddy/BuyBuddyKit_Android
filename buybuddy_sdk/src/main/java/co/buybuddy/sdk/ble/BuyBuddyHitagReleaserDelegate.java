package co.buybuddy.sdk.ble;

import co.buybuddy.sdk.ble.exception.BleScanException;

public abstract class BuyBuddyHitagReleaserDelegate implements IBuyBuddyHitagReleaser {

    /**
     *
     * @param exception look at the {@link BleScanException}
     */

    @Override
    public void onExceptionThrown(BleScanException exception) {

    }

    /**
     *
     * @param hitagId Identifier of event owner hitag.
     * @param event { PASSWORD_OLD,
     *                PASSWORD_WRONG,
     *                STATE_BUGGY,
     *                CONNECTION_FAILED,
     *                NOT_FOUND }
     */

    @Override
    public void onHitagFailed(String hitagId, Hitag.State event) {

    }

    /**
     *
     * @param hitagId Identifier of event owner hitag.
     * @param event { CONNECTED,
     *                DISCONNECTED,
     *                STATE_UNLOCKING }
     */

    @Override
    public void onHitagEvent(String hitagId, Hitag.State event) {

    }

    /**
     *
     * @param hitagId Identifier of released hitag.
     */

    @Override
    public void onHitagReleased(String hitagId) {

    }

    /**
     *
     * All hitag operations completed with failed and successfully devices lists;
     */

    @Override
    public void didFinish() {

    }
}
