package co.buybuddy.sampledelegateapp;

import android.graphics.Color;
import android.support.v4.util.ArraySet;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Set;

import co.buybuddy.sampledelegateapp.adapter.HitagReleaseStatusAdapter;
import co.buybuddy.sampledelegateapp.adapter.HitagViewHolder;
import co.buybuddy.sdk.BuyBuddy;
import co.buybuddy.sdk.BuyBuddyShoppingCartDelegate;
import co.buybuddy.sdk.BuyBuddyUtil;
import co.buybuddy.sdk.ble.BuyBuddyHitagReleaserDelegate;
import co.buybuddy.sdk.ble.BuyBuddyHitagReleaseManager;
import co.buybuddy.sdk.ble.HitagState;
import co.buybuddy.sdk.interfaces.BuyBuddyApiCallback;
import co.buybuddy.sdk.interfaces.BuyBuddyUserTokenExpiredDelegate;
import co.buybuddy.sdk.location.BuyBuddyStore;
import co.buybuddy.sdk.location.BuyBuddyStoreInfoDelegate;
import co.buybuddy.sdk.model.Address;
import co.buybuddy.sdk.responses.BuyBuddyApiError;
import co.buybuddy.sdk.responses.BuyBuddyApiObject;
import co.buybuddy.sdk.model.BuyBuddyItem;
import co.buybuddy.sdk.responses.OrderDelegate;

import static android.view.View.GONE;

public class MainActivity extends AppCompatActivity {

    long orderId = -1;
    volatile BigDecimal totalBasket = new BigDecimal("0");
    BuyBuddyHitagReleaseManager manager;


    Button btnReset, btnCreateOrder, btnRelease;
    Set<String> hitagIds;
    RecyclerView hitagStatusView;
    HitagReleaseStatusAdapter hitagStatusAdapter;

    String qwe = "Furkan";
    String qwe2 = qwe;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnReset = findViewById(R.id.btnReset);
        btnCreateOrder = findViewById(R.id.btnCreateOrder);
        btnRelease = findViewById(R.id.btnPay);

        btnRelease.setVisibility(GONE);
        btnRelease.setAlpha(0);

        hitagIds = new ArraySet<>();
        hitagIds.add("DENZ15084");
        hitagIds.add("SLDA06778");

        manager = new BuyBuddyHitagReleaseManager();
        BuyBuddy.getInstance().api
                .setSandBoxMode(true)
                .setPublicAuthParameters("b2Z1cmthbmFiaTEyMzY4OTMxMjMxZmF1cmw5NzEyMzQxbWFkeMOnZMOnZA==", "ANDROIDTEST")
                .setUserToken("dLgOo29JTrqafHVp/8VZYcBeqiWua0zHn888OzFsZcv2+VZ3XjFACbfgihGllovVO3D/W17wRXqwL1EFV2Gpaw==");

        btnCreateOrder.setVisibility(GONE);

        BuyBuddy.getInstance().shoppingCart.getItems();

        BuyBuddy.getInstance().api.setInvalidationTokenDelegate(
                new BuyBuddyUserTokenExpiredDelegate() {
            @Override
            public void tokenExpired() {
                Log.d("*x*", "TOKENEXPIRED");
            }
        });

        for (String hitagId : hitagIds) {
            getProduct(hitagId);
        }

