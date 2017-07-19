package co.buybuddy.sampledelegateapp;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import co.buybuddy.sdk.BuyBuddy;
import co.buybuddy.sdk.BuyBuddyHitagReleaser;
import co.buybuddy.sdk.BuyBuddyUtil;
import co.buybuddy.sdk.HitagScanService;
import co.buybuddy.sdk.interfaces.BuyBuddyApiCallback;
import co.buybuddy.sdk.interfaces.BuyBuddyUserTokenExpiredDelegate;
import co.buybuddy.sdk.responses.BuyBuddyApiError;
import co.buybuddy.sdk.responses.BuyBuddyApiObject;
import co.buybuddy.sdk.model.BuyBuddyItem;
import co.buybuddy.sdk.responses.OrderDelegate;

import static android.view.View.GONE;

public class MainActivity extends AppCompatActivity {

    long orderId = -1;
    volatile float totalBasket = 0;
    BuyBuddyHitagReleaser hitagReleaser;

    Button btnReset, btnCreateOrder, btnRelease;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnReset = (Button) findViewById(R.id.btnReset);
        btnCreateOrder = (Button) findViewById(R.id.btnCreateOrder);
        btnRelease = (Button) findViewById(R.id.btnPay);

        btnRelease.setVisibility(GONE);
        btnRelease.setAlpha(0);


        BuyBuddy.sdkInitialize(this);
        BuyBuddy.getInstance().api
                .setUserToken("+ymQgwO0QTSXqmduK/4Yxg1qOjp4/U3Fgr7AdtSyI2fLCvzwnj1OJYMLVKWr0Sx5krLZ6LeAQ/OEIOV+vGLJ5g==")
                .setSandBoxMode(true);

        btnCreateOrder.setVisibility(GONE);


        BuyBuddy.getInstance().shoppingCart.getItems();


        BuyBuddy.getInstance().api.getProductWithHitagId(BuyBuddyUtil.isValidPatternForHitag("01-0000-0007"), new BuyBuddyApiCallback<BuyBuddyItem>() {
            @Override
            public void success(BuyBuddyApiObject<BuyBuddyItem> response) {
                //Log.i("ITEM :", Buyresponse.getData().toString());

                final BuyBuddyItem six = response.getData();
                totalBasket += six.getPrice().getCurrentPrice();
                BuyBuddy.getInstance().shoppingCart.addToBasket(six);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        btnCreateOrder.setVisibility(View.VISIBLE);
                        btnCreateOrder.animate().alpha(1).setDuration(600);

                    }
                });
            }

            @Override
            public void error(BuyBuddyApiError error) {

            }
        });

        BuyBuddy.getInstance().api.setInvalidationTokenDelegate(
                new BuyBuddyUserTokenExpiredDelegate() {
            @Override
            public void tokenExpired() {

            }
        });

        btnCreateOrder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (HitagScanService.validateActiveHitag("0100000007")){
                    BuyBuddy.getInstance().api.createOrder(new int[]{8}, totalBasket,

                            new BuyBuddyApiCallback<OrderDelegate>() {
                                @Override
                                public void success(BuyBuddyApiObject<OrderDelegate> response) {
                                    orderId = response.getData().getOrderId();
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            btnRelease.setVisibility(View.VISIBLE);
                                            btnRelease.animate().alpha(1).setDuration(500);
                                        }
                                    });
                                }

                                @Override
                                public void error(BuyBuddyApiError error) {

                                }
                            });
                    }
            }
        });

        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                orderId = -1;
                btnRelease.setVisibility(GONE);
                //btnCreateOrder.setVisibility(GONE);
                getOrderId();
            }
        });



        btnRelease.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        BuyBuddyHitagReleaser.startReleasing(orderId, MainActivity.this, new BuyBuddyHitagReleaser.Delegate() {
                            @Override
                            public void statusUpdate(String hitagId, BuyBuddyHitagReleaser.Status hitagStatus) {

                            }

                            @Override
                            public void completed(String hitagID) {

                            }

                            @Override
                            public void error(BuyBuddyHitagReleaser.HitagReleaserError error) {

                            }
                        });
                    }
                });
            }
        });
    }

    private void getOrderId() {

    }

}
