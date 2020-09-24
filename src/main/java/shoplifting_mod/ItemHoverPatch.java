package shoplifting_mod;

import basemod.ReflectionHacks;
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
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.shop.Merchant;
import com.megacrit.cardcrawl.shop.ShopScreen;
import com.megacrit.cardcrawl.shop.StorePotion;
import com.megacrit.cardcrawl.shop.StoreRelic;
import javassist.CannotCompileException;
import javassist.CtBehavior;

public class ItemHoverPatch {
    private static boolean itemHovered = false;

    // Show tooltip above a hovered item if hotkey is pressed

    @SpirePatch(
            clz = ShopScreen.class,
            method = "update"
    )
    @SpirePatch(
            clz = StoreRelic.class,
            method = "update"
    )
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

    private static class ItemHoveredLocator extends SpireInsertLocator {
        public int[] Locate(CtBehavior ctMethodToPatch) throws CannotCompileException, PatchingException {
            Matcher matcher = new Matcher.MethodCallMatcher(ShopScreen.class, "moveHand");
            return LineFinder.findAllInOrder(ctMethodToPatch, matcher);
        }
    }

    @SpirePatch(clz = CardCrawlGame.class, method = "render")
    public static class RenderToolTipTextPatch {
/*
        @SpireInsertPatch(
                locator=DisplayCursorLocator.class
        )
        public static void InsertBeforeBlackFadeScreen(CardCrawlGame __instance){
            if (itemHovered) {
                AbstractDungeon.overlayMenu.showBlackScreen();
            }
        }
*/

        @SpireInsertPatch(
                locator=RenderBlackScreenLocator.class
        )
        public static void InsertAfterBlackFadeScreen(CardCrawlGame __instance) {
            if (itemHovered) {
                float x = InputHelper.mX;
                float y = InputHelper.mY - 64;
                SpriteBatch sb = (SpriteBatch)ReflectionHacks.getPrivate(__instance, CardCrawlGame.class, "sb");
                FontHelper.renderFontLeft(sb, FontHelper.bannerFont, "Steal item?", x, y, Color.WHITE);
                itemHovered = false;
            }
        }

        // Locators

        private static class RenderBlackScreenLocator extends SpireInsertLocator {
            public int[] Locate(CtBehavior ctMethodToPatch) throws CannotCompileException, PatchingException {
                Matcher matcher = new Matcher.MethodCallMatcher(CardCrawlGame.class, "renderBlackFadeScreen");
                return LineFinder.findAllInOrder(ctMethodToPatch, matcher);
            }
        }
/*

        private static class DisplayCursorLocator extends SpireInsertLocator {
            public int[] Locate(CtBehavior ctMethodToPatch) throws CannotCompileException, PatchingException {
                Matcher matcher = new Matcher.FieldAccessMatcher(CardCrawlGame.class, "displayCursor");
                return LineFinder.findAllInOrder(ctMethodToPatch, matcher);
            }
        }
*/
    }
}