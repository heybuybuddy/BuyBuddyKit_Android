package co.buybuddy.sampledelegateapp;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import org.greenrobot.eventbus.EventBus;

import co.buybuddy.sampledelegateapp.events.MessageEvent;

/**
 * Created by furkan on 7/19/17.
 * Gururla sunar. AHAHAHAHA Some spagetties
 */

public class EmptyTestActivity extends AppCompatActivity {

    Button btnSend;
    EditText txtMessage;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        txtMessage = (EditText) findViewById(R.id.txtMessage);
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
        });



    }
}
