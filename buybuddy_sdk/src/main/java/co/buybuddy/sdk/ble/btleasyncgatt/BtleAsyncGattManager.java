package co.buybuddy.sdk.ble.btleasyncgatt;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Semaphore;

import co.buybuddy.sdk.BuyBuddyUtil;

/**
 * Created by Furkan on 12.03.2018.
 */

public class BtleAsyncGattManager extends Thread implements Runnable {

    public static String TAG = "BtleAsyncGattManager";
    private BluetoothGatt gatt;

    private BlockingQueue<BtleAsyncGattCommand> queue;
    private Semaphore queueReadySemaphore = new Semaphore(0);


    public BtleAsyncGattManager(BluetoothGatt gatt) {
        this.gatt = gatt;
        queue = new ArrayBlockingQueue<>(10);
    }

    public void cancel() {
        this.interrupt();
    }

    public void enable() {
        BuyBuddyUtil.printD(TAG, "Enabling btle command thread");
        queueReadySemaphore.release();
    }

    public void writeCompleted() {
        queueReadySemaphore.release();
    }

    public void readCompleted() {
        queueReadySemaphore.release();
    }

    public void writeDescriptor(BluetoothGattDescriptor descriptor) throws BtleAsyncGattError {

        BtleAsyncGattCommand command = new BtleAsyncGattCommand(descriptor);
        try {
            queue.put(command);
        } catch (InterruptedException e) {
            throw new BtleAsyncGattError("Error while writing the descriptor");
        }
    }

    public void writeCharacteristic(BluetoothGattCharacteristic characteristic) throws BtleAsyncGattError {

        BtleAsyncGattCommand command = new BtleAsyncGattCommand(
                BtleAsyncGattCommand.CommandType.WRITE_CHARACTERISTIC, characteristic);
        try {
            queue.put(command);
        } catch (InterruptedException e) {
            throw new BtleAsyncGattError("Error while writing the characteristic");
        }
    }

    public void readCharacteristic(BluetoothGattCharacteristic characteristic) throws BtleAsyncGattError {

        BtleAsyncGattCommand command = new BtleAsyncGattCommand(
                BtleAsyncGattCommand.CommandType.READ_CHARACTERISTIC, characteristic);
        try {
            queue.put(command);
        } catch (InterruptedException e) {
            throw new BtleAsyncGattError("Error while reading the characteristic");
        }
    }

    @Override
    public void run() {
        BtleAsyncGattCommand command;

        try {
            while (!(Thread.currentThread().isInterrupted())) {

                queueReadySemaphore.acquire();

                BuyBuddyUtil.printD(TAG, "ttt- Waiting for BTLE command");

                command = queue.take();

                switch (command.getCommandType()) {
                    case WRITE_DESCRIPTOR:
                        BuyBuddyUtil.printD(TAG,"Starting to write descriptor:" + command.getDescriptor().getUuid());
                        if (!gatt.writeDescriptor(command.getDescriptor())) {
                            throw new BtleAsyncGattError("Error while writing the descriptor");
                        }

                        break;
                    case WRITE_CHARACTERISTIC:
                        BuyBuddyUtil.printD(TAG,"Starting to write characteristic:" + command.getCharacteristic().getUuid());
                        if (!gatt.writeCharacteristic(command.getCharacteristic())) {
                            throw new BtleAsyncGattError("Error while writing the characteristic");
                        }

                        break;
                    case READ_CHARACTERISTIC:
                        BuyBuddyUtil.printD(TAG,"Starting to read characteristic:" + command.getCharacteristic().getUuid());
                        if (!gatt.readCharacteristic(command.getCharacteristic())) {
                            throw new BtleAsyncGattError("Error while writing the characteristic");
                        }

                        break;
                    default:
                        BuyBuddyUtil.printD(TAG,"Unknown command received");
                        break;
                }
            }

        } catch (InterruptedException e) {
            BuyBuddyUtil.printD(TAG, "Btle thread interrupted, closing.");
        } catch (BtleAsyncGattError e) {
            BuyBuddyUtil.printD(TAG, "Error while reading:" + e.getMessage());
            //sensor.retry();
        }

        gatt = null;
        queue = null;
        queueReadySemaphore = null;
        Thread.currentThread().interrupt();
    }
}
