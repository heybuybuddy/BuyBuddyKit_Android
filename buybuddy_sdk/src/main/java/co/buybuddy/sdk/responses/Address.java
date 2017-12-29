package co.buybuddy.sdk.responses;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

/**
 * Created by Emir on 29/12/2017.
 */

public class Address implements Serializable,Parcelable {

    private String title;
    private String street;
    private String region;
    private String city;
    private Integer zipcode;
    private String definition;
    private String country;
    private Integer id;


    protected Address(Parcel in) {
        title = in.readString();
        street = in.readString();
        region = in.readString();
        city = in.readString();
        zipcode = in.readInt();
        definition = in.readString();
        country = in.readString();
        id = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(title);
        dest.writeString(street);
        dest.writeString(region);
        dest.writeString(city);
        dest.writeInt(zipcode);
        dest.writeString(definition);
        dest.writeString(country);
        dest.writeInt(id);

    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator<Address> CREATOR = new Parcelable.Creator<Address>() {
        @Override
        public Address createFromParcel(Parcel in) {
            return new Address(in);
        }

        @Override
        public Address[] newArray(int size) {
            return new Address[size];
        }
    };

    public String getTitle(){
        return title;
    }

    public String getStreet(){
        return street;
    }

    public String getRegion(){
        return region;
    }

    public String getCity(){
        return city;
    }

    public Integer getZipcode(){
        return zipcode;
    }

    public String getDefinition(){
        return definition;
    }

    public String getCountry(){
        return country;
    }

    public Integer getId(){
        return id;
    }
}
