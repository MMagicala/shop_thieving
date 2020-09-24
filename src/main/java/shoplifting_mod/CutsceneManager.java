package shoplifting_mod;

import basemod.ReflectionHacks;
import com.badlogic.gdx.Gdx;
import com.evacipated.cardcrawl.modthespire.lib.*;
import com.evacipated.cardcrawl.modthespire.patcher.PatchingException;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.rooms.ShopRoom;
import com.megacrit.cardcrawl.shop.Merchant;
import com.megacrit.cardcrawl.ui.buttons.ProceedButton;
import com.megacrit.cardcrawl.vfx.SpeechBubble;
import javassist.CannotCompileException;
import javassist.CtBehavior;

import java.util.LinkedList;

public class CutsceneManager {
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
        public static void Prefix(ProceedButton __instance){
            if(!isDialogueFinished() && !PunishmentManager.isPunishmentIssued){
                __instance.hide();
            }
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
            if (ShopliftingManager.isKickedOut) {
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
                    } else if (!PunishmentManager.isPunishmentIssued) {
                        // After dialogue is finally completed, apply the punishment
                        PunishmentManager.issuePunishment();
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

    public static void enqueueMerchantDialogue(String[] dialoguePool, float duration) {
        Merchant merchant = ((ShopRoom) (AbstractDungeon.getCurrRoom())).merchant;
        int index = ShopliftingMod.random.nextInt(dialoguePool.length);
        dialogueQueue.add(new Dialogue(merchant.hb.cX - 50.0F * Settings.scale, merchant.hb.cY + 70.0F * Settings.scale, dialoguePool[index], duration));
    }

    /**
     * Determines if shopkeeper has stopped talking or not
     */
    public static boolean isDialogueFinished(){
        return currentDialogueTime <= 0 && dialogueQueue.isEmpty();
    }

    public static void reset(){
        currentDialogueTime = 0;
        dialogueQueue.clear();
    }
}
