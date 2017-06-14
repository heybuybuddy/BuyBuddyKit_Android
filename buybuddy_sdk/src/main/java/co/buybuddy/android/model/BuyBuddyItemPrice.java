package co.buybuddy.android.model;

final public class BuyBuddyItemPrice {
    private float current_price;
    private float discount_price;
    private float ex_price;

    public float getCurrentPrice() {
        return current_price;
    }

    public float getDiscountedPrice() {
        return discount_price;
    }
}
