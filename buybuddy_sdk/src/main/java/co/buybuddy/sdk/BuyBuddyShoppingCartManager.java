package co.buybuddy.sdk;

import android.content.Context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import co.buybuddy.sdk.model.BuyBuddyItem;

/**
 * Created by Emir on 29/06/2017.
 */

public class BuyBuddyShoppingCartManager {

    private static Map<String, BuyBuddyItem> basket;
    private Float totPrice = 0.0f;

    public ArrayList<BuyBuddyItem> getItems() {
        if (basket != null) {
            return (ArrayList<BuyBuddyItem>) basket.values();
        }

        return new ArrayList<>();
    }

    BuyBuddyShoppingCartManager() {
        basket = new HashMap<>();
    }

    public boolean addToBasket(BuyBuddyItem item){
        if (basket != null) {
            if(HitagScanService.validateActiveHitag(item.getHitagId()))
            basket.put(item.getHitagId(),item);

            return true;
        }
        return false;
    }

    public boolean containsId(String hitagId){
        if (basket != null) {
            if(basket.containsKey(hitagId))
            return true;
        }
        return false;
    }

    public boolean removeFromBasket(String hitagId) {
        if (basket != null) {
            if (basket.get(hitagId) != null) {
                basket.remove(hitagId);
                return true;
            }
        }

        return false;
    }

    public Float totalPrice(){
       basket.values();

        Iterator<BuyBuddyItem> iter = BuyBuddyShoppingCartManager.basket.values().iterator();

        while (iter.hasNext()) {
            BuyBuddyItem product = iter.next();

                totPrice += product.getPrice().getCurrentPrice();

        }

        return  totPrice;
    }

}
