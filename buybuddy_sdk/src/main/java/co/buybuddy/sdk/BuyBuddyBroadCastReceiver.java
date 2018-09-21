package co.buybuddy.sdk;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;

import static android.bluetooth.BluetoothAdapter.STATE_OFF;
import static android.bluetooth.BluetoothAdapter.STATE_ON;
import static android.bluetooth.BluetoothAdapter.STATE_TURNING_OFF;
import static android.bluetooth.BluetoothAdapter.STATE_TURNING_ON;

/**
 * Created by Emir on 14/07/2017.
 */

public class BuyBuddyBroadCastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {

        if (!(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2)){
            // BuyBuddyScanner can not work properly under android version 4.3
            return;
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
            context.startService(new Intent(context, HitagScanService.class));
        }

        if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) {

            final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

            if(STATE_ON == state){
                context.startService(new Intent(context, HitagScanService.class));
            }

            switch (state) {
                case STATE_ON:
                    EventBus.getDefault().post(BuyBuddyBluetoothState.ON);
                    break;

                case STATE_TURNING_OFF:
                    EventBus.getDefault().post(BuyBuddyBluetoothState.OFF);
                    break;

                case STATE_TURNING_ON:
                    EventBus.getDefault().post(BuyBuddyBluetoothState.TURNING_ON);
                    break;

                case STATE_OFF:
                    EventBus.getDefault().post(BuyBuddyBluetoothState.TURNING_OFF);
                    break;
            }
        }
    }
}