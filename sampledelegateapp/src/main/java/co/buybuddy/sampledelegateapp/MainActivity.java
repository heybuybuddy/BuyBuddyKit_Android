package co.buybuddy.sampledelegateapp;

import android.app.DownloadManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

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

public class MainActivity extends AppCompatActivity {

    long orderId = -1;
    BuyBuddyHitagReleaser hitagReleaser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BuyBuddy.sdkInitialize(this);
        BuyBuddy.getInstance().api
                .setUserToken("+ymQgwO0QTSXqmduK/4Yxg1qOjp4/U3Fgr7AdtSyI2fLCvzwnj1OJYMLVKWr0Sx5krLZ6LeAQ/OEIOV+vGLJ5g==")
                .setSandBoxMode(true);

        BuyBuddy.getInstance().api.getProductWithHitagId("0100000006", new BuyBuddyApiCallback<BuyBuddyItem>() {
            @Override
            public void success(BuyBuddyApiObject<BuyBuddyItem> response) {
                //Log.i("ITEM :", response.getData().toString());

                final BuyBuddyItem seven = response.getData();

                BuyBuddy.getInstance().api.createOrder(new int[]{seven.getHitagIdInt()}, seven.getPrice().getCurrentPrice(),
                        new BuyBuddyApiCallback<OrderDelegate>() {
                            @Override
                            public void success(BuyBuddyApiObject<OrderDelegate> response) {
                                orderId = response.getData().getOrderId();
                            }

                            @Override
                            public void error(BuyBuddyApiError error) {

                            }
                        });

            }


            @Override
            public void error(BuyBuddyApiError error) {

            }
        });

        findViewById(R.id.btnPay).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hitagReleaser = new BuyBuddyHitagReleaser(orderId, MainActivity.this);
            }
        });
    }
}
