package co.buybuddy.android.model;

/**
 * Created by furkan on 6/14/17.
 * Gururla sunar. AHAHAHAHA Some spagetties
 */

public final class OrderDetail {
    private long sale_id;
    private float total_price;
    private float total_discount_price;
    private float total_campaign_price;
    private String[] hitag_ids;

    public long getSale_id() {
        return sale_id;
    }

    public float getTotal_price() {
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
