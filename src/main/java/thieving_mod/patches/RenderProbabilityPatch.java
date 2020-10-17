package thieving_mod.patches;

import basemod.ReflectionHacks;
import com.badlogic.gdx.Gdx;
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
                locator = CardRenderLocator.class,
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
    public static class RelicProbPatch {
        @SpireInsertPatch(
                locator = RelicRenderLocator.class
        )
        public static void Insert(Object __instance, SpriteBatch sb) {
            renderItemProbability(__instance, sb, null);
        }
    }

    @SpirePatch(
            clz = StorePotion.class,
            method = "render"
    )
    public static class PotionProbPatch {
        @SpireInsertPatch(
                locator = PotionRenderLocator.class
        )
        public static void Insert(Object __instance, SpriteBatch sb) {
            renderItemProbability(__instance, sb, null);
        }
    }

    private static float yProgress = 0;

    private static void renderItemProbability(Object item, SpriteBatch sb, AbstractCard card) {
        float itemProb = item instanceof ShopScreen ? ShopliftingHandler.getSuccessRate(card) : ShopliftingHandler.getSuccessRate(item);
        float itemX = -1, itemY = -1, dX = -1, dY = -1;
        Color color;

        if (ThievingMod.isConfigKeyPressed()) {
            yProgress += Gdx.graphics.getDeltaTime();
            if(yProgress > 1){
                yProgress = 1;
            }
        }else if(yProgress > 0){
            // Reset y offset position
            yProgress -= Gdx.graphics.getDeltaTime();
            if(yProgress < 0)
                yProgress = 0;
        }

        if (item instanceof StoreRelic) { // TODO: use reflection hacks only once
            dX = (float) ReflectionHacks.getPrivateStatic(StoreRelic.class, "RELIC_PRICE_OFFSET_X");
            dY = (float) ReflectionHacks.getPrivateStatic(StoreRelic.class, "RELIC_PRICE_OFFSET_Y");
            AbstractRelic relic = ((StoreRelic) item).relic;
            itemX = relic.currentX - dX;
            itemY = relic.currentY - yProgress*dY;
        } else if (item instanceof StorePotion) {
            dX = (float) ReflectionHacks.getPrivateStatic(StorePotion.class, "RELIC_PRICE_OFFSET_X");
            dY = (float) ReflectionHacks.getPrivateStatic(StorePotion.class, "RELIC_PRICE_OFFSET_Y");
            AbstractPotion potion = ((StorePotion) item).potion;
            itemX = potion.posX - dX;
            itemY = potion.posY - yProgress*dY;
        } else if (item instanceof ShopScreen) {
            dX = (float) ReflectionHacks.getPrivateStatic(ShopScreen.class, "PRICE_TEXT_OFFSET_X");
            dY = (float) ReflectionHacks.getPrivateStatic(ShopScreen.class, "PRICE_TEXT_OFFSET_Y");
            itemX = card.current_x - dX;
            itemY = card.current_y - yProgress*dY;
        }

        // Determine color by success rate
        if(itemProb < 0.3f){
            color = Color.RED;
        }else if(itemProb < 0.5f){
            color = Color.ORANGE;
        }else if(itemProb < 1f){
            color = Color.YELLOW;
        }else{
            color = Color.GREEN;
        }
        color.a = yProgress;

        FontHelper.renderFontLeftTopAligned(sb, FontHelper.tipHeaderFont, String.format("%.0f%%", itemProb*100), itemX, itemY, color);
    }

    // Locators

    private static class PotionRenderLocator extends SpireInsertLocator {
        public int[] Locate(CtBehavior ctMethodToPatch) throws CannotCompileException, PatchingException {
            Matcher matcher = new Matcher.MethodCallMatcher(AbstractPotion.class, "shopRender");
            return LineFinder.findInOrder(ctMethodToPatch, matcher);
        }
    }

    private static class RelicRenderLocator extends SpireInsertLocator {
        public int[] Locate(CtBehavior ctMethodToPatch) throws CannotCompileException, PatchingException {
            Matcher matcher = new Matcher.MethodCallMatcher(AbstractRelic.class, "renderWithoutAmount");
            return LineFinder.findInOrder(ctMethodToPatch, matcher);
        }
    }

    private static class CardRenderLocator extends SpireInsertLocator {
        public int[] Locate(CtBehavior ctMethodToPatch) throws CannotCompileException, PatchingException {
            Matcher matcher = new Matcher.MethodCallMatcher(AbstractCard.class, "render");
            return LineFinder.findAllInOrder(ctMethodToPatch, matcher);
        }
    }
}