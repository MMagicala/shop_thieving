package thieving_mod;

import thieving_mod.handlers.ShopliftingHandler;

public class Dialogue {
    public final String text;
    public final float duration;
    public final String sfxKey;

    /**
     * By default, use the grumpy sound effect (randomly picked)
     */
    public Dialogue(String text, float duration) {
        this(text, duration, "2");
    }

    /**
     * Merchant dialogue
     * @param text What the speech bubble should read
     * @param duration How long until to play the next speech bubble
     * @param string All merchant sfx keys comprise VO_MERCHANT_, a string, and A-C
     */
    public Dialogue(String text, float duration, String string) {
        this.text = text;
        this.duration = duration;

        char suffix = (char)('A' + ThievingMod.random.nextInt(3));
        this.sfxKey = "VO_MERCHANT_" + string + suffix;
    }
}
