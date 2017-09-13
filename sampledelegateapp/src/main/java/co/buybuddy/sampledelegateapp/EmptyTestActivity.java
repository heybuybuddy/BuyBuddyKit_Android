package co.buybuddy.sampledelegateapp;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;
import android.widget.EditText;

import co.buybuddy.sdk.BuyBuddy;
import co.buybuddy.sdk.BuyBuddyHitagReleaser_;

/**
 * Created by Furkan Ençkü on 7/19/17.
 * This code written by buybuddy Android Team
 */

public class EmptyTestActivity extends AppCompatActivity {

    Button btnSend;
    EditText txtMessage;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        /*txtMessage = (EditText) findViewById(R.id.txtMessage);
        btnSend = (Button) findViewById(R.id.btnSend);

        startService(new Intent(this, SocketTest.class));

        txtMessage.setText("");

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = txtMessage.getText().toString();
                txtMessage.setText("");
                EventBus.getDefault().post(new MessageEvent().setMessage(message));
            }
        });*/

        BuyBuddy.sdkInitialize(this);
        BuyBuddyHitagReleaser_.startReleasing(1, new BuyBuddyHitagReleaser_.Delegate() {
            @Override
            public void statusUpdate(String hitagId, BuyBuddyHitagReleaser_.Status hitagStatus) {

            }

            @Override
            public void completed(String hitagID) {

            }

            @Override
            public void error(BuyBuddyHitagReleaser_.HitagReleaserError error) {

            }
        });


    }
}
