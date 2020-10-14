package thieving_mod.patches;

import com.evacipated.cardcrawl.modthespire.lib.*;
import com.evacipated.cardcrawl.modthespire.patcher.PatchingException;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.shop.Merchant;
import javassist.CannotCompileException;
import javassist.CtBehavior;
import thieving_mod.*;
import thieving_mod.handlers.CutsceneHandler;
import thieving_mod.handlers.ShopliftingHandler;

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
        if (ShopliftingHandler.isPlayerKickedOut && CutsceneHandler.isDialogueFinished()) {
            // Play custom merchant dialogue once clicked
            CutsceneHandler.enqueueMerchantDialogue(DialoguePool.FORBID);
        }
        // Don't let player enter shop if they were kicked out
        return ShopliftingHandler.isPlayerKickedOut ? SpireReturn.Return(null) : SpireReturn.Continue();
    }

    private static class MerchantClickedLocator extends SpireInsertLocator {
        public int[] Locate(CtBehavior ctMethodToPatch) throws CannotCompileException, PatchingException {
            Matcher matcher = new Matcher.FieldAccessMatcher(AbstractDungeon.class, "overlayMenu");
            return LineFinder.findInOrder(ctMethodToPatch, matcher);
        }
    }
}
