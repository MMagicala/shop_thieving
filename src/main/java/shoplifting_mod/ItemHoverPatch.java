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

import javax.smartcardio.Card;

public class ItemHoverPatch {
    private static boolean showTooltip = false;
    private static float savedScreenColorAlpha;
    // Show tooltip above a hovered item if hotkey is pressed

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
            int itemPrice = ShopliftingMod.getItemPrice(__instance, hoveredCard);
            if (AbstractDungeon.player.gold < itemPrice) {
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

    @SpirePatch(clz = CardCrawlGame.class, method = "render")
    public static class RenderToolTipTextPatch {
        private static final float FADE_DURATION = 0.5f;
        @SpireInsertPatch(
                locator = PreRenderBlackScreenLocator.class
        )
        public static void InsertBeforeBlackFadeScreen(CardCrawlGame __instance) {
            Color screenColor = (Color) ReflectionHacks.getPrivate(__instance, CardCrawlGame.class, "screenColor");
            if (showTooltip) {
                savedScreenColorAlpha = screenColor.a;
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

        @SpireInsertPatch(
                locator = PostRenderBlackScreenLocator.class
        )
        public static void InsertAfterBlackFadeScreen(CardCrawlGame __instance) {
            if (showTooltip) {
                float x = InputHelper.mX;
                float y = InputHelper.mY - 64;
                SpriteBatch sb = (SpriteBatch) ReflectionHacks.getPrivate(__instance, CardCrawlGame.class, "sb");
                FontHelper.renderFontLeft(sb, FontHelper.bannerFont, "Steal item?", x, y, Color.WHITE);
            }
            // Restore defaults
            showTooltip = false;
        }

        // Locators

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