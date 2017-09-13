package co.buybuddy.sampledelegateapp;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import co.buybuddy.sdk.ble.BuyBuddyHitagReleaser;

/**
 * Created by Furkan Ençkü on 8/22/17.
 * This code written by buybuddy Android Team
 */

public class BleTestActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ble_test);
    }
}
