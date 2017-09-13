package co.buybuddy.sampledelegateapp;

import android.support.v4.util.ArraySet;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;


import java.util.Set;

import co.buybuddy.sdk.BuyBuddy;
import co.buybuddy.sdk.ble.BuyBuddyHitagReleaserDelegate;
import co.buybuddy.sdk.ble.BuyBuddyHitagReleaseManager;
import co.buybuddy.sdk.ble.HitagState;
import co.buybuddy.sdk.ble.exception.BleScanException;
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
    BuyBuddyHitagReleaseManager manager;

    Button btnReset, btnCreateOrder, btnRelease;

    Set<String> hitagIds;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        manager = new BuyBuddyHitagReleaseManager();

        btnReset = findViewById(R.id.btnReset);
        btnCreateOrder = findViewById(R.id.btnCreateOrder);
        btnRelease = findViewById(R.id.btnPay);

        btnRelease.setVisibility(GONE);
        btnRelease.setAlpha(0);

        hitagIds = new ArraySet<>();
        hitagIds.add("FRKN00395");
        hitagIds.add("ERSL01623");
        hitagIds.add("SVDA00907");

        BuyBuddy.sdkInitialize(this);
        BuyBuddy.getInstance().api
                .setUserToken("9Bxs2WY8Sq6y4wYu5SIjUbM6kZl/iENIrfqAaBxAtcGNddDZcU1ANI9g7wUVM4rEDkup8J5gQzuWfGE9uPGhYe==");

        btnCreateOrder.setVisibility(GONE);

        BuyBuddy.getInstance().shoppingCart.getItems();

        BuyBuddy.getInstance().api.setInvalidationTokenDelegate(
                new BuyBuddyUserTokenExpiredDelegate() {
            @Override
            public void tokenExpired() {

            }
        });

        for (String hitagId : hitagIds) {
            getProduct(hitagId);
        }

        btnCreateOrder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                BuyBuddy.getInstance().api.createOrder(BuyBuddy.getInstance().shoppingCart.getHitagIdentifiers(), totalBasket,

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

                manager.startReleasing(orderId).subscribeForHitagEvents(new BuyBuddyHitagReleaserDelegate() {

                    @Override
                    public void onHitagEvent(String hitagId, HitagState event) {
                        super.onHitagEvent(hitagId, event);

                        Log.d("*x* HitagEvent", hitagId + " " + event.name());
                    }

                    @Override
                    public void onHitagReleased(String hitagId) {
                        super.onHitagReleased(hitagId);

                        Log.d("*x* HitagReleased", hitagId);
                    }

                    @Override
                    public void onHitagFailed(String hitagId, HitagState event) {
                        super.onHitagFailed(hitagId, event);

                        Log.d("*x* Failed", "id" + hitagId + " reason :" + event);
                    }

                    @Override
                    public void onExceptionThrown(BleScanException exception) {
                        super.onExceptionThrown(exception);

                        Log.d("*x* EXCEPTION", "ex" + exception.toString());
                    }

                    @Override
                    public void didFinish() {
                        super.didFinish();

                        Log.d("*x* HitagDidFinish", "Finish");
                    }
                });
            }
        });
    }

    public void getProduct(String hitagId) {
        BuyBuddy.getInstance().api.getProductWithHitagId(hitagId, new BuyBuddyApiCallback<BuyBuddyItem>() {
            @Override
            public void success(BuyBuddyApiObject<BuyBuddyItem> response) {

                synchronized (BuyBuddy.getInstance().shoppingCart) {

                    final BuyBuddyItem first = response.getData();
                    totalBasket += first.getPrice().getCurrentPrice();

                    BuyBuddy.getInstance().shoppingCart.addToBasket(first);
                }

                if (BuyBuddy.getInstance().shoppingCart.getItems().size() == 3) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            btnCreateOrder.setVisibility(View.VISIBLE);
                            btnCreateOrder.animate().alpha(1).setDuration(600);
                        }
                    });
                }
            }

            @Override
            public void error(BuyBuddyApiError error) {
                Log.d("BB ERROR", error.toString());
            }
        });
    }

    private void getOrderId() {

    }

}
