package thieving_mod.effects;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import com.megacrit.cardcrawl.vfx.ExhaustBlurEffect;
import com.megacrit.cardcrawl.vfx.ExhaustEmberEffect;

public abstract class LoseItemEffect extends AbstractGameEffect {
    protected final float initX, initY, targetX, targetY;
    protected float currX, currY;
    private static final float START_DURATION = 2.0f;
    protected static final float END_DURATION = 1.15f;
    protected static final float ITEM_VANISH_DURATION = 1.0f;
    private boolean playedSmokeEffect;

    public LoseItemEffect(float initX, float initY, float targetX, float targetY) {
        this.initX = initX;
        this.initY = initY;
        this.targetX = targetX;
        this.targetY = targetY;
        currX = initX;
        currY = initY;
        duration = START_DURATION;
    }

    @Override
    public void update() {
        duration -= Gdx.graphics.getDeltaTime();
        if (duration >= END_DURATION) {
            // Move item gradually to center using sine wave function
            // Measures progress from 0 to 1 (input)
            float linearProgress = (START_DURATION - duration) / (START_DURATION - END_DURATION);
            // Sine output
            float sineProgress = 1 / 2f * (float) Math.sin(Math.PI * (linearProgress - 1 / 2f)) + 1 / 2f;
            currX = MathUtils.lerp(initX, targetX, sineProgress);
            currY = MathUtils.lerp(initY, targetY, sineProgress);
        } else {
            // Center item x and y
            currX = targetX;
            currY = targetY;
        }
        if (duration < 0) {
            isDone = true;
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        if (duration < END_DURATION && !playedSmokeEffect) {
            // Play smoke effect once item is in center
            playedSmokeEffect = true;
            CardCrawlGame.sound.play("CARD_EXHAUST", 0.2F);
            int i;
            for (i = 0; i < 90; i++)
                AbstractDungeon.topLevelEffectsQueue.add(new ExhaustBlurEffect(currX, currY));
            for (i = 0; i < 50; i++)
                AbstractDungeon.topLevelEffectsQueue.add(new ExhaustEmberEffect(currX, currY));
        }
    }

    @Override
    public void dispose() {
    }
}
