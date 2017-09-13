package co.buybuddy.sdk.responses;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Furkan Ençkü on 6/14/17.
 * This code written by buybuddy Android Team
 */

public class OrderDelegate implements Parcelable {
    private long sale_id;
    private long delegate_id;
    private float grand_total;
    private int status_flag;

    protected OrderDelegate(Parcel in) {
        sale_id = in.readLong();
        delegate_id = in.readLong();
        grand_total = in.readFloat();
        status_flag = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(sale_id);
        dest.writeLong(delegate_id);
        dest.writeFloat(grand_total);
        dest.writeInt(status_flag);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<OrderDelegate> CREATOR = new Creator<OrderDelegate>() {
        @Override
        public OrderDelegate createFromParcel(Parcel in) {
            return new OrderDelegate(in);
        }

        @Override
        public OrderDelegate[] newArray(int size) {
            return new OrderDelegate[size];
        }
    };

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
