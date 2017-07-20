package co.buybuddy.sampledelegateapp;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.phoenixframework.channels.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import co.buybuddy.sampledelegateapp.events.MessageEvent;

/**
 * Created by furkan on 7/19/17.
 * Gururla sunar. AHAHAHAHA Some spagetties
 */

interface UIInterface{
    void sendMessage(String message);
}

public class SocketTest extends Service {

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvent event) {
        sendMessage(event.getMessage());
    }

    public void sendMessage(String message) {
        if (socket.isConnected()) {
            try {
                ObjectNode node = new ObjectNode(JsonNodeFactory.instance)
                        .put("user", "my_username")
                        .put("body", message);

                channel.push("new:msg", node);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        EventBus.getDefault().register(this);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    Socket socket;
    Channel channel;

    @Override
    public void onCreate() {
        super.onCreate();


        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> objectMap = new HashMap<>();
        objectMap.put("HELLO", 123);
        final JsonNode node = mapper.convertValue(objectMap, JsonNode.class);

        try {
            socket = new Socket("ws://192.168.1.106:4000/virtual_basket/websocket");
            socket.connect();

        channel = socket.chan("rooms:lobby", null);

        channel.join()
                .receive("ignore", new IMessageCallback() {
                    @Override
                    public void onMessage(Envelope envelope) {
                        System.out.println("IGNORE");
                    }
                })
                .receive("ok", new IMessageCallback() {
                    @Override
                    public void onMessage(Envelope envelope) {
                        System.out.println("JOINED with " + envelope.toString());
                    }
                });

        channel.on("new:msg", new IMessageCallback() {
            @Override
            public void onMessage(Envelope envelope) {
                System.out.println("NEW MESSAGE: " + envelope.toString());
            }
        });

        channel.on(ChannelEvent.CLOSE.getPhxEvent(), new IMessageCallback() {
            @Override
            public void onMessage(Envelope envelope) {
                System.out.println("CLOSED: " + envelope.toString());
            }
        });

        channel.on(ChannelEvent.ERROR.getPhxEvent(), new IMessageCallback() {
            @Override
            public void onMessage(Envelope envelope) {
                System.out.println("ERROR: " + envelope.toString());
            }
        });

        //Sending a message. This library uses Jackson for JSON serialization
        /*ObjectNode node = new ObjectNode(JsonNodeFactory.instance)
                .put("user", "my_username")
                .put("body", "{\"testData\":\"furkan\"}");

        channel.push("new:msg", node);*/

        } catch (IOException e) {
            e.printStackTrace();
        }


    }
}
