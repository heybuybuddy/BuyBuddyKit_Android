package co.buybuddy.sdk;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import co.buybuddy.sdk.interfaces.BuyBuddyApiCallback;
import co.buybuddy.sdk.model.Address;
import co.buybuddy.sdk.model.BuyBuddyBasketCampaign;
import co.buybuddy.sdk.model.BuyBuddyCampaign;
import co.buybuddy.sdk.model.BuyBuddyCampaignItem;
import co.buybuddy.sdk.model.BuyBuddyItem;
import co.buybuddy.sdk.responses.BuyBuddyApiError;
import co.buybuddy.sdk.responses.BuyBuddyApiObject;
import co.buybuddy.sdk.responses.OrderDelegate;

public final class BuyBuddyShoppingCartManager {

    private Map<String, BuyBuddyItem> basket;
    private Map<Integer, BuyBuddyCampaign> campaigns;
    private Map<Integer, BuyBuddyCampaignItem> campaignItems;
    private ArrayList<String> hitagIdArrayList = new ArrayList<>();

    public ArrayList<BuyBuddyItem> getItems() {
        if (basket != null) {
            return new ArrayList<>(basket.values());
        }

        ArrayList<BuyBuddyItem> tempArray = new ArrayList<>();

        for(String id: hitagIdArrayList) {
           tempArray.add(basket.get(id));
        }

        return tempArray;
    }

    public Map<Integer, BuyBuddyCampaign> getCampaigns() {
        return campaigns;
    }

    public int[] getHitagIdentifiers() {
        int[] hitagIds = new int[basket.size()];

        int index = 0;

        for (String compiledIdentifier : basket.keySet()) {
            hitagIds[index] = basket.get(compiledIdentifier).getHitagIdInt();
            index++;
        }

        return hitagIds;
    }

    BuyBuddyShoppingCartManager() {
        basket = new HashMap<>();
        campaigns = new HashMap<>();
        campaignItems = new HashMap<>();
    }

    public boolean addToBasket(@NonNull BuyBuddyItem item, @Nullable final BuyBuddyShoppingCartDelegate delegate){
        if (basket != null) {
            hitagIdArrayList.add(item.getHitagId());
            basket.put(item.getHitagId(),item);
            updateBasket(delegate);
            return true;
        }
        return false;
    }

    private void updateBasket(final BuyBuddyShoppingCartDelegate delegate) {

        BuyBuddy.getInstance().api.getCampaigns(
                getHitagIdentifiers(), new BuyBuddyApiCallback<BuyBuddyBasketCampaign>() {
                    @Override
                    public void success(BuyBuddyApiObject<BuyBuddyBasketCampaign> response) {

                        campaigns.clear();

                        if (response.getData() != null) {

                            if (response.getData().getCampaigns() != null)
                                for (BuyBuddyCampaign campaign : response.getData().getCampaigns()) {
                                    campaigns.put(campaign.getId(), campaign);
                                }

                            if (response.getData().getCampaignItems() != null)
                                for (BuyBuddyCampaignItem item : response.getData().getCampaignItems()) {
                                    campaignItems.put(item.getHitagId(), item);
                                }


                            for (String hitagId : basket.keySet()) {

                                BuyBuddyItem item = basket.get(hitagId);

                                if (campaignItems.get(item.getHitagIdInt()) != null) {

                                    BuyBuddyCampaignItem campaignItem = campaignItems.get(item.getHitagIdInt());
                                    basket.get(hitagId).setAppliedCampaingIds(campaignItem.getCampaignIds());
                                    basket.get(hitagId).getPrice().setCampaignedPrice(campaignItem.getCampaignPrice());
                                } else {

                                    basket.get(hitagId).setAppliedCampaingIds(null);
                                    basket.get(hitagId).getPrice().unsetCampaigns();
                                }
                            }
                        }

                        if (delegate != null)
                            delegate.basketAndCampaingsUpdated();
                    }

                    @Override
                    public void error(BuyBuddyApiError error) {

                        campaigns.clear();

                        if (delegate != null)
                            delegate.basketAndCampaingsUpdated();
                    }
                }
        );
    }

    public void createOrder(Address address, String email, String governmentId, BuyBuddyApiCallback<OrderDelegate> delegate) {

        List<Integer> campaignIds = new ArrayList<>();
        int[] campaignIdsArray = new int[campaigns.size()];

        for (BuyBuddyCampaign campaign : campaigns.values()) {
            campaignIds.add(campaign.getId());
        }

        for (int i = 0; i < campaignIds.size(); i++) {
            campaignIdsArray[i] = campaignIds.get(i);
        }

        BuyBuddy.getInstance().api.createOrder(getHitagIdentifiers(), campaignIdsArray, getTotalPrice(), address, email, governmentId, delegate);
    }

    public boolean removeAll(){

        if (basket != null){
            hitagIdArrayList.clear();
            basket.clear();
            return true;
        }

        return false;
    }

    public boolean containsId(@NonNull String hitagId){
        if (basket != null) {
            if(basket.containsKey(hitagId))
            return true;
        }
        return false;
    }

    public boolean removeFromBasket(@NonNull final String hitagId, @Nullable  BuyBuddyShoppingCartDelegate delegate) {
        if (basket != null) {
            if (basket.get(hitagId) != null) {
                basket.remove(hitagId);
                hitagIdArrayList.remove(hitagId);
                updateBasket(delegate);
                return true;
            }
        }

        return false;
    }

    public boolean removeFromBasket(@NonNull BuyBuddyItem item, @Nullable BuyBuddyShoppingCartDelegate delegate) {
        if (basket != null) {
            if (basket.get(item.getHitagId()) != null) {
                basket.remove(item.getHitagId());
                hitagIdArrayList.remove(item.getHitagId());
                updateBasket(delegate);
                return true;
            }
        }

        return false;
    }

    private float getTotalPrice(){

        BigDecimal totalPrice = new BigDecimal("0");

        Iterator<BuyBuddyItem> iter = basket.values().iterator();

        while (iter.hasNext()) {

            BuyBuddyItem item = iter.next();

            BigDecimal price = new BigDecimal(item.getPrice().isCampaignApplied() ?item.getPrice().getCampaignedPrice() : item.getPrice().getCurrentPrice());

            totalPrice = totalPrice.add(price, MathContext.DECIMAL64);

        }

        return  totalPrice.floatValue();
    }

}
