package shoplifting_mod;

public class Dialogue {
    private final float x;
    private final float y;
    private final String text;
    private final float duration;

    public Dialogue(float x, float y, String text, float duration) {
        this.x = x;
        this.y = y;
        this.text = text;
        this.duration = duration;
    }
}
