package co.buybuddy.android.http.model;

/**
 * Created by furkan on 6/14/17.
 * Gururla sunar. AHAHAHAHA Some spagetties
 */

public class OrderDelegate {
    private long sale_id;
    private long delegate_id;
    private long employee_id;
    private float grand_total;
    private int status_flag;
    private int hitag_ids[];

    public long getSaleId(){
        return sale_id;
    }

    public long getDelegateId(){
        return delegate_id;
    }
}
