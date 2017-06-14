package co.buybuddy.sampledelegateapp;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import co.buybuddy.android.BuyBuddy;
import co.buybuddy.android.http.BuyBuddyApi;
import co.buybuddy.android.http.model.BuyBuddyApiCallback;
import co.buybuddy.android.http.model.BuyBuddyApiError;
import co.buybuddy.android.http.model.BuyBuddyApiObject;
import co.buybuddy.android.model.BuyBuddyItem;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BuyBuddy.sdkInitialize(this);

        BuyBuddyApi.getSharedInstance().setSandBoxMode(true)
                                       .setUserToken("+ymQgwO0QTSXqmduK/4Yxg1qOjp4/U3Fgr7AdtSyI2fLCvzwnj1OJYMLVKWr0Sx5krLZ6LeAQ/OEIOV+vGLJ5g==");


        BuyBuddyApi.getSharedInstance().getProductWithHitagId("0100000001", new BuyBuddyApiCallback<BuyBuddyItem>() {
            @Override
            public void success(BuyBuddyApiObject<BuyBuddyItem> response) {
                Log.i("qwe", "");
            }

            @Override
            public void error(BuyBuddyApiError error) {
                Log.i("qwe", "");
            }
        });

    }
}
