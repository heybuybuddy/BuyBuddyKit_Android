package co.buybuddy.sdk.model;

import java.io.Serializable;

/**
 * Created by furkan on 10/22/17.
 * Gururla sunar. AHAHAHAHA Some spagetties
 */

public class BuyBuddyBasketCampaign implements Serializable {

    private BuyBuddyCampaignItem[] items;
    private BuyBuddyCampaign[] campaigns;


    public BuyBuddyCampaignItem[] getCampaignItems() {
        return items;
    }

    public BuyBuddyCampaign[] getCampaigns() {
        return campaigns;
    }
}
