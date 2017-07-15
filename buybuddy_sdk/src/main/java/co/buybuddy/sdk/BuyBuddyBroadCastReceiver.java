package co.buybuddy.sdk;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

/**
 * Created by Emir on 14/07/2017.
 */

public class BuyBuddyBroadCastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
            context.startService(new Intent(context, HitagScanService.class));
        }

        if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) {

            final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

            if(BluetoothAdapter.STATE_ON == state){
                context.startService(new Intent(context, HitagScanService.class));
            }
        }
    }
}