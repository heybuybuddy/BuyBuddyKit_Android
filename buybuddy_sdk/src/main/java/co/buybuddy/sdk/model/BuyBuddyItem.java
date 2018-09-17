package co.buybuddy.sdk.model;

import android.support.annotation.Nullable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

import co.buybuddy.sdk.location.BuyBuddyStore;

/**
 * Created by Furkan Ençkü on 6/12/17.
 * This code written by buybuddy Android Team
 */



public final class BuyBuddyItem implements Serializable {
    private String hitag_id;
    private String name;
    private int h_id;
    private int id;
    private BuyBuddyItemData metadata;
    private String[] images;
    private String description;
    private BuyBuddyItemPrice price;
    private int[] appliedCampaingIds;
    private BuyBuddyStore store;
    private HashMap<String, SizeImageMetaData> others;

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

    public String[] getImageURL() {
        return images;
    }

    public BuyBuddyItemPrice getPrice() {
        return price;
    }

    public BuyBuddyStore getStore() {
        return store;
    }

    public HashMap<String,SizeImageMetaData> getOthers(){return others;}

    @Nullable public int[] getAppliedCampaingIds() {
        return appliedCampaingIds;
    }


}

