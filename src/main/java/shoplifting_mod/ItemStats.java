package shoplifting_mod;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.shop.StorePotion;
import com.megacrit.cardcrawl.shop.StoreRelic;

public class ItemStats {
    public static int getPrice(Object item){
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
