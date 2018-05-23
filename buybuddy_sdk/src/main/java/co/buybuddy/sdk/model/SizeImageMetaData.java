package co.buybuddy.sdk.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

/**
 * Created by Emir on 21.02.2018.
 */

public class SizeImageMetaData implements Serializable {
    private String[] sizes;
    private String[] images;

    public String[] getSizes(){
        return sizes;
    }

    public String[] getImages(){
        return images;
    }

}
