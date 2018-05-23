package co.buybuddy.sdk.model;

import java.io.Serializable;

/**
 * Created by Furkan Ençkü on 6/14/17.
 * This code written by buybuddy Android Team
 */

public final class OrderDetail implements Serializable {
    private long sale_id;
    private float total_price;
    private float total_discount_price;
    private float total_campaign_price;
    private String[] hitag_ids;

    public long getOrderId() {
        return sale_id;
    }

    public float getTotalPrice() {
        return total_price;
    }

    public float getTotal_discount_price() {
        return total_discount_price;
    }

    public float getTotal_campaign_price() {
        return total_campaign_price;
    }

    public String[] getHitag_ids() {
        return hitag_ids;
    }
}
