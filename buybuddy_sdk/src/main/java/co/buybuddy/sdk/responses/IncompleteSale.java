package co.buybuddy.sdk.responses;

/**
 * Created by furkan on 6/14/17.
 * Gururla sunar. AHAHAHAHA Some spagetties
 */

public class IncompleteSale {
    private long sale_id;
    private String[] hitag_ids;
    private int hitag_completion_count;
    private int hitag_count;
    private int status_flag;

    public long getSaleId() {
        return sale_id;
    }

    public String[] getHitagIds() {
        return hitag_ids;
    }

    public int getHitagCompletionCount() {
        return hitag_completion_count;
    }

    public int getHitagCount() {
        return hitag_count;
    }

    public int getStatusFlag() {
        return status_flag;
    }
}
