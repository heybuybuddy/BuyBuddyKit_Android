package co.buybuddy.sdk;

import co.buybuddy.sdk.BuyBuddy;
import co.buybuddy.sdk.BuyBuddyUtil;
import co.buybuddy.sdk.interfaces.BuyBuddyApiCallback;
import co.buybuddy.sdk.location.BuyBuddyStore;
import co.buybuddy.sdk.location.BuyBuddyStoreInfoDelegate;
import co.buybuddy.sdk.responses.BuyBuddyApiError;
import co.buybuddy.sdk.responses.BuyBuddyApiObject;

public class BuyBuddyStoreInfoProvider {

    private static String BBSP_LAST_KNOWN_LOCATION_TIME_KEY = "buybuddy_sharedpref_and_last_kltk";
    private static String BBSP_LAST_KNOWN_LOCATION_INFO_KEY = "buybuddy_sharedpref_and_last_knik";

    /* Reliability duration of current location information.  */
    private static long LOCATION_PERIOD_OF_VALIDITY = 300000L;

    private BuyBuddyStore location = null;
    BuyBuddyStoreInfoDelegate delegate;

    public void setDelegate(BuyBuddyStoreInfoDelegate delegate) {
        this.delegate = delegate;
    }

    void getLocation(String hitagId) {

        if (BuyBuddy.getContext() != null) {

            long lastLocationTime = BuyBuddyUtil.getSP().getLong(BBSP_LAST_KNOWN_LOCATION_TIME_KEY, 0);

            if (System.currentTimeMillis() -  lastLocationTime > LOCATION_PERIOD_OF_VALIDITY) {

                BuyBuddyUtil.getSP().edit().putLong(BBSP_LAST_KNOWN_LOCATION_TIME_KEY, System.currentTimeMillis()).apply();

                BuyBuddy.getInstance().api.getStoreInfo(hitagId, new BuyBuddyApiCallback<BuyBuddyStore>() {
                    @Override
                    public void success(BuyBuddyApiObject<BuyBuddyStore> response) {

                        if (location != null && location.getId() != response.getData().getId()) {

                            if (delegate != null)
                                delegate.enterRegion(response.getData());

                        } else if (location != null && location.getId() == response.getData().getId()) {

                            if (delegate != null)
                                delegate.activeRegion(response.getData());

                        } else if (location == null) {

                            if (delegate != null)
                                delegate.enterRegion(response.getData());

                        }

                        location = response.getData();
                    }

                    @Override
                    public void error(BuyBuddyApiError error) {

                    }
                });

            } else {

                if (location != null) {

                    if (delegate != null)
                        delegate.activeRegion(location);

                }
            }
        }
    }
}
