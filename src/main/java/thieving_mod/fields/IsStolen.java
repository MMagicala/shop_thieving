package thieving_mod.fields;

import com.evacipated.cardcrawl.modthespire.lib.SpireField;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.potions.BloodPotion;
import com.megacrit.cardcrawl.shop.StorePotion;
import com.megacrit.cardcrawl.shop.StoreRelic;

@SpirePatch(
        clz= StorePotion.class,
        method=SpirePatch.CLASS
)
@SpirePatch(
        clz= StoreRelic.class,
        method=SpirePatch.CLASS
)
@SpirePatch(
        clz= AbstractCard.class,
        method=SpirePatch.CLASS
)
public class IsStolen {
    public static SpireField<Boolean> isStolen = new SpireField<>(() -> false);
}
