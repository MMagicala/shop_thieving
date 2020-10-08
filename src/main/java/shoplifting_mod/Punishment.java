package shoplifting_mod;

public enum Punishment {
    CURSES("May this curse stick with you for the rest of your journey..."),
    LOSE_ALL_GOLD("You tried to steal from me. Now I will do the same to you..."),
    BLIGHT("Have this blight instead!");

    public String[] dialogue;

    Punishment(String... dialogue) {
        this.dialogue = dialogue;
    }
}
