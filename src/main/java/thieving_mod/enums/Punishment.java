package thieving_mod.enums;

import com.megacrit.cardcrawl.vfx.combat.FlameBarrierEffect;
import thieving_mod.Dialogue;
import thieving_mod.Effect;

public enum Punishment {
    CURSES(new Dialogue("May this curse stick with you for the rest of your journey...", 3, "K",
            new Effect(Entity.MERCHANT, FlameBarrierEffect.class))),
    LOSE_ALL_GOLD(new Dialogue("You tried to steal from me?", 2.5f, "3"),
            new Dialogue("Now I will do the same to you...", 2.5f, "K")),
    BLIGHT(new Dialogue("Have this blight instead!", 3, "K",
            new Effect(Entity.MERCHANT, FlameBarrierEffect.class))),
    LOSE_CARDS(true, new Dialogue("You tried to steal from me?", 2.5f, "3"),
            new Dialogue("Now I will do the same to you...", 2.5f, "K")),
    LOSE_RELICS(true, new Dialogue("You tried to steal from me?", 2.5f, "3"),
            new Dialogue("Now I will do the same to you...", 2.5f, "K")),
    LOSE_POTIONS(true, new Dialogue("You tried to steal from me?", 2.5f, "3"),
            new Dialogue("Now I will do the same to you...", 2.5f, "K")),
    MAP_FOG(new Dialogue("Time to blind you!", 2f, "K"));

    public Dialogue[] dialogue;
    public boolean loseItem;

    Punishment(boolean loseItem, Dialogue... dialogue) {
        this.loseItem = loseItem;
        this.dialogue = dialogue;
    }

    Punishment(Dialogue... dialogue) {
        this(false, dialogue);
    }
}
