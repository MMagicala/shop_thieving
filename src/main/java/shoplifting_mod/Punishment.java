package shoplifting_mod;

public enum Punishment {
    CURSES("May this curse stick with you for the rest of your journey..."),
    LOSE_ALL_GOLD("You tried to steal from me. Now I will do the same to you...");

    public String[] dialoguePool;

    Punishment(String... dialoguePool) {
        this.dialoguePool = dialoguePool;
    }
}
