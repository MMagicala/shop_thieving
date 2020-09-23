package shoplifting_mod;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.*;
import com.evacipated.cardcrawl.modthespire.patcher.PatchingException;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.rooms.ShopRoom;
import com.megacrit.cardcrawl.shop.Merchant;
import javassist.CannotCompileException;
import javassist.CtBehavior;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;
import javassist.expr.MethodCall;

@SpirePatch(
        clz = Merchant.class,
        method = "render"
)
public class MerchantRenderPatch {
    private static boolean isCodePatched = false;
    public static ExprEditor Instrument() {
        return new ExprEditor() {
            @Override
            public void edit(FieldAccess f) throws CannotCompileException {
                // Don't show hover effect if a dialogue is in progress
                if(f.getFieldName().equals("hovered") && !isCodePatched){
                    f.replace("$_ = "+MerchantRenderPatch.class.getName()+".instrumentCondition();");
                    isCodePatched = true;
                }
            }
        };
    }

    public static boolean instrumentCondition(){
        return ((ShopRoom)AbstractDungeon.getCurrRoom()).merchant.hb.hovered && ShopliftingMod.isDialogueFinished();
    }
}
