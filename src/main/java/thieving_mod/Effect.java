package thieving_mod;

import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class Effect {
    public Entity entity;
    public Class<? extends AbstractGameEffect> effect;

    public Effect(Entity entity, Class<? extends AbstractGameEffect> effect) {
        this.effect = effect;
        this.entity = entity;
    }
}
