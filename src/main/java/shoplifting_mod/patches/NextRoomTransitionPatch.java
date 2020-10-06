package shoplifting_mod.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.saveAndContinue.SaveFile;
import shoplifting_mod.handlers.CutsceneHandler;
import shoplifting_mod.handlers.PunishmentHandler;
import shoplifting_mod.handlers.ShopliftingHandler;

// Reset everything once we enter a new room
@SpirePatch(
        clz = AbstractDungeon.class,
        method = "nextRoomTransition",
        paramtypez = {SaveFile.class}
)
public class NextRoomTransitionPatch {
    @SpirePostfixPatch
    public static void Postfix(AbstractDungeon __instance, SaveFile saveFile) {
        // Reset flags
        if (ShopliftingHandler.isKickedOut) {
            ShopliftingHandler.isKickedOut = false;
        }
        if (PunishmentHandler.isPunishmentIssued) {
            PunishmentHandler.isPunishmentIssued = false;
        }
        CutsceneHandler.reset();

        // Reset stats
        ShopliftingHandler.successRateMultiplier = 1;
    }
}
