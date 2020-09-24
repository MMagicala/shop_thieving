package shoplifting_mod;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.*;
import com.evacipated.cardcrawl.modthespire.patcher.PatchingException;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.GameCursor;
import com.megacrit.cardcrawl.core.OverlayMenu;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.shop.Merchant;
import com.megacrit.cardcrawl.shop.ShopScreen;
import com.megacrit.cardcrawl.shop.StorePotion;
import com.megacrit.cardcrawl.shop.StoreRelic;
import javassist.CannotCompileException;
import javassist.CtBehavior;

public class ItemHoverPatch {
    // Play smoke effect around item when hovered
    @SpirePatch(
            clz = StoreRelic.class,
            method = "update"
    )
    @SpirePatch(
            clz = StorePotion.class,
            method = "update"
    )
    public static class HoverPotionsAndRelicsPatch {
        @SpireInsertPatch(
                locator = ItemHoveredLocator.class
        )
        public static void Insert(Object __instance) {
            if (ShopliftingMod.isConfigKeyPressed()) {
                float x, y;
                if (__instance instanceof StoreRelic) {
                    x = ((StoreRelic) __instance).relic.currentX;
                    y = ((StoreRelic) __instance).relic.currentY;
                } else if (__instance instanceof StorePotion) {
                    x = ((StorePotion) __instance).potion.posX;
                    y = ((StorePotion) __instance).potion.posY;
                }
                // Play smoke effect
                
            }
        }
    }

    @SpirePatch(
            clz = ShopScreen.class,
            method = "update"
    )
    public static class HoverCardsPatch {
        @SpireInsertPatch(
                locator = ItemHoveredLocator.class
        )
        public static void Insert(Object __instance) {
        }
    }

            private static class ItemHoveredLocator extends SpireInsertLocator {
        public int[] Locate(CtBehavior ctMethodToPatch) throws CannotCompileException, PatchingException {
            Matcher matcher = new Matcher.MethodCallMatcher(ShopScreen.class, "moveHand");
            return LineFinder.findAllInOrder(ctMethodToPatch, matcher);
        }
    }

/*
    @SpirePatch(
            clz = StorePotion.class,
            method = "update"
    )
    public static class SetHoverFlagPatch {
        @SpireInsertPatch(
                locator = ItemHoveredLocator.class
        )
        public static void Insert(Object __instance) {
            if (ShopliftingMod.isConfigKeyPressed()) {
                itemHovered = true;
            }
        }
    }
*/
}