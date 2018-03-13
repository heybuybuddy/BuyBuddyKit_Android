package co.buybuddy.sdk.ble.btleasyncgatt;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;

/**
 * Created by Furkan on 12.03.2018.
 */


public class BtleAsyncGattCommand {

    enum CommandType {
        WRITE_DESCRIPTOR,
        WRITE_CHARACTERISTIC,
        READ_CHARACTERISTIC
    }

    private CommandType commandType = null;
    private BluetoothGattCharacteristic characteristic = null;
    private BluetoothGattDescriptor descriptor = null;

    public CommandType getCommandType() {
        return commandType;
    }

    public BluetoothGattCharacteristic getCharacteristic() {
        return characteristic;
    }

    public BluetoothGattDescriptor getDescriptor() {
        return descriptor;
    }

    public BtleAsyncGattCommand(CommandType commandType, BluetoothGattCharacteristic characteristic) {
        this.commandType = commandType;
        this.characteristic = characteristic;
    }

    public BtleAsyncGattCommand(BluetoothGattDescriptor descriptor) {
        this.commandType = CommandType.WRITE_DESCRIPTOR;
        this.descriptor = descriptor;
    }

}
