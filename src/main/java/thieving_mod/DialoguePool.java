package thieving_mod;

public enum DialoguePool {
    CAUGHT("Thief!", "Hey! No stealing!", "I caught you!"),
    FORBID("Don't come into my shop again!", "Screw off!");

    public String[] values;
    DialoguePool(String... values){
        this.values = values;
    }
}
