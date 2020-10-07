package shoplifting_mod.patches;

import com.evacipated.cardcrawl.modthespire.lib.*;
import com.evacipated.cardcrawl.modthespire.patcher.PatchingException;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.shop.Merchant;
import javassist.CannotCompileException;
import javassist.CtBehavior;
import shoplifting_mod.*;
import shoplifting_mod.handlers.CutsceneHandler;
import shoplifting_mod.handlers.PunishmentHandler;
import shoplifting_mod.handlers.ShopliftingHandler;

// Merchant dialogue on click
@SpirePatch(
        clz = Merchant.class,
        method = "update"
)
public class MerchantClickedPatch {
    @SpireInsertPatch(
            locator = MerchantClickedLocator.class
    )
    public static SpireReturn<Void> Insert(Merchant __instance) {
        if (ShopliftingHandler.isKickedOut && PunishmentHandler.isPunishmentIssued && CutsceneHandler.isDialogueFinished()) {
            // Play custom merchant dialogue once clicked
            CutsceneHandler.enqueueMerchantDialogue(DialoguePool.FORBID, 5f);
        }
        // Don't let player enter shop if they were kicked out
        return ShopliftingHandler.isKickedOut ? SpireReturn.Return(null) : SpireReturn.Continue();
    }

    private static class MerchantClickedLocator extends SpireInsertLocator {
        public int[] Locate(CtBehavior ctMethodToPatch) throws CannotCompileException, PatchingException {
            Matcher matcher = new Matcher.FieldAccessMatcher(AbstractDungeon.class, "overlayMenu");
            return LineFinder.findInOrder(ctMethodToPatch, matcher);
        }
    }
}
