package co.buybuddy.sdk.location;

import java.io.Serializable;

/**
 * Created by furkan on 2/19/18.
 * Gururla sunar. AHAHAHAHA Some spagetties
 */

public class BuyBuddyStore implements Serializable{

    private int id;
    private BuyBuddyLocation location;
    private String name;

    public int getId() {
        return id;
    }

    public BuyBuddyLocation getLocation() {
        return location;
    }

    public String getName() {
        return name;
    }
}
