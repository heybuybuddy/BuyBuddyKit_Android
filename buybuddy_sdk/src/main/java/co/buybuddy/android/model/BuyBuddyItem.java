package co.buybuddy.android.model;

/**
 * Created by furkan on 6/12/17.
 * Gururla sunar. AHAHAHAHA Some spagetties
 */

public final class BuyBuddyItem {
    private String hitagId;
    private String name;
    private int h_id;
    private int id;
    private BuyBuddyItemData metadata;
    private String imageURL;
    private String description;
    private BuyBuddyItemPrice price;

    public String getHitagId() {
        return hitagId;
    }

    public String getName() {
        return name;
    }

    public BuyBuddyItemData getMetadata() {
        return metadata;
    }

    public String getDescription() {
        return description;
    }

    public String getImageURL() {
        return imageURL;
    }

    public BuyBuddyItemPrice getPrice() {
        return price;
    }
}

