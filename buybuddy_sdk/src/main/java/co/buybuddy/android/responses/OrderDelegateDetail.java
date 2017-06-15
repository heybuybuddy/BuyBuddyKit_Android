package co.buybuddy.android.responses;

/**
 * Created by furkan on 6/14/17.
 * Gururla sunar. AHAHAHAHA Some spagetties
 */

public class OrderDelegateDetail {
    private long sale_id;
    private float total_price;
    private float total_discount_price;
    private float total_campaign_price;
    private String hitag_ids[];

    public long getSaleId() {
        return sale_id;
    }

    public float getTotalPrice() {
        return total_price;
    }

    public float getTotalDiscountPrice() {
        return total_discount_price;
    }

    public float getTotalCampaignPrice() {
        return total_campaign_price;
    }

    public String[] getHitagIds() {
        return hitag_ids;
    }
}