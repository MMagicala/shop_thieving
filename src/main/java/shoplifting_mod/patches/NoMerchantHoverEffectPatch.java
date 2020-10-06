package shoplifting_mod.patches;

import com.evacipated.cardcrawl.modthespire.lib.*;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.rooms.ShopRoom;
import com.megacrit.cardcrawl.shop.Merchant;
import javassist.CannotCompileException;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;
import shoplifting_mod.handlers.CutsceneHandler;

@SpirePatch(
        clz = Merchant.class,
        method = "render"
)
public class NoMerchantHoverEffectPatch {
    private static boolean isCodePatched = false;
    public static ExprEditor Instrument() {
        return new ExprEditor() {
            @Override
            public void edit(FieldAccess f) throws CannotCompileException {
                // Don't show hover effect if a dialogue is in progress
                if(f.getFieldName().equals("hovered") && !isCodePatched){
                    f.replace("$_ = "+ NoMerchantHoverEffectPatch.class.getName()+".instrumentCondition();");
                    isCodePatched = true;
                }
            }
        };
    }

    public static boolean instrumentCondition(){
        return ((ShopRoom)AbstractDungeon.getCurrRoom()).merchant.hb.hovered && CutsceneHandler.isDialogueFinished();
    }
}
