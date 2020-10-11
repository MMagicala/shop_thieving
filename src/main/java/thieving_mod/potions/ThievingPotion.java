package thieving_mod.potions;

import basemod.abstracts.CustomPotion;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.PowerTip;
import com.megacrit.cardcrawl.localization.PotionStrings;
import com.megacrit.cardcrawl.potions.AbstractPotion;
import thieving_mod.handlers.ShopliftingHandler;
import thieving_mod.ThievingMod;

public class ThievingPotion extends CustomPotion {
    public static final String POTION_ID = ThievingMod.makeID("ThievingPotion");

    // TODO: how does this work?
    private static final PotionStrings potionStrings = CardCrawlGame.languagePack.getPotionString(POTION_ID);
    public static final String NAME = potionStrings.NAME;
    public static final String[] DESCRIPTIONS = potionStrings.DESCRIPTIONS;

    public ThievingPotion() {
        // The bottle shape and inside is determined by potion size and color. The actual colors are the main DefaultMod.java
        super(NAME, POTION_ID, PotionRarity.RARE, PotionSize.M, PotionColor.SMOKE);

        // Do you throw this potion at an enemy or do you just consume it.
        isThrown = false;
    }

    public void initializeData(){
        potency = getPotency();
        description = DESCRIPTIONS[0] + potency + DESCRIPTIONS[1];
        tips.clear();
        tips.add(new PowerTip(this.name, this.description));
    }

    public boolean canUse() {
        // You cannot use this potion during this event. Don't game the system
        if ((AbstractDungeon.getCurrRoom()).event != null &&
                (AbstractDungeon.getCurrRoom()).event instanceof com.megacrit.cardcrawl.events.shrines.WeMeetAgain)
            return false;
        return true;
    }

    @Override
    public void use(AbstractCreature target) {
        ShopliftingHandler.successRateMultiplier *= potency;
    }

    @Override
    public AbstractPotion makeCopy() {
        return new ThievingPotion();
    }

    @Override
    public int getPotency(final int potency) {
        return 2;
    }

    /*public void upgradePotion()
    {
        potency += 1;
        tips.clear();
        tips.add(new PowerTip(name, description));
    }*/
}