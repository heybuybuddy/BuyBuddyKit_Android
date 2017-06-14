package co.buybuddy.android;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

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

        BuyBuddyApi.getSharedInstance().getProductWithHitagId("01-0000-0001", new BuyBuddyApiCallback<BuyBuddyItem>() {
            @Override
            public void success(BuyBuddyApiObject<BuyBuddyItem> response) {

            }

            @Override
            public void error(BuyBuddyApiError error) {

            }
        });
    }
}
