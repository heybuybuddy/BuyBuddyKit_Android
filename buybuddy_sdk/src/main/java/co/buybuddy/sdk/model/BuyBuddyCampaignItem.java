package co.buybuddy.sdk.model;

/**
 * Created by furkan on 10/22/17.
 * Gururla sunar. AHAHAHAHA Some spagetties
 */

public class BuyBuddyCampaignItem {

    private int hitag_id;
    private int item_id;
    private float old_price;
    private float price;
    private float discount_price;
    private float discount_ratio;
    private float campaign_price;
    private float tax_rate;
    private float tax_price;
    private float tax_excluded_price;
    private int[] campaign_id;

    public int[] getCampaignIds() {
        return campaign_id;
    }

    public int getHitagId() {
        return hitag_id;
    }

    public int getItemId() {
        return item_id;
    }

    public float getOldPrice() {
        return old_price;
    }

    public float getPrice() {
        return price;
    }

    public float getDiscountedPrice() {
        return discount_price;
    }

    public float getDiscountRatio() {
        return discount_ratio;
    }

    public float getCampaignPrice() {
        return campaign_price;
    }

    public float getTaxRate() {
        return tax_rate;
    }

    public float getTaxPrice() {
        return tax_price;
    }

    public float getTaxExcludedPrice() {
        return tax_excluded_price;
    }
}