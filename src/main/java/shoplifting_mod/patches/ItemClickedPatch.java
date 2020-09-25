package shoplifting_mod.patches;

import com.evacipated.cardcrawl.modthespire.lib.*;
import com.evacipated.cardcrawl.modthespire.patcher.PatchingException;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.rooms.ShopRoom;
import com.megacrit.cardcrawl.shop.Merchant;
import com.megacrit.cardcrawl.shop.ShopScreen;
import com.megacrit.cardcrawl.shop.StorePotion;
import com.megacrit.cardcrawl.shop.StoreRelic;
import com.megacrit.cardcrawl.vfx.CollectorCurseEffect;
import javassist.CannotCompileException;
import javassist.CtBehavior;
import shoplifting_mod.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ItemClickedPatch {
    private static final float successRate = 0.5f;
    private static final int damageAmount = 0;

    // Hotkey + click listeners

    @SpirePatch(
            clz = ShopScreen.class,
            method = "update"
    )
    public static class StealCardPatch {
        @SpireInsertPatch(
                locator = ItemClickedLocator.class,
                localvars = {"hoveredCard"}
        )
        public static SpireReturn<Void> Insert(Object __instance, AbstractCard hoveredCard) {
            return CommonInsert(hoveredCard);
        }
    }

    @SpirePatch(
            clz = StorePotion.class,
            method = "update"
    )
    @SpirePatch(
            clz = StoreRelic.class,
            method = "update"
    )
    public static class StealPotionOrRelicPatch {
        @SpireInsertPatch(
                locator = ItemClickedLocator.class
        )
        public static SpireReturn<Void> Insert(Object __instance) {
            return CommonInsert(__instance);
        }
    }

    // Find code right after item was clicked

    private static class ItemClickedLocator extends SpireInsertLocator {
        public int[] Locate(CtBehavior ctMethodToPatch) throws CannotCompileException, PatchingException {
            Matcher matcher = new Matcher.FieldAccessMatcher(Settings.class, "isTouchScreen");
            int[] results = LineFinder.findAllInOrder(ctMethodToPatch, matcher);
            int[] desiredResults = new int[1];
            desiredResults[0] = results[results.length - 1];
            return desiredResults;
        }
    }

    /**
     * Determines if player failed or succeeded in stealing an item
     * @param item the item being clicked on
     */
    private static SpireReturn<Void> CommonInsert(Object item) {
        int itemPrice = ShopliftingManager.getItemPrice(item);
        if (AbstractDungeon.player.gold < itemPrice && ShopliftingMod.isConfigKeyPressed()) {
            // Attempt to steal the item
            float rollResult = ShopliftingMod.random.nextFloat();
            if (rollResult < successRate) {
                // Success! Give the player enough money and purchase the item
                AbstractDungeon.player.gold += itemPrice;
                ShopliftingManager.isItemSuccessfullyStolen = true;
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
                        purchaseCardMethod.invoke(AbstractDungeon.shopScreen, item);
                    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                // Take damage if caught and play sound/vfx
                AbstractDungeon.player.damage(new DamageInfo(null, damageAmount, DamageInfo.DamageType.NORMAL));
                int coin = ShopliftingMod.random.nextInt(2);
                String soundKey = coin == 1 ? "BLUNT_FAST" : "BLUNT_HEAVY";
                CardCrawlGame.sound.play(soundKey);

                // Kick player out of shop if they are alive
                if (!AbstractDungeon.player.isDead) {
                    AbstractDungeon.closeCurrentScreen();
                    ShopliftingManager.isKickedOut = true;
                }
                PunishmentManager.chooseRandomPunishment();

                // Load shopkeeper dialogue
                CutsceneManager.enqueueMerchantDialogue(DialoguePool.CAUGHT.values, 2.5f);
                CutsceneManager.enqueueMerchantDialogue(PunishmentManager.decidedPunishment.dialoguePool, 3f);
                // Add effects for punishment
                Merchant merchant = ((ShopRoom) (AbstractDungeon.getCurrRoom())).merchant;
                CutsceneManager.addEffect(new CollectorCurseEffect(merchant.hb.cX, merchant.hb.cY), 2.5f);
            }

            // Return early
            return SpireReturn.Return(null);
        }
        // Hotkey not pressed, return to normal
        return SpireReturn.Continue();
    }
}
