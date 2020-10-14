package thieving_mod.handlers;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.potions.AbstractPotion;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.shop.ShopScreen;
import com.megacrit.cardcrawl.shop.StorePotion;
import com.megacrit.cardcrawl.shop.StoreRelic;
import thieving_mod.DialoguePool;
import thieving_mod.ItemStats;
import thieving_mod.ThievingMod;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

public class ShopliftingHandler {

    // Temp vars
    public static float prevItemX, prevItemY;

    // Flags
    public static boolean isItemSuccessfullyStolen = false;
    public static boolean isPlayerKickedOut = false;

    // Stats
    public static float successRateMultiplier = 1;
    public static final int damageAmount = 20;

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
            put(AbstractRelic.RelicTier.SPECIAL, 0.2f);
        }
    };

    private static final HashMap<AbstractCard.CardRarity, Float> cardProbabilities = new HashMap<AbstractCard.CardRarity, Float>(){
        {
            put(AbstractCard.CardRarity.COMMON, 0.5f);
            put(AbstractCard.CardRarity.UNCOMMON, 0.4f);
            put(AbstractCard.CardRarity.RARE, 0.3f);
            put(AbstractCard.CardRarity.SPECIAL, 0.2f);
        }
    };

    public static float getSuccessRate(Object item){
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

    public static void attemptToSteal(Object item){
        // Attempt to steal the item
        float rollResult = ThievingMod.random.nextFloat();
        if (rollResult < ShopliftingHandler.getSuccessRate(item)) {
            // Success! Set flags to true
            isItemSuccessfullyStolen = true;

            // Give player money to "purchase" item
            AbstractDungeon.player.gold += ItemStats.getPrice(item);

            // Call purchase method
            if (item instanceof StoreRelic) {
                ((StoreRelic) item).purchaseRelic();
            } else if (item instanceof StorePotion) {
                ((StorePotion) item).purchasePotion();
            } else if (item instanceof AbstractCard) {
                try {
                    Method purchaseCardMethod = ShopScreen.class.getDeclaredMethod("purchaseCard", AbstractCard.class);
                    if (!purchaseCardMethod.isAccessible()) {
                        purchaseCardMethod.setAccessible(true);
                    }
                    // Run the patched version of the purchase method
                    purchaseCardMethod.invoke(AbstractDungeon.shopScreen, (AbstractCard)item);
                } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        } else {
            // If caught, take damage
            AbstractDungeon.player.damage(new DamageInfo(null, ShopliftingHandler.damageAmount, DamageInfo.DamageType.NORMAL));

            // Play sound
            int coin = ThievingMod.random.nextInt(2);
            String soundKey = coin == 1 ? "BLUNT_FAST" : "BLUNT_HEAVY";
            CardCrawlGame.sound.play(soundKey);

            if(AbstractDungeon.player.isDead){
                return;
            }

            // Kick player out of shop
            AbstractDungeon.closeCurrentScreen();
            ShopliftingHandler.isPlayerKickedOut = true;

            PunishmentHandler.selectRandomPunishment();

            // Load shopkeeper dialogue
            CutsceneHandler.enqueueMerchantDialogue(DialoguePool.CAUGHT);
            CutsceneHandler.enqueueMerchantDialogue(PunishmentHandler.decidedPunishment);
        }
    }
}
