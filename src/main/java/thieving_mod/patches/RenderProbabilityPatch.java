package thieving_mod.patches;

import basemod.ReflectionHacks;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.*;
import com.evacipated.cardcrawl.modthespire.patcher.PatchingException;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.potions.AbstractPotion;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.shop.ShopScreen;
import com.megacrit.cardcrawl.shop.StorePotion;
import com.megacrit.cardcrawl.shop.StoreRelic;
import javassist.CannotCompileException;
import javassist.CtBehavior;
import thieving_mod.ThievingMod;
import thieving_mod.handlers.ShopliftingHandler;

public class RenderProbabilityPatch {
    @SpirePatch(
            clz = ShopScreen.class,
            method = "renderCardsAndPrices"
    )
    public static class CardProbPatch {
        @SpireInsertPatch(
                locator = Locator.class,
                localvars = {"c"}
        )
        public static void Insert(Object __instance, SpriteBatch sb, AbstractCard c) {
            renderItemProbability(__instance, sb, c);
        }
    }

    @SpirePatch(
            clz = StoreRelic.class,
            method = "render"
    )
    @SpirePatch(
            clz = StorePotion.class,
            method = "render"
    )
    public static class PotionsAndRelicsProbPatch {
        @SpireInsertPatch(
                locator = Locator.class
        )
        public static void Insert(Object __instance, SpriteBatch sb) {
            renderItemProbability(__instance, sb, null);
        }
    }

    private static void renderItemProbability(Object item, SpriteBatch sb, AbstractCard card) {
        if (ThievingMod.isConfigKeyPressed()) {
            float itemProb = item instanceof ShopScreen ? ShopliftingHandler.getSuccessRate(card) : ShopliftingHandler.getSuccessRate(item);
            float itemX = -1, itemY = -1, dX = -1, dY = -1;
            if (item instanceof StoreRelic) {
                dX = (float) ReflectionHacks.getPrivateStatic(StoreRelic.class, "RELIC_PRICE_OFFSET_X");
                dY = (float) ReflectionHacks.getPrivateStatic(StoreRelic.class, "RELIC_PRICE_OFFSET_Y");
                AbstractRelic relic = ((StoreRelic) item).relic;
                itemX = relic.currentX - dX;
                itemY = relic.currentY - dY;
            } else if (item instanceof StorePotion) {
                dX = (float) ReflectionHacks.getPrivateStatic(StorePotion.class, "RELIC_PRICE_OFFSET_X");
                dY = (float) ReflectionHacks.getPrivateStatic(StorePotion.class, "RELIC_PRICE_OFFSET_Y");
                AbstractPotion potion = ((StorePotion) item).potion;
                itemX = potion.posX - dX;
                itemY = potion.posY - dY;
            } else if (item instanceof ShopScreen) {
                dX = (float) ReflectionHacks.getPrivateStatic(ShopScreen.class, "PRICE_TEXT_OFFSET_X");
                dY = (float) ReflectionHacks.getPrivateStatic(ShopScreen.class, "PRICE_TEXT_OFFSET_Y");
                itemX = card.current_x - dX;
                itemY = card.current_y - dY;
            }
            FontHelper.renderFontLeftTopAligned(sb, FontHelper.tipHeaderFont, String.format("%.0f%%", itemProb*100), itemX, itemY, Color.WHITE);
        }
    }

    private static class Locator extends SpireInsertLocator {
        public int[] Locate(CtBehavior ctMethodToPatch) throws CannotCompileException, PatchingException {
            Matcher matcher = new Matcher.MethodCallMatcher(FontHelper.class, "renderFontLeftTopAligned");
            return LineFinder.findAllInOrder(ctMethodToPatch, matcher);
        }
    }
}