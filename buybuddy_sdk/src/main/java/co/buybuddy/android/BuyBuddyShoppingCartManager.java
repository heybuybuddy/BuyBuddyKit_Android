package co.buybuddy.android;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;

import com.polidea.rxandroidble.RxBleClient;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import co.buybuddy.android.model.BuyBuddyItem;
import co.buybuddy.android.util.BuyBuddyError;

/**
 * Created by Emir on 29/06/2017.
 */

public class BuyBuddyShoppingCartManager {

    private static Context mContext;
    public static BuyBuddyShoppingCartManager _instance = new BuyBuddyShoppingCartManager();
    public  static Map<String, BuyBuddyItem> basket;
    private Float totPrice = 0.0f;


    private BuyBuddyShoppingCartManager() {
        basket = new HashMap<>();
    }

    public Float totalPrice(){
        BuyBuddyShoppingCartManager._instance.basket.values();

        Iterator<BuyBuddyItem> iter = BuyBuddyShoppingCartManager.basket.values().iterator();

        while (iter.hasNext()) {
            BuyBuddyItem product = iter.next();

                totPrice += product.getPrice().getCurrentPrice();

        }

        return  totPrice;
    }





}
