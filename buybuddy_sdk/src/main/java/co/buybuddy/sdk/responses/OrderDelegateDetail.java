package co.buybuddy.sdk.responses;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Furkan Ençkü on 6/14/17.
 * This code written by buybuddy Android Team
 */

public class OrderDelegateDetail implements Parcelable {
    private long sale_id;
    private float total_price;
    private float total_discount_price;
    private float total_campaign_price;
    private String hitag_ids[];

    protected OrderDelegateDetail(Parcel in) {
        sale_id = in.readLong();
        total_price = in.readFloat();
        total_discount_price = in.readFloat();
        total_campaign_price = in.readFloat();
        hitag_ids = in.createStringArray();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(sale_id);
        dest.writeFloat(total_price);
        dest.writeFloat(total_discount_price);
        dest.writeFloat(total_campaign_price);
        dest.writeStringArray(hitag_ids);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<OrderDelegateDetail> CREATOR = new Creator<OrderDelegateDetail>() {
        @Override
        public OrderDelegateDetail createFromParcel(Parcel in) {
            return new OrderDelegateDetail(in);
        }

        @Override
        public OrderDelegateDetail[] newArray(int size) {
            return new OrderDelegateDetail[size];
        }
    };

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