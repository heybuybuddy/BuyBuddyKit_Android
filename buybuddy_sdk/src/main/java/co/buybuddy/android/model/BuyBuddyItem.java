package co.buybuddy.android.model;

/**
 * Created by furkan on 6/12/17.
 * Gururla sunar. AHAHAHAHA Some spagetties
 */

public final class BuyBuddyItem {
    private String hitag_id;
    private String name;
    private int h_id;
    private int id;
    private BuyBuddyItemData metadata;
    private String image;
    private String description;
    private BuyBuddyItemPrice price;

    public String getHitagId() {
        return hitag_id;
    }

    public int getHitagIdInt() {
        return h_id;
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
        return image;
    }

    public BuyBuddyItemPrice getPrice() {
        return price;
    }
}

