package shoplifting_mod.patches;

import com.badlogic.gdx.graphics.Color;
import com.evacipated.cardcrawl.modthespire.lib.*;
import com.evacipated.cardcrawl.modthespire.patcher.PatchingException;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.shop.ShopScreen;
import com.megacrit.cardcrawl.shop.StorePotion;
import com.megacrit.cardcrawl.shop.StoreRelic;
import com.megacrit.cardcrawl.vfx.DarkSmokePuffEffect;
import com.megacrit.cardcrawl.vfx.TextAboveCreatureEffect;
import com.megacrit.cardcrawl.vfx.combat.SmokeBombEffect;
import javassist.CannotCompileException;
import javassist.CtBehavior;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;
import javassist.expr.MethodCall;
import shoplifting_mod.handlers.ShopliftingHandler;

import java.util.ArrayList;

// Patch purchase item methods
public class ItemStolenEffectsPatch {
    @SpirePatch(
            clz = ShopScreen.class,
            method = "purchaseCard"
    )
    @SpirePatch(
            clz = StorePotion.class,
            method = "purchasePotion"
    )
    @SpirePatch(
            clz = StoreRelic.class,
            method = "purchaseRelic"
    )
    public static class MainPatch {
        private static final ArrayList<String> METHOD_NAMES = new ArrayList<String>() {
            {
                add("play");
                add("addShopPurchaseData");
                add("playBuySfx");
                add("createSpeech");
            }
        };
        private static boolean isCreateSpeechMethodPatched = false;
        private static String savedFileName = null;

        public static ExprEditor Instrument() {
            return new ExprEditor() {
                @Override
                public void edit(MethodCall m) throws CannotCompileException {
                    String currentFileName = m.getFileName();
                    // Determine if patching a new class
                    if (savedFileName == null || !savedFileName.equals(m.getFileName())) {
                        savedFileName = currentFileName;
                        // Reset flag for this class
                        isCreateSpeechMethodPatched = false;
                    }
                    String methodName = m.getMethodName();
                    // Only patch the methods we want
                    if (METHOD_NAMES.contains(methodName) && !isCreateSpeechMethodPatched) {
                        m.replace(getExpr(currentFileName, methodName));
                        if (methodName.equals("createSpeech")) {
                            // Don't patch any next createSpeech method calls
                            isCreateSpeechMethodPatched = true;
                        }
                    }
                }
            };
        }

        /**
         * Only play purchase item method's original code if we aren't stealing it
         *
         * @param fileName Identifies the type of item
         * @return
         */
        private static String getExpr(String fileName, String methodName) {
            StringBuilder sb = new StringBuilder();
            sb.append("if(!");
            sb.append(ShopliftingHandler.class.getName());
            sb.append(".isItemSuccessfullyStolen){ $_ = $proceed($$);");
            if (methodName.equals("createSpeech")) {
                sb.append("}else{");
                // Play gold jingle and smoke sound
                sb.append(CardCrawlGame.class.getName());
                sb.append(".sound.play(\"GOLD_JINGLE\");");
                // Play "Stolen card" text effect
                addPlayEffectExpr(sb, TextAboveCreatureEffect.class);
                // Play smoke vfx
                addPlayEffectExpr(sb, SmokeBombEffect.class);
            } else if (methodName.equals("addShopPurchaseData")) {
                // Save x and y for smoke fx later
                sb.append("}else{");
                // Determine class of item to get the right field
                if (fileName.equals("StoreRelic.java")) {
                    addSaveCoordinatesExpr(sb, "relic.current", true);
                } else if (fileName.equals("ShopScreen.java")) {
                    addSaveCoordinatesExpr(sb, "hoveredCard.current_", false);
                }
            }
            sb.append("}");
            return sb.toString();
        }

        /**
         * Plays an effect at the coordinates where item was stolen
         *
         * @param effectClass the type of effect being played
         */
        private static void addPlayEffectExpr(StringBuilder sb, Class<?> effectClass) {
            sb.append(AbstractDungeon.class.getName());
            sb.append(".topLevelEffectsQueue.add(new ");
            sb.append(effectClass.getName());
            sb.append("(");
            sb.append(ShopliftingHandler.class.getName());
            sb.append(".prevItemX, ");
            sb.append(ShopliftingHandler.class.getName());
            sb.append(".prevItemY");
            if (effectClass == TextAboveCreatureEffect.class) {
                sb.append(",\"Item stolen!\", ");
                sb.append(Color.class.getName());
                sb.append(".WHITE");
            }
            sb.append("));");
        }

        /**
         * Generates code expression to save the coordinates of an item
         *
         * @param propertyPrefix identifies the item's property name
         * @param isUppercase    determines case for x and y in property name. If false, lowercase
         */
        private static void addSaveCoordinatesExpr(StringBuilder sb, String propertyPrefix, boolean isUppercase) {
            char uppercaseCoordinate = 'X';
            char coordinate = isUppercase ? 'X' : 'x';
            for (int i = 0; i < 2; i++) {
                sb.append(ShopliftingHandler.class.getName());
                sb.append(".prevItem");
                sb.append(uppercaseCoordinate);
                sb.append(" = ");
                sb.append(propertyPrefix);
                sb.append(coordinate);
                sb.append(';');
                coordinate++;
                uppercaseCoordinate++;
            }
        }
    }

    // Get potion x and y before it is moved
    @SpirePatch(
            clz = StorePotion.class,
            method = "purchasePotion"
    )
    public static class SavePotionCoordinatePatch {
        private static class PotionBeforeMoveLocator extends SpireInsertLocator {
            public int[] Locate(CtBehavior ctMethodToPatch) throws CannotCompileException, PatchingException {
                Matcher matcher = new Matcher.MethodCallMatcher(AbstractPlayer.class, "obtainPotion");
                return LineFinder.findInOrder(ctMethodToPatch, matcher);
            }
        }

        @SpireInsertPatch(
                locator = PotionBeforeMoveLocator.class
        )
        public static void Insert(StorePotion __instance) {
            ShopliftingHandler.prevItemX = __instance.potion.posX;
            ShopliftingHandler.prevItemY = __instance.potion.posY;
        }
    }

    @SpirePatch(
            clz = SmokeBombEffect.class,
            method = "update"
    )
    public static class SmokeBombEffectRenderPatch {
        public static ExprEditor Instrument() {
            return new ExprEditor() {
                @Override
                public void edit(MethodCall m) throws CannotCompileException {
                    if (m.getMethodName().equals("add")) {
                        m.replace("if(" + ShopliftingHandler.class.getName() + ".isItemSuccessfullyStolen){" +
                                "$_ = " + AbstractDungeon.class.getName() + ".topLevelEffectsQueue.add($$);" +
                                "}else{" +
                                "$_ = $proceed($$);" +
                                "}");
                    }
                }
            };
        }

        @SpirePostfixPatch
        public static void Postfix() {
            if (ShopliftingHandler.isItemSuccessfullyStolen) {
                ShopliftingHandler.isItemSuccessfullyStolen = false;
            }
        }
    }
}