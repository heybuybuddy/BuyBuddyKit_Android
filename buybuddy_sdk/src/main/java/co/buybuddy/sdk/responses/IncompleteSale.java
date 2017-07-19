package co.buybuddy.sdk.responses;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by furkan on 6/14/17.
 * Gururla sunar. AHAHAHAHA Some spagetties
 */

public class IncompleteSale implements Parcelable {
    
    private long sale_id;
    private String[] hitag_ids;
    private int hitag_completion_count;
    private int hitag_count;
    private int status_flag;

    protected IncompleteSale(Parcel in) {
        sale_id = in.readLong();
        hitag_ids = in.createStringArray();
        hitag_completion_count = in.readInt();
        hitag_count = in.readInt();
        status_flag = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(sale_id);
        dest.writeStringArray(hitag_ids);
        dest.writeInt(hitag_completion_count);
        dest.writeInt(hitag_count);
        dest.writeInt(status_flag);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<IncompleteSale> CREATOR = new Creator<IncompleteSale>() {
        @Override
        public IncompleteSale createFromParcel(Parcel in) {
            return new IncompleteSale(in);
        }

        @Override
        public IncompleteSale[] newArray(int size) {
            return new IncompleteSale[size];
        }
    };

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
