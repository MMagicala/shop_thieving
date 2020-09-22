/*
MIT License

Copyright (c) 2020 MMagicala
Copyright (c) 2018 t-larson

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/
package shoplifting_mod;

import basemod.*;
import basemod.interfaces.PostInitializeSubscriber;
import basemod.interfaces.RenderSubscriber;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.*;
import com.evacipated.cardcrawl.modthespire.patcher.PatchingException;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.rooms.ShopRoom;
import com.megacrit.cardcrawl.shop.Merchant;
import com.megacrit.cardcrawl.shop.ShopScreen;
import com.megacrit.cardcrawl.shop.StorePotion;
import com.megacrit.cardcrawl.shop.StoreRelic;
import com.megacrit.cardcrawl.vfx.SpeechBubble;
import com.megacrit.cardcrawl.vfx.combat.DamageImpactBlurEffect;
import javassist.CannotCompileException;
import javassist.CtBehavior;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;
import javassist.expr.MethodCall;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

@SpireInitializer
public class ShopliftingMod implements PostInitializeSubscriber {
    private static SpireConfig config;

    // Stats
    private static final float successRate = 0.5f;
    private static final int damageAmount = 0;

    private static boolean isItemStolen = false;
    public static float prevItemX, prevItemY;
    public static boolean isKickedOut = false;

    // Mod UI
    private static final String HOTKEY_KEY = "hotkey";
    private static final float BUTTON_X = 350.0f;
    private static final float BUTTON_Y = 650.0f;
    private static final float BUTTON_LABEL_X = 475.0f;
    private static final float BUTTON_LABEL_Y = 700.0f;

    // Merchant dialogue
    private static final String[] CAUGHT_DIALOGUE = {"Thief!", "Hey! No stealing!", "Do you want me to kick you out?"};
    private static final String[] FORBID_DIALOGUE = {"Don't come into my shop again!", "Screw off!"};

    private static final Random random = new Random();

    public ShopliftingMod() {
        System.out.println("Shoplifting Mod initialized");
    }

    public static void initialize() {
        BaseMod.subscribe(new ShopliftingMod());
    }

    @Override
    public void receivePostInitialize() {
        // Define default properties
        Properties properties = new Properties();
        properties.setProperty(HOTKEY_KEY, Integer.toString(Input.Keys.CONTROL_LEFT));

        // Try to load a config file. If not found, use the default properties
        try {
            config = new SpireConfig("ShopliftingMod", "config", properties);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // create mod settings panel
        ModPanel settingsPanel = new ModPanel();

        ModLabel buttonLabel = new ModLabel("", BUTTON_LABEL_X, BUTTON_LABEL_Y, settingsPanel, (me) -> {
            if (me.parent.waitingOnEvent) {
                me.text = "Press key";
            } else {
                me.text = "Change shoplifting hotkey (" + Input.Keys.toString(config.getInt(HOTKEY_KEY)) + ")";
            }
        });
        settingsPanel.addUIElement(buttonLabel);

        ModButton hotkeyButton = new ModButton(BUTTON_X, BUTTON_Y, settingsPanel, (me) -> {
            me.parent.waitingOnEvent = true;
            InputProcessor oldInputProcessor = Gdx.input.getInputProcessor();
            Gdx.input.setInputProcessor(new InputAdapter() {
                @Override
                public boolean keyUp(int keycode) {
                    config.setInt(HOTKEY_KEY, keycode);
                    me.parent.waitingOnEvent = false;
                    Gdx.input.setInputProcessor(oldInputProcessor);
                    return true;
                }
            });
        });
        settingsPanel.addUIElement(hotkeyButton);

        // Load config
        BaseMod.registerModBadge(ImageMaster.loadImage("badge.jpg"), "Best Route Mod", "MMagicala", "Find the best route in the map!", settingsPanel);
    }

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
            return CommonInsert(__instance, hoveredCard);
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
            return CommonInsert(__instance, null);
        }
    }

    private static class ItemClickedLocator extends SpireInsertLocator {
        public int[] Locate(CtBehavior ctMethodToPatch) throws CannotCompileException, PatchingException {
            Matcher matcher = new Matcher.FieldAccessMatcher(Settings.class, "isTouchScreen");
            int[] results = LineFinder.findAllInOrder(ctMethodToPatch, matcher);
            int[] desiredResults = new int[1];
            desiredResults[0] = results[results.length-1];
            return desiredResults;
        }
    }

    /**
     * Common insert patch for each item
     * @param __instance  the item being clicked on
     * @param hoveredCard is null if the item is not a card
     * @return
     */
    private static SpireReturn<Void> CommonInsert(Object __instance, AbstractCard hoveredCard) {
        int itemPrice = -1;
        if (__instance instanceof StoreRelic) {
            itemPrice = ((StoreRelic) __instance).price;
        } else if (__instance instanceof StorePotion) {
            itemPrice = ((StorePotion) __instance).price;
        } else if (__instance instanceof ShopScreen) {
            itemPrice = hoveredCard.price;
        }
        if (AbstractDungeon.player.gold < itemPrice && Gdx.input.isKeyPressed(config.getInt(HOTKEY_KEY))) {
            // Attempt to steal the item
            float rollResult = random.nextFloat();
            if (rollResult < successRate) {
                // Success! Give the player money to purchase the item
                AbstractDungeon.player.gold += itemPrice;
                isItemStolen = true;
                if (__instance instanceof StoreRelic) {
                    ((StoreRelic) __instance).purchaseRelic();
                } else if (__instance instanceof StorePotion) {
                    ((StorePotion) __instance).purchasePotion();
                } else if (__instance instanceof ShopScreen) {
                    try {
                        Method purchaseCardMethod = ShopScreen.class.getDeclaredMethod("purchaseCard", AbstractCard.class);
                        if (!purchaseCardMethod.isAccessible()) {
                            purchaseCardMethod.setAccessible(true);
                        }
                        purchaseCardMethod.invoke(__instance, hoveredCard);
                    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                // Take damage if caught and play sound/vfx
                AbstractDungeon.player.damage(new DamageInfo(null, damageAmount, DamageInfo.DamageType.NORMAL));
                int coin = random.nextInt(2);
                String soundKey = coin == 1 ? "BLUNT_FAST" : "BLUNT_HEAVY";
                CardCrawlGame.sound.play(soundKey);
                // Kick player out of shop if they are alive
                if (!AbstractDungeon.player.isDead) {
                    AbstractDungeon.closeCurrentScreen();
                    isKickedOut = true;
                }
            }

            // Return early
            return SpireReturn.Return(null);
        }
        // Hotkey not pressed, return to normal
        return SpireReturn.Continue();
    }

    // Getter/setter methods

    public static void resetFlag() {
        isItemStolen = false;
    }

    public static boolean isItemStolen() {
        return isItemStolen;
    }

    // Prevent shopkeeper from talking after caught shoplifting
    @SpirePatch(
            clz = Merchant.class,
            method = "update"
    )
    public static class MuteMerchantDialoguePatch {
        // Freeze speech timer
        @SpireInsertPatch(
                locator = SpeechTimerUpdateLocator.class
        )
        public static void Insert1(Merchant __instance) {
            // Undo the update if freezing the timer
            if(isKickedOut){
                float speechTimer = (float)ReflectionHacks.getPrivate(__instance, Merchant.class, "speechTimer");
                speechTimer += Gdx.graphics.getDeltaTime();
                ReflectionHacks.setPrivate(__instance, Merchant.class, "speechTimer",speechTimer);
            }
        }

        private static class SpeechTimerUpdateLocator extends SpireInsertLocator {
            public int[] Locate(CtBehavior ctMethodToPatch) throws CannotCompileException, PatchingException {
                Matcher matcher = new Matcher.FieldAccessMatcher(Merchant.class, "speechTimer");
                return LineFinder.findInOrder(ctMethodToPatch, matcher);
            }
        }

        @SpireInsertPatch(
                locator = MerchantClickedLocator.class
        )
        public static SpireReturn<Void> Insert2(Merchant __instance) {
            // Play custom merchant dialogue
            playCustomMerchantDialogue(FORBID_DIALOGUE);
            // Exit the method early
            return isKickedOut ? SpireReturn.Return(null) : SpireReturn.Continue();
        }

        private static class MerchantClickedLocator extends SpireInsertLocator {
            public int[] Locate(CtBehavior ctMethodToPatch) throws CannotCompileException, PatchingException {
                Matcher matcher = new Matcher.FieldAccessMatcher(AbstractDungeon.class, "overlayMenu");
                return LineFinder.findInOrder(ctMethodToPatch, matcher);
            }
        }
    }

    private static void playCustomMerchantDialogue(String[] dialogue){
        Merchant merchant = ((ShopRoom) (AbstractDungeon.getCurrRoom())).merchant;
        int index = random.nextInt(dialogue.length);
        AbstractDungeon.effectList.add(new SpeechBubble(merchant.hb.cX - 50.0F * Settings.scale, merchant.hb.cY + 70.0F * Settings.scale, 3.0F, dialogue[index], false));
    }
}