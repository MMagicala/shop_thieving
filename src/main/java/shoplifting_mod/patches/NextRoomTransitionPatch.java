package shoplifting_mod.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.saveAndContinue.SaveFile;
import shoplifting_mod.CutsceneManager;
import shoplifting_mod.PunishmentManager;
import shoplifting_mod.ShopliftingManager;
import shoplifting_mod.ShopliftingMod;

// Reset everything once we enter a new room
@SpirePatch(
        clz = AbstractDungeon.class,
        method = "nextRoomTransition",
        paramtypez = {SaveFile.class}
)
public class NextRoomTransitionPatch {
    @SpirePostfixPatch
    public static void Postfix(AbstractDungeon __instance, SaveFile saveFile) {
        if (ShopliftingManager.isKickedOut) {
            ShopliftingManager.isKickedOut = false;
        }
        if (PunishmentManager.isPunishmentIssued) {
            PunishmentManager.isPunishmentIssued = false;
        }
        CutsceneManager.reset();
    }
}
