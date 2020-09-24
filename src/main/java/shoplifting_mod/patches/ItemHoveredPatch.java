package shoplifting_mod.patches;

import basemod.ReflectionHacks;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.*;
import com.evacipated.cardcrawl.modthespire.patcher.PatchingException;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.shop.ShopScreen;
import com.megacrit.cardcrawl.shop.StorePotion;
import com.megacrit.cardcrawl.shop.StoreRelic;
import javassist.CannotCompileException;
import javassist.CtBehavior;
import shoplifting_mod.ShopliftingManager;
import shoplifting_mod.ShopliftingMod;

public class ItemHoveredPatch {
    private static boolean showTooltip = false;

    // Hover over item listeners

    @SpirePatch(
            clz = ShopScreen.class,
            method = "update"
    )
    public static class HoverCardPatch {
        @SpireInsertPatch(
                locator = ItemHoveredLocator.class,
                localvars = {"hoveredCard"}
        )
        public static void Insert(Object __instance, AbstractCard hoveredCard) {
            CommonInsert(__instance, hoveredCard);
        }
    }

    @SpirePatch(
            clz = StoreRelic.class,
            method = "update"
    )
    @SpirePatch(
            clz = StorePotion.class,
            method = "update"
    )
    public static class HoverPotionAndRelicPatch {
        @SpireInsertPatch(
                locator = ItemHoveredLocator.class
        )
        public static void Insert(Object __instance) {
            CommonInsert(__instance, null);
        }
    }

    private static void CommonInsert(Object __instance, AbstractCard hoveredCard) {
        if (ShopliftingMod.isConfigKeyPressed()) {
            int itemPrice = ShopliftingManager.getItemPrice(__instance, hoveredCard);
            if (AbstractDungeon.player.gold < itemPrice) {
                // If we can't afford the item, show tooltip asking if we want to steal it
                showTooltip = true;
            }
        }
    }

    private static class ItemHoveredLocator extends SpireInsertLocator {
        public int[] Locate(CtBehavior ctMethodToPatch) throws CannotCompileException, PatchingException {
            Matcher matcher = new Matcher.MethodCallMatcher(ShopScreen.class, "moveHand");
            return LineFinder.findAllInOrder(ctMethodToPatch, matcher);
        }
    }

    // Render effects when hotkey + hover
    @SpirePatch(
            clz = CardCrawlGame.class,
            method = "render"
    )
    public static class RenderPatch {
        // How long it takes for dark background to fully show/hide
        private static final float FADE_DURATION = 0.5f;

        // Show dark background
        @SpireInsertPatch(
                locator = PreRenderBlackScreenLocator.class
        )
        public static void ShowDarkBackgroundPatch(CardCrawlGame __instance) {
            Color screenColor = (Color) ReflectionHacks.getPrivate(__instance, CardCrawlGame.class, "screenColor");
            if (showTooltip) {
                if (screenColor.a < 0.5f) {
                    screenColor.a += Gdx.graphics.getDeltaTime()/FADE_DURATION;
                    if (screenColor.a > 0.5f) {
                        screenColor.a = 0.5f;
                    }
                }
            } else if (screenColor.a > 0f) {
                screenColor.a -= Gdx.graphics.getDeltaTime()/FADE_DURATION;
                if (screenColor.a < 0f) {
                    screenColor.a = 0f;
                }
            }
        }

        // Show tooltip
        @SpireInsertPatch(
                locator = PostRenderBlackScreenLocator.class
        )
        public static void ShowTooltipPatch(CardCrawlGame __instance) {
            if (showTooltip) {
                float x = InputHelper.mX;
                float y = InputHelper.mY - 64;
                SpriteBatch sb = (SpriteBatch) ReflectionHacks.getPrivate(__instance, CardCrawlGame.class, "sb");
                FontHelper.renderFontLeft(sb, FontHelper.bannerFont, "Steal item?", x, y, Color.WHITE);
            }
            // Reset flags for next render cycle
            showTooltip = false;
        }

        // Render locators

        private static class PreRenderBlackScreenLocator extends SpireInsertLocator {
            public int[] Locate(CtBehavior ctMethodToPatch) throws CannotCompileException, PatchingException {
                Matcher matcher = new Matcher.MethodCallMatcher(CardCrawlGame.class, "renderBlackFadeScreen");
                return LineFinder.findAllInOrder(ctMethodToPatch, matcher);
            }
        }

        private static class PostRenderBlackScreenLocator extends SpireInsertLocator {
            public int[] Locate(CtBehavior ctMethodToPatch) throws CannotCompileException, PatchingException {
                Matcher matcher = new Matcher.MethodCallMatcher(CardCrawlGame.class, "renderBlackFadeScreen");
                int[] result = LineFinder.findAllInOrder(ctMethodToPatch, matcher);
                result[0]++;
                return result;
            }
        }
    }
}