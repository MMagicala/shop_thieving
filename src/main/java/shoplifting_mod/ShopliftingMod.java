/*
MIT License

Copyright (c) 2020 MMagicala
Copyright (c) 2018 t-larson

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/
package shoplifting_mod;

import basemod.*;
import basemod.interfaces.EditStringsSubscriber;
import basemod.interfaces.PostBattleSubscriber;
import basemod.interfaces.PostInitializeSubscriber;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Color;
import com.evacipated.cardcrawl.modthespire.lib.*;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.CardHelper;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.localization.PotionStrings;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import shoplifting_mod.handlers.ShopliftingHandler;
import shoplifting_mod.potions.ThievingPotion;

import java.io.IOException;
import java.util.*;

@SpireInitializer
public class ShopliftingMod implements PostInitializeSubscriber, EditStringsSubscriber {
    // Mod data
    private static SpireConfig config;
    private static final String modID = "ShopliftingMod";
    private static final String HOTKEY_KEY = "hotkey";

    // TODO: add seed
    public static final Random random = new Random();

    // Mod UI
    private static final float BUTTON_X = 350.0f;
    private static final float BUTTON_Y = 650.0f;
    private static final float BUTTON_LABEL_X = 475.0f;
    private static final float BUTTON_LABEL_Y = 700.0f;

    // Custom potion
    public static final Color THIEVING_POTION_LIQUID = CardHelper.getColor(101, 37, 165); // Dark purple
    public static final Color THIEVING_POTION_HYBRID = CardHelper.getColor(101, 37, 165); // Dark purple
    public static final Color THIEVING_POTION_SPOTS = CardHelper.getColor(101, 37, 165); // Dark purple

    public ShopliftingMod() {
        System.out.println("Shoplifting Mod initialized");
    }

    public static void initialize() {
        BaseMod.subscribe(new ShopliftingMod());
    }

    public static boolean isConfigKeyPressed() {
        return Gdx.input.isKeyPressed(config.getInt(HOTKEY_KEY));
    }

    @Override
    public void receivePostInitialize() {
        // Define default properties
        Properties properties = new Properties();
        properties.setProperty(HOTKEY_KEY, Integer.toString(Input.Keys.CONTROL_LEFT));

        // Try to load a config file. If not found, use the default properties
        try {
            config = new SpireConfig("ShopliftingMod", "config", properties);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // create mod settings panel
        ModPanel settingsPanel = new ModPanel();

        ModLabel buttonLabel = new ModLabel("", BUTTON_LABEL_X, BUTTON_LABEL_Y, settingsPanel, (me) -> {
            if (me.parent.waitingOnEvent) {
                me.text = "Press key";
            } else {
                me.text = "Change shoplifting hotkey (" + Input.Keys.toString(config.getInt(HOTKEY_KEY)) + ")";
            }
        });
        settingsPanel.addUIElement(buttonLabel);

        ModButton hotkeyButton = new ModButton(BUTTON_X, BUTTON_Y, settingsPanel, (me) -> {
            me.parent.waitingOnEvent = true;
            InputProcessor oldInputProcessor = Gdx.input.getInputProcessor();
            Gdx.input.setInputProcessor(new InputAdapter() {
                @Override
                public boolean keyUp(int keycode) {
                    config.setInt(HOTKEY_KEY, keycode);
                    me.parent.waitingOnEvent = false;
                    Gdx.input.setInputProcessor(oldInputProcessor);
                    return true;
                }
            });
        });
        settingsPanel.addUIElement(hotkeyButton);

        // Add thieving potion
        BaseMod.addPotion(ThievingPotion.class, THIEVING_POTION_LIQUID, THIEVING_POTION_HYBRID,
                THIEVING_POTION_SPOTS, ThievingPotion.POTION_ID);

        // Load config
        BaseMod.registerModBadge(ImageMaster.loadImage("badge.jpg"), "Best Route Mod", "MMagicala", "Find the best route in the map!", settingsPanel);
    }

    @Override
    public void receiveEditStrings() {
        // PotionStrings
        BaseMod.loadCustomStringsFile(PotionStrings.class,
                modID + "Resources/localization/eng/ShopliftingMod-Potion-Strings.json");
    }

    public static String makeID(String idText) {
        return modID + ":" + idText;
    }

/*
    // Don't show any rewards after a gremlin fight
    @Override
    public void receivePostBattle(AbstractRoom abstractRoom) {
        if(ShopliftingHandler.isKickedOut){
            AbstractDungeon.getCurrRoom().rewards.clear();
        }
    }
*/
}