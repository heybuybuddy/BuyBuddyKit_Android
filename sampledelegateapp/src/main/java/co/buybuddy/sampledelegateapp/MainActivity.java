package co.buybuddy.sampledelegateapp;

import android.app.DownloadManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import co.buybuddy.android.BuyBuddy;
import co.buybuddy.android.BuyBuddyApi;
import co.buybuddy.android.interfaces.BuyBuddyApiCallback;
import co.buybuddy.android.responses.BuyBuddyApiError;
import co.buybuddy.android.responses.BuyBuddyApiObject;
import co.buybuddy.android.model.BuyBuddyItem;
import okhttp3.OkHttpClient;
import okhttp3.Request;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BuyBuddy.sdkInitialize(this);
        BuyBuddy.getInstance().api
                .setUserToken("ez73kR5hQeKOwecOc54PZL/A15NA9UlNntZvviAWp3rQmKSN5PJHarZ1we1iWLQdJxqvbwV2RiCtvFxgvaTvIw==")
                .setSandBoxMode(true);

        BuyBuddy.getInstance().api.getProductWithHitagId("0100000001", new BuyBuddyApiCallback<BuyBuddyItem>() {
            @Override
            public void success(BuyBuddyApiObject<BuyBuddyItem> response) {
                Log.i("ITEM :", response.getData().toString());
            }

            @Override
            public void error(BuyBuddyApiError error) {

            }
        });

    }
}
