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
import com.megacrit.cardcrawl.ui.panels.TopPanel;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import com.megacrit.cardcrawl.vfx.SpeechBubble;
import javassist.CannotCompileException;
import javassist.CtBehavior;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

import java.util.ArrayList;
import java.util.LinkedList;

public class CutsceneManager {
    private static final LinkedList<Dialogue> dialogueQueue = new LinkedList<>();
    private static float currentDialogueTimeLeft;

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

    private static final ArrayList<Effect> effectsToPlay = new ArrayList<>();
    private static float totalDialogueTimeElapsed;

    // effects that play during dialogue
    private static class Effect{
        // When to play the effect once dialogue starts
        private final float time;
        private final AbstractGameEffect effect;

        public Effect(AbstractGameEffect effect, float time) {
            this.effect = effect;
            this.time = time;
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
            // TODO: improve logic
            if (ShopliftingManager.isKickedOut && !PunishmentManager.isPunishmentIssued) {
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
                        m.replace("if(!" + ShopliftingManager.class.getName() + ".isKickedOut || " +
                                PunishmentManager.class.getName() + ".isPunishmentIssued){$_ = $proceed($$);}");
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
            if (ShopliftingManager.isKickedOut) {
                // Freeze the merchant's speech timer when kicked out
                float speechTimer = (float) ReflectionHacks.getPrivate(__instance, Merchant.class, "speechTimer");
                speechTimer += Gdx.graphics.getDeltaTime();
                ReflectionHacks.setPrivate(__instance, Merchant.class, "speechTimer", speechTimer);

                if (currentDialogueTimeLeft > 0) {
                    // Update our own speech timer
                    currentDialogueTimeLeft -= Gdx.graphics.getDeltaTime();
                } else {
                    if (!dialogueQueue.isEmpty()) {
                        // Once time runs out, make merchant talk
                        Dialogue dialogue = dialogueQueue.poll();
                        assert dialogue != null;
                        AbstractDungeon.effectList.add(new SpeechBubble(dialogue.x, dialogue.y, 3.0F,
                                dialogue.text, false));
                        // Reset timer
                        currentDialogueTimeLeft = dialogue.duration;
                    } else {
                        // No more dialogue. Reset dialogue time elapsed
                        totalDialogueTimeElapsed = 0;
                        if (!PunishmentManager.isPunishmentIssued) {
                            // Apply punishment if we haven't already
                            PunishmentManager.issuePunishment();
                        }
                    }
                }
                // Play enqueued effects at the right time
                if(!isDialogueFinished()){
                    totalDialogueTimeElapsed += Gdx.graphics.getDeltaTime();
                    ArrayList<Effect> immediateEffects = new ArrayList<>();
                    effectsToPlay.forEach((e) -> {
                        if(e.time >= totalDialogueTimeElapsed){
                            immediateEffects.add(e);
                        }
                    });

                    effectsToPlay.removeAll(immediateEffects);

                    immediateEffects.forEach((e) -> {
                        AbstractDungeon.topLevelEffectsQueue.add(e.effect);
                    });
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
     * Add an effect to play during dialogue
     * @param time When to play the effect since the dialogue has started
     */
    public static void addEffect(AbstractGameEffect effect, float time){
        effectsToPlay.add(new Effect(effect, time));
    }

    /**
     * Determines if shopkeeper has stopped talking or not
     */
    public static boolean isDialogueFinished() {
        return currentDialogueTimeLeft <= 0 && dialogueQueue.isEmpty();
    }

    public static void reset() {
        currentDialogueTimeLeft = 0;
        totalDialogueTimeElapsed = 0;
        // Clear queues
        dialogueQueue.clear();
        effectsToPlay.clear();
    }
}
