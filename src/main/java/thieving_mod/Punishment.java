package thieving_mod;

public enum Punishment {
    CURSES(new Dialogue("May this curse stick with you for the rest of your journey...", 3, "K")),
    LOSE_ALL_GOLD(new Dialogue("You tried to steal from me?", 2.5f, "3"),
            new Dialogue("Now I will do the same to you...", 2.5f, "K")),
    BLIGHT(new Dialogue("Have this blight instead!", 3, "K")),
    LOSE_CARD(new Dialogue("You tried to steal from me?", 2.5f, "3"),
            new Dialogue("Now I will do the same to you...", 2.5f, "K")),
    LOSE_RELIC(new Dialogue("You tried to steal from me?", 2.5f, "3"),
            new Dialogue("Now I will do the same to you...", 2.5f, "K")),
    LOSE_POTION(new Dialogue("You tried to steal from me?", 2.5f, "3"),
            new Dialogue("Now I will do the same to you...", 2.5f, "K"));
    public Dialogue[] dialogue;

    Punishment(Dialogue... dialogue) {
        this.dialogue = dialogue;
    }
}
