package shoplifting_mod;

import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.potions.AbstractPotion;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.shop.StorePotion;
import com.megacrit.cardcrawl.shop.StoreRelic;

import java.util.HashMap;

public class ShopliftingManager {

    // Temp vars
    public static float prevItemX, prevItemY;

    // Flags
    public static boolean isItemSuccessfullyStolen = false;
    public static boolean isKickedOut = false;

    // Stats
    public static float successRateMultiplier = 1;
    public static final int damageAmount = 0;

    // Probability tables
    private static final HashMap<AbstractPotion.PotionRarity, Float> potionProbabilities = new HashMap<AbstractPotion.PotionRarity, Float>(){
        {
            put(AbstractPotion.PotionRarity.COMMON, 0.5f);
            put(AbstractPotion.PotionRarity.UNCOMMON, 0.4f);
            put(AbstractPotion.PotionRarity.RARE, 0.3f);
        }
    };

    private static final HashMap<AbstractRelic.RelicTier, Float> relicProbabilities = new HashMap<AbstractRelic.RelicTier, Float>(){
        {
            put(AbstractRelic.RelicTier.COMMON, 0.5f);
            put(AbstractRelic.RelicTier.SHOP, 0.5f);
            put(AbstractRelic.RelicTier.UNCOMMON, 0.4f);
            put(AbstractRelic.RelicTier.RARE, 0.3f);
        }
    };

    private static final HashMap<AbstractCard.CardRarity, Float> cardProbabilities = new HashMap<AbstractCard.CardRarity, Float>(){
        {
            put(AbstractCard.CardRarity.COMMON, 0.5f);
            put(AbstractCard.CardRarity.UNCOMMON, 0.4f);
            put(AbstractCard.CardRarity.RARE, 0.3f);
        }
    };

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

    public static float getItemSuccessRate(Object item){
        float successRate = successRateMultiplier;
        if (item instanceof StoreRelic) {
            successRate *= relicProbabilities.get(((StoreRelic)item).relic.tier);
        } else if (item instanceof StorePotion) {
            successRate *= potionProbabilities.get(((StorePotion)item).potion.rarity);
        } else if (item instanceof AbstractCard) {
            successRate *= cardProbabilities.get(((AbstractCard)item).rarity);
        }

        // Success rate can't be higher than one hundred percent
        if(successRate > 1){
            successRate = 1;
        }

        return successRate;
    }
}
