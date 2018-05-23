package co.buybuddy.sdk.model;

import java.io.Serializable;

final public class BuyBuddyItemData implements Serializable {
    private String color;
    private String size;
    private int code;

    public String getColor() {
        return color;
    }

    public String getSize() {
        return size;
    }

    public int getCode() {
        return code;
    }
}
