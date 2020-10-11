package thieving_mod;

public enum DialoguePool {
    CAUGHT(new Dialogue("Thief!", 3), new Dialogue("Hey! No stealing!", 3), new Dialogue("I caught you!", 3)),
    FORBID(new Dialogue("Don't come into my shop again!", 5), new Dialogue("Screw off!", 5));

    public Dialogue[] values;

    DialoguePool(Dialogue... values) {
        this.values = values;
    }
}
