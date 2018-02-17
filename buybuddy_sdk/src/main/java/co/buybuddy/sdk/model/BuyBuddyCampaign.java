package co.buybuddy.sdk.model;

import java.io.Serializable;

/**
 * Created by furkan on 10/22/17.
 * Gururla sunar. AHAHAHAHA Some spagetties
 */

public class BuyBuddyCampaign implements Serializable {

    private int id;
    private String name;
    private String description;

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getId() {
        return id;
    }
}
