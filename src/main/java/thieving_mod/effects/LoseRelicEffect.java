package thieving_mod.effects;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.relics.AbstractRelic;

public class LoseRelicEffect extends LoseItemEffect {
    private final AbstractRelic relic;

    public LoseRelicEffect(AbstractRelic relic, float initX, float initY, float targetX, float targetY) {
        super(initX, initY, targetX, targetY);
        this.relic = relic;
    }

    @Override
    public void render(SpriteBatch sb) {
        super.render(sb);
        // Render potion
        if(duration >= ITEM_VANISH_DURATION) {
            relic.currentX = currX;
            relic.currentY = currY;
            relic.render(sb);
        }
    }
}
