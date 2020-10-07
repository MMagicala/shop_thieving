package shoplifting_mod.handlers;

import basemod.ReflectionHacks;
import com.badlogic.gdx.Gdx;
import com.evacipated.cardcrawl.modthespire.lib.*;
import com.evacipated.cardcrawl.modthespire.patcher.PatchingException;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.rooms.ShopRoom;
import com.megacrit.cardcrawl.shop.Merchant;
import com.megacrit.cardcrawl.ui.buttons.ProceedButton;
import com.megacrit.cardcrawl.ui.panels.TopPanel;
import com.megacrit.cardcrawl.vfx.SpeechBubble;
import javassist.CannotCompileException;
import javassist.CtBehavior;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;
import shoplifting_mod.DialoguePool;
import shoplifting_mod.Punishment;
import shoplifting_mod.ShopliftingMod;

import java.util.LinkedList;

public class CutsceneHandler {
    private static final LinkedList<Dialogue> dialogueQueue = new LinkedList<>();
    private static float currentDialogueTime;

    // Merchant dialogue

    private static class Dialogue {
        private final float x;
        private final float y;
        private final String text;
        private final float duration;

        public Dialogue(float x, float y, String text, float duration) {
            this.x = x;
            this.y = y;
            this.text = text;
            this.duration = duration;
        }
    }

    // Hide proceed button while dialogue in progress
    @SpirePatch(
            clz = ProceedButton.class,
            method = "update"
    )
    public static class DisableProceedButtonPatch {
        @SpirePrefixPatch
        public static void Prefix(ProceedButton __instance) {
            if (ShopliftingHandler.isKickedOut && !PunishmentHandler.isPunishmentIssued) {
                __instance.hide();
            }
        }
    }

    // Prevent opening map screen while dialogue in progress
    @SpirePatch(
            clz = TopPanel.class,
            method = "updateButtons"
    )
    public static class DisableMapButtonPatch {
        public static ExprEditor Instrument() {
            return new ExprEditor() {
                @Override
                public void edit(MethodCall m) throws CannotCompileException {
                    if (m.getMethodName().equals("updateMapButtonLogic")) {
                        m.replace("if(!" + ShopliftingHandler.class.getName() + ".isKickedOut || " +
                                PunishmentHandler.class.getName() + ".isPunishmentIssued){$_ = $proceed($$);}");
                    }
                }
            };
        }
    }

    // Mute merchant and play our own automatic dialogue
    @SpirePatch(
            clz = Merchant.class,
            method = "update"
    )
    public static class CustomMerchantDialoguePatch {
        @SpireInsertPatch(
                locator = SpeechTimerUpdateLocator.class
        )
        public static void Insert(Merchant __instance) {
            if (ShopliftingHandler.isKickedOut) {
                // Freeze the merchant's speech timer when kicked out
                float speechTimer = (float) ReflectionHacks.getPrivate(__instance, Merchant.class, "speechTimer");
                speechTimer += Gdx.graphics.getDeltaTime();
                ReflectionHacks.setPrivate(__instance, Merchant.class, "speechTimer", speechTimer);

                if (currentDialogueTime > 0) {
                    // Update our own speech timer
                    currentDialogueTime -= Gdx.graphics.getDeltaTime();
                } else {
                    if (!dialogueQueue.isEmpty()) {
                        // Once time runs out, make merchant talk
                        Dialogue dialogue = dialogueQueue.poll();
                        assert dialogue != null;
                        AbstractDungeon.effectList.add(new SpeechBubble(dialogue.x, dialogue.y, 3.0F,
                                dialogue.text, false));
                        // Reset timer
                        currentDialogueTime = dialogue.duration;
                    } else if (!PunishmentHandler.isPunishmentIssued) {
                        // After dialogue is finally completed, apply the punishment
                        PunishmentHandler.issuePunishment();
                    }
                }
            }
        }

        private static class SpeechTimerUpdateLocator extends SpireInsertLocator {
            public int[] Locate(CtBehavior ctMethodToPatch) throws CannotCompileException, PatchingException {
                Matcher matcher = new Matcher.FieldAccessMatcher(Merchant.class, "speechTimer");
                return LineFinder.findInOrder(ctMethodToPatch, matcher);
            }
        }
    }

    public static void enqueueMerchantDialogue(DialoguePool dialoguePool, float duration) {
        Merchant merchant = ((ShopRoom) (AbstractDungeon.getCurrRoom())).merchant;
        int index = ShopliftingMod.random.nextInt(dialoguePool.values.length);
        dialogueQueue.add(new Dialogue(merchant.hb.cX - 50.0F * Settings.scale, merchant.hb.cY + 70.0F * Settings.scale, dialoguePool.values[index], duration));
    }

    public static void enqueueMerchantDialogue(Punishment punishment, float duration) {
        Merchant merchant = ((ShopRoom) (AbstractDungeon.getCurrRoom())).merchant;
        for(String dialogue: punishment.dialogue){
            dialogueQueue.add(new Dialogue(merchant.hb.cX - 50.0F * Settings.scale, merchant.hb.cY + 70.0F * Settings.scale, dialogue, duration));
        }
    }

    /**
     * Determines if shopkeeper has stopped talking or not
     */
    public static boolean isDialogueFinished() {
        return currentDialogueTime <= 0 && dialogueQueue.isEmpty();
    }

    public static void reset() {
        currentDialogueTime = 0;
        dialogueQueue.clear();
    }
}
