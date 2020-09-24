package shoplifting_mod;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.shop.ShopScreen;
import com.megacrit.cardcrawl.shop.StorePotion;
import com.megacrit.cardcrawl.shop.StoreRelic;

public class ShopliftingManager {

    // Temp vars
    public static float prevItemX, prevItemY;

    // Flags
    public static boolean isItemSuccessfullyStolen = false;
    public static boolean isKickedOut = false;

    /**
     * Determine the class of an item and get its price
     * @param __instance Item
     * @param hoveredCard Real item if __instance is the shop screen
     */
    public static int getItemPrice(Object __instance, AbstractCard hoveredCard){
        int itemPrice = -1;
        if (__instance instanceof StoreRelic) {
            itemPrice = ((StoreRelic) __instance).price;
        } else if (__instance instanceof StorePotion) {
            itemPrice = ((StorePotion) __instance).price;
        } else if (__instance instanceof ShopScreen) {
            itemPrice = hoveredCard.price;
        }
        return itemPrice;
    }
}
