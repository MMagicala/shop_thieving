package shoplifting_mod;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.shop.StorePotion;
import com.megacrit.cardcrawl.shop.StoreRelic;

public class ShopliftingManager {

    // Temp vars
    public static float prevItemX, prevItemY;

    // Flags
    public static boolean isItemSuccessfullyStolen = false;
    public static boolean isKickedOut = false;

    // Stats
    public static float BASE_SUCCESS_RATE = 0.4f;
    public static float successRate = BASE_SUCCESS_RATE;
    public static final int damageAmount = 0;

    /**
     * Determine the class of an item and get its price
     */
    public static int getItemPrice(Object item){
        int itemPrice = -1;
        if (item instanceof StoreRelic) {
            itemPrice = ((StoreRelic) item).price;
        } else if (item instanceof StorePotion) {
            itemPrice = ((StorePotion) item).price;
        } else if (item instanceof AbstractCard) {
            itemPrice = ((AbstractCard)item).price;
        }
        return itemPrice;
    }
}
