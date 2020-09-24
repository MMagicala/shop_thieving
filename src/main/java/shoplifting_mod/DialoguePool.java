package shoplifting_mod;

public enum DialoguePool {
    CAUGHT("Thief!", "Hey! No stealing!", "Do you want me to kick you out?"),
    FORBID("Don't come into my shop again!", "Screw off!");

    public String[] values;
    DialoguePool(String... values){
        this.values = values;
    }
}
