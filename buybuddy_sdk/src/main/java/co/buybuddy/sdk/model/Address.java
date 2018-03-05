package co.buybuddy.sdk.model;

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
    private String zipcode;
    private String address;
    private String country;
    private Integer id = -1000;


    protected Address(Parcel in) {
        title = in.readString();
        street = in.readString();
        region = in.readString();
        city = in.readString();
        zipcode = in.readString();
        address = in.readString();
        country = in.readString();
        id = in.readInt();
    }

    public Address(String title,String street,String region,String city,String zipcode,String address,String country){
            this.title = title;
            this.street = street;
            this.region = region;
            this.city = city;
            this.zipcode = zipcode;
            this.address = address;
            this.country = country;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(title);
        dest.writeString(street);
        dest.writeString(region);
        dest.writeString(city);
        dest.writeString(zipcode);
        dest.writeString(address);
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

    public String getZipcode(){
        return zipcode;
    }

    public String getDefinition(){
        return address;
    }

    public String getCountry(){
        return country;
    }

    public Integer getId(){
        return id;
    }
}
