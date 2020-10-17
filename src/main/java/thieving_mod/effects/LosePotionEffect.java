package thieving_mod.effects;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.potions.AbstractPotion;
import com.megacrit.cardcrawl.vfx.ExhaustBlurEffect;
import com.megacrit.cardcrawl.vfx.ExhaustEmberEffect;

public class LosePotionEffect extends LoseItemEffect {
    private AbstractPotion potion;

    public LosePotionEffect(AbstractPotion potion, float initX, float initY, float targetX, float targetY) {
        super(initX, initY, targetX, targetY);
        this.potion = potion;
    }

    @Override
    public void render(SpriteBatch sb) {
        super.render(sb);
        // Render potion
        if(duration >= ITEM_VANISH_DURATION) {
            potion.posX = currX;
            potion.posY = currY;
            potion.render(sb);
        }
    }
}
