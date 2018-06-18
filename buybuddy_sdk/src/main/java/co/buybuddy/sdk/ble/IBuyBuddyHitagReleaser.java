package co.buybuddy.sdk.ble;

/**
 * Created by Furkan Ençkü on 9/12/17.
 * This code written by buybuddy Android Team
 */

interface IBuyBuddyHitagReleaser {
    void onHitagFailed(String hitagId, HitagState event);
    void onHitagEvent(String hitagId, HitagState event);
    void onHitagReleased(String hitagId);
    void onExceptionThrown(Exception exception);
    void didFinish();
}
