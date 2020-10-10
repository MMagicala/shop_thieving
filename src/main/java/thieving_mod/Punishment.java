package thieving_mod;

public enum Punishment {
    CURSES("May this curse stick with you for the rest of your journey..."),
    LOSE_ALL_GOLD("Give me all your gold!"),
    BLIGHT("Have this blight instead!"),
    LOSE_CARD("You tried to steal from me?",
            "I'll steal something from you too!"),
    LOSE_RELIC("You tried to steal from me?",
            "I'll steal something from you too!"),
    LOSE_POTION("You tried to steal from me?",
            "I'll steal something from you too!");
    public String[] dialogue;

    Punishment(String... dialogue) {
        this.dialogue = dialogue;
    }
}
