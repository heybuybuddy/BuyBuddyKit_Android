package co.buybuddy.sampledelegateapp;

import android.animation.Animator;
import android.app.DownloadManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.polidea.rxandroidble.RxBleClient;
import com.polidea.rxandroidble.scan.ScanFilter;
import com.polidea.rxandroidble.scan.ScanResult;
import com.polidea.rxandroidble.scan.ScanSettings;

import co.buybuddy.android.BuyBuddy;
import co.buybuddy.android.BuyBuddyHitagReleaser;
import co.buybuddy.android.interfaces.BuyBuddyApiCallback;
import co.buybuddy.android.responses.BuyBuddyApiError;
import co.buybuddy.android.responses.BuyBuddyApiObject;
import co.buybuddy.android.model.BuyBuddyItem;
import co.buybuddy.android.responses.OrderDelegate;
import rx.functions.Action1;

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

        BuyBuddy.getInstance().api.getProductWithHitagId("0100000006", new BuyBuddyApiCallback<BuyBuddyItem>() {
            @Override
            public void success(BuyBuddyApiObject<BuyBuddyItem> response) {
                //Log.i("ITEM :", response.getData().toString());

                final BuyBuddyItem six = response.getData();
                totalBasket += six.getPrice().getCurrentPrice();

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

        btnCreateOrder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BuyBuddy.getInstance().api.createOrder(new int[]{7}, totalBasket,

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
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        BuyBuddyHitagReleaser.startReleasing(orderId, MainActivity.this);
                    }
                });
            }
        });
    }

    private void getOrderId() {

    }

}
