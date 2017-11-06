package co.buybuddy.sdk.model;

import android.support.annotation.Nullable;

/**
 * Created by Furkan Ençkü on 6/12/17.
 * This code written by buybuddy Android Team
 */

//TODO Serializable !!

public final class BuyBuddyItem {
    private String hitag_id;
    private String name;
    private int h_id;
    private int id;
    private BuyBuddyItemData metadata;
    private String image;
    private String description;
    private BuyBuddyItemPrice price;
    private int[] appliedCampaingIds;

    @Nullable public int[] getAppliedCampaingIds() {
        return appliedCampaingIds;
    }

    public BuyBuddyItem setAppliedCampaingIds(int[] appliedCampaingIds) {
        this.appliedCampaingIds = appliedCampaingIds;
        return this;
    }

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

