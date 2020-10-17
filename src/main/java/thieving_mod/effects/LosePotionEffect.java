package thieving_mod.effects;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.potions.AbstractPotion;

public class LosePotionEffect extends LoseItemEffect {
    private final AbstractPotion potion;

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
