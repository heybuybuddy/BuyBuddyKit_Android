package co.buybuddy.sdk.ble.btleasyncgatt;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;

/**
 * Created by Furkan on 12.03.2018.
 */


class BtleAsyncGattCommand {

    enum CommandType {
        WRITE_DESCRIPTOR,
        WRITE_CHARACTERISTIC,
        READ_CHARACTERISTIC
    }

    private CommandType commandType = null;
    private BluetoothGattCharacteristic characteristic = null;
    private BluetoothGattDescriptor descriptor = null;

    CommandType getCommandType() {
        return commandType;
    }

    BluetoothGattCharacteristic getCharacteristic() {
        return characteristic;
    }

    BluetoothGattDescriptor getDescriptor() {
        return descriptor;
    }

    BtleAsyncGattCommand(CommandType commandType, BluetoothGattCharacteristic characteristic) {
        this.commandType = commandType;
        this.characteristic = characteristic;
    }

    BtleAsyncGattCommand(BluetoothGattDescriptor descriptor) {
        this.commandType = CommandType.WRITE_DESCRIPTOR;
        this.descriptor = descriptor;
    }

}