        final GridLayoutManager layoutManager = new GridLayoutManager(this,3);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);

        hitagStatusView = findViewById(R.id.hitagStatusView);
        hitagStatusView.setLayoutManager(layoutManager);
        hitagStatusAdapter = new HitagReleaseStatusAdapter(this);
        hitagStatusView.setAdapter(hitagStatusAdapter);
        hitagStatusView.setItemAnimator(null);

        BuyBuddy.getInstance().getStoreInfoProvider().setDelegate(new BuyBuddyStoreInfoDelegate() {
            @Override
            public void enterRegion(BuyBuddyStore store) {
                Log.d("asdas", "enterRegion: ");
            }

            @Override
            public void activeRegion(BuyBuddyStore store) {
                Log.d("asdasd", "activeRegion: ");
            }

            @Override
            public void exitRegion(BuyBuddyStore store) {
            }
        });

        btnCreateOrder.setOnClickListener(new View.OnClickListener() {
            Address address = new Address("asd","asd","asd","asd","asd","asd","asd");

            @Override
            public void onClick(View view) {

            BuyBuddy.getInstance().shoppingCart.createOrder(address,"emir@buybuddy.co","22273019220",new BuyBuddyApiCallback<OrderDelegate>() {
                @Override
                public void success(final BuyBuddyApiObject<OrderDelegate> response) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            orderId = response.getData().getOrderId();
                            btnRelease.setVisibility(View.VISIBLE);
                            btnRelease.animate().alpha(1);
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
                getOrderId();

                hitagStatusAdapter.clear();
            }
        });

        btnRelease.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                manager.startReleasing(orderId).subscribeForHitagEvents(new BuyBuddyHitagReleaserDelegate() {

                    @Override
                    public void onHitagEvent(final String hitagId, HitagState event) {
                        super.onHitagEvent(hitagId, event);

                        Log.d("*x* HitagEvent", hitagId + " " + event.name());

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                hitagStatusAdapter.updateHitag(hitagId, 1, Color.GRAY);

                                int viewPosition = hitagStatusAdapter.findPositionWith(hitagId);
                                if (viewPosition != -1) {
                                    HitagViewHolder holder = (HitagViewHolder) hitagStatusView.findViewHolderForLayoutPosition(viewPosition);
                                    if (holder != null) {
                                        Log.d("HOLDER FOUND", hitagId);
                                    } else {
                                        Log.d("HOLDER NOT FOUND", hitagId);
                                    }
                                }
                            }
                        });
                    }

                    @Override
                    public void onHitagReleased(final String hitagId) {
                        super.onHitagReleased(hitagId);

                        Log.d("*x* HitagReleased", hitagId);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                hitagStatusAdapter.updateHitag(hitagId, 1, Color.GREEN);

                                int viewPosition = hitagStatusAdapter.findPositionWith(hitagId);
                                if (viewPosition != -1) {
                                    HitagViewHolder holder = (HitagViewHolder) hitagStatusView.findViewHolderForLayoutPosition(viewPosition);
                                    if (holder != null) {
                                        Log.d("HOLDER FOUND", hitagId);
                                        hitagStatusAdapter.animateLights(holder, Color.GREEN);
                                    } else {
                                        Log.d("HOLDER NOT FOUND", hitagId);
                                    }
                                }
                            }
                        });
                    }

                    @Override
                    public void onHitagFailed(final String hitagId, HitagState event) {
                        super.onHitagFailed(hitagId, event);

                        Log.d("*x* Failed", hitagId + " reason :" + event);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                hitagStatusAdapter.updateHitag(hitagId, 1, Color.RED);

                                int viewPosition = hitagStatusAdapter.findPositionWith(hitagId);
                                if (viewPosition != -1) {
                                    HitagViewHolder holder = (HitagViewHolder) hitagStatusView.findViewHolderForLayoutPosition(viewPosition);
                                    if (holder != null) {
                                        Log.d("HOLDER FOUND", hitagId);
                                        hitagStatusAdapter.animateLights(holder, Color.RED);
                                    } else {
                                        Log.d("HOLDER NOT FOUND", hitagId);
                                    }
                                }
                            }
                        });
                    }

                    @Override
                    public void onExceptionThrown(Exception exception) {
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

        qwe2 = "f";
        Log.d("", "onCreate: ");

        BuyBuddyUtil.setDEBUG(false);

    }

    public void getProduct(String hitagId) {

        BuyBuddy.getInstance().api.getProductWithHitagId(hitagId, new BuyBuddyApiCallback<BuyBuddyItem>() {
            @Override
            public void success(BuyBuddyApiObject<BuyBuddyItem> response) {

                synchronized (BuyBuddy.getInstance().shoppingCart) {

                    final BuyBuddyItem first = response.getData();

                    totalBasket = totalBasket.add(new BigDecimal(first.getPrice().getCurrentPrice()), MathContext.DECIMAL64);

                    BuyBuddyUtil.printD("PRICE : ", response.getData().getPrice().getCurrentPrice() + "");

                    BuyBuddy.getInstance().shoppingCart.addToBasket(first, new BuyBuddyShoppingCartDelegate() {
                        @Override
                        public void basketAndCampaingsUpdated() {

                            Object object = BuyBuddy.getInstance().shoppingCart.getCampaigns();
                        }
                    });
                }

                if (BuyBuddy.getInstance().shoppingCart.getItems().size() == hitagIds.size()) {
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
                Log.d("BB ERROR", error.toString() + " " + error.getResponseCode());
            }
        });
    }

    private void getOrderId() {

    }

}
