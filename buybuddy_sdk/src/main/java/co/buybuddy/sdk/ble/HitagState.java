package co.buybuddy.sdk.ble;

import java.util.Arrays;

public enum HitagState {

    PASSWORD_IN_PROGRESS("IN_PROGRESS"),
    PASSWORD_FIRST_PAYLOAD("01EE"),
    PASSWORD_SECOND_PAYLOAD("02EE"),
    PASSWORD_THIRD_PAYLOAD("03EE"),

    STATE_LOCKED("10"),
    STATE_LOCKING("11"),
    STATE_UNLOCKED("20"),
    STATE_UNLOCKING("21"),

    PASSWORD_OLD("04"),
    PASSWORD_WRONG("05"),

    RELEASE_PROCESS_STARTING("00"),
    RELEASE_VALIDATION_SUCCESS("01"),
    RELEASE_VALIDATION_FAILED("02"),

    PIN_WAITING_FOR_UNLOCK("31"),
    PIN_WAITING_FOR_LOCK("30"),

    STATE_BUGGY("FF"),
    STATE_NONE("FE"),
    STATE_PIN_INSIDE("FD"),
    STATE_PIN_OUTSIDE("FC"),

    FLASH_ERROR("F1"),
    FLASH_ERROR2("F2"),

    ERROR("FFFF"),
    UNKNOWN(""),

    CONNECTED("CONNECTED"),
    DISCONNECTED("DISCONNECTED"),

    INITIALIZING("init"),
    DISCOVERING("discovering"),

    NOT_FOUND("NOT_FOUND"),
    CONNECTION_FAILED("CONNECTION_FAILED");

    HitagState(String response) {
        this.byteArray = BuyBuddyBleUtils.parseHexBinary(response);
    }

    private byte[] byteArray;

    public static boolean compare(HitagState response, byte[] value) {
        return Arrays.equals(response.byteArray, value);
    }
}
