package co.buybuddy.sdk.model;

import java.io.Serializable;
import java.math.BigDecimal;

final public class BuyBuddyItemPrice implements Serializable {
    private float price;
    private float discount_price;
    private float ex_price;
    private float old_price;
    private boolean isCampaignApplied = false;
    private float campaigned_price;

    public float getCampaignedPrice() {
        return campaigned_price;
    }

    public void setCampaignedPrice(float price) {
        isCampaignApplied = true;
        this.campaigned_price = price;
    }

    public float getOldPrice() {
        return old_price;
    }

    public boolean isCampaignApplied() {
        return isCampaignApplied;
    }

    public void unsetCampaigns() {
        this.isCampaignApplied = false;
    }

    public float getCurrentPrice() {
        return price;
    }

    public float getDiscountedPrice() {
        return discount_price;
    }
}
