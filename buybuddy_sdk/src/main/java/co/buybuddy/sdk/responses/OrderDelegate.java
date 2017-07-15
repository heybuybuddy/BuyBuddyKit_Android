package co.buybuddy.sdk.responses;

/**
 * Created by furkan on 6/14/17.
 * Gururla sunar. AHAHAHAHA Some spagetties
 */

public class OrderDelegate {
    private long sale_id;
    private long delegate_id;
    private float grand_total;
    private int status_flag;

    public long getOrderId(){
        return sale_id;
    }

    public long getDelegateId(){
        return delegate_id;
    }

    public float getTotalPrice() {
        return grand_total;
    }
}
