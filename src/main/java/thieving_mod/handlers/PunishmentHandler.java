package thieving_mod.handlers;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.*;
import com.evacipated.cardcrawl.modthespire.patcher.PatchingException;
import com.megacrit.cardcrawl.blights.AbstractBlight;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.CardGroup;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.cutscenes.Cutscene;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.neow.NeowEvent;
import com.megacrit.cardcrawl.helpers.BlightHelper;
import com.megacrit.cardcrawl.potions.AbstractPotion;
import com.megacrit.cardcrawl.rooms.ShopRoom;
import com.megacrit.cardcrawl.screens.select.GridCardSelectScreen;
import com.megacrit.cardcrawl.shop.Merchant;
import com.megacrit.cardcrawl.vfx.GainPennyEffect;
import com.megacrit.cardcrawl.vfx.cardManip.PurgeCardEffect;
import javassist.CannotCompileException;
import javassist.CtBehavior;
import thieving_mod.Punishment;
import thieving_mod.ThievingMod;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import static thieving_mod.Punishment.*;

public class PunishmentHandler {
    public static Punishment decidedPunishment;
    public static boolean isPunishmentIssued = false;
    private static boolean dontInitializeNeowEvent = false;

    public static void selectRandomPunishment() {
        // Randomly pick punishment in advance
        ArrayList<Punishment> punishmentPool = new ArrayList<>(Arrays.asList(Punishment.values()));
        // TODO: use streams?
        // Filter punishments
        if (AbstractDungeon.player.gold < 99) {
            punishmentPool.remove(LOSE_ALL_GOLD);
        }
        if (AbstractDungeon.player.potions.size() == 0) {
            punishmentPool.remove(LOSE_POTION);
        }
        if (AbstractDungeon.player.relics.size() == 0) {
            punishmentPool.remove(LOSE_RELIC);
        }
        if (AbstractDungeon.player.masterDeck.size() == 0) {
            punishmentPool.remove(LOSE_CARD);
        }

        int bound = punishmentPool.size();
        int randomIndex = ThievingMod.random.nextInt(bound);
        decidedPunishment = punishmentPool.get(randomIndex);
    }

    public static void issuePunishment() {
        switch (decidedPunishment) {
            case LOSE_ALL_GOLD:
                // Sfx
                CardCrawlGame.sound.play("GOLD_JINGLE");
                // Play steal gold effect
                Merchant merchant = ((ShopRoom) AbstractDungeon.getCurrRoom()).merchant;
                float playerX = AbstractDungeon.player.hb.cX;
                float playerY = AbstractDungeon.player.hb.cY;
                AbstractCreature dummyEntity = new AbstractCreature() {
                    @Override
                    public void damage(DamageInfo damageInfo) {

                    }

                    @Override
                    public void render(SpriteBatch spriteBatch) {

                    }
                };
                for (int j = 0; j < AbstractDungeon.player.gold; j++) {
                    AbstractDungeon.effectList.add(new GainPennyEffect(dummyEntity, playerX, playerY, merchant.hb.cX, merchant.hb.cY, false));
                }
                // Steal gold
                AbstractDungeon.player.loseGold(AbstractDungeon.player.gold);
                CutsceneHandler.showProceedButton = true;
                break;
            case CURSES:
                // Sfx and vfx
                CardCrawlGame.sound.play("BELL");
                CardGroup cardGroup = new CardGroup(CardGroup.CardGroupType.UNSPECIFIED);
                for (int j = 0; j < 3; j++) {
                    cardGroup.addToBottom(AbstractDungeon.returnRandomCurse());
                }
                // Apply
                AbstractDungeon.gridSelectScreen.openConfirmationGrid(cardGroup, "You shoplifted!");
                break;
            case BLIGHT:
                try {
                    Method endlessBlight = NeowEvent.class.getDeclaredMethod("endlessBlight");
                    endlessBlight.setAccessible(true);
                    dontInitializeNeowEvent = true;
                    endlessBlight.invoke(new NeowEvent());
                    dontInitializeNeowEvent = false;
                } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
                break;
            case LOSE_CARD:
                // Lose five cards
/*
                AbstractDungeon.topLevelEffects.add(new PurgeCardEffect(card, Settings.WIDTH / 2.0F, Settings.HEIGHT / 2.0F));
                AbstractDungeon.player.masterDeck.removeCard(card);
*/
                break;
            case LOSE_POTION:
                // Remove all potions
                Iterator<AbstractPotion> itr = AbstractDungeon.player.potions.iterator();
                while (itr.hasNext()) {
                    AbstractDungeon.player.removePotion(itr.next());
                }
                break;
            case LOSE_RELIC:
                // Steal a relic
                int index = ThievingMod.random.nextInt(AbstractDungeon.player.relics.size());
                String relicId = AbstractDungeon.player.relics.get(index).relicId;
                AbstractDungeon.player.loseRelic(relicId);
                break;
        }
        // Set flag
        if(decidedPunishment != Punishment.CURSES)
        {
            isPunishmentIssued = true;
        }
        isPunishmentIssued = true;
    }


    @SpirePatch(
            clz = NeowEvent.class,
            method = "uniqueBlight"
    )
    public static class UniqueBlightPatch{
        // Set punishment issued flag to true once we receive blights
        @SpirePrefixPatch
        public static void patch(NeowEvent __instance){
            if (ShopliftingHandler.isPlayerKickedOut) {
                CutsceneHandler.showProceedButton = true;
            }
        }

        // If we get the three curses blight, dont show the proceed button yet
        @SpireInsertPatch(
                locator = GrotesqueTrophyLocator.class
        )
        public static void Insert(NeowEvent __instance) {
            if (CutsceneHandler.showProceedButton) {
                CutsceneHandler.showProceedButton = false;
            }
        }

        private static class GrotesqueTrophyLocator extends SpireInsertLocator {
            public int[] Locate(CtBehavior ctMethodToPatch) throws CannotCompileException, PatchingException {
                Matcher matcher = new Matcher.MethodCallMatcher(AbstractPlayer.class, "getBlight");
                int[] results = LineFinder.findAllInOrder(ctMethodToPatch, matcher);
                return new int[]{results[3]};
            }
        }
    }

    // Don't initialize anything in the neow event if we are just running the blight functions
    @SpirePatch(
            clz = NeowEvent.class,
            method = SpirePatch.CONSTRUCTOR,
            paramtypez = {boolean.class}
    )
    public static class NeowEventConstructorPatch {
        @SpirePrefixPatch
        public static SpireReturn<Void> patch(NeowEvent __instance, boolean isDone) {
            if (dontInitializeNeowEvent) {
                return SpireReturn.Return(null);
            }
            return SpireReturn.Continue();
        }
    }

    // Set punishment issued flag to true once curses are received from a punishment
    @SpirePatch(
            clz = GridCardSelectScreen.class,
            method = "update"
    )
    public static class GridScreenConfirmPatch {
        @SpireInsertPatch(
                locator = CloseCurrentScreenLocator.class
        )
        public static void Insert(GridCardSelectScreen __instance) {
            if (ShopliftingHandler.isPlayerKickedOut) {
                CutsceneHandler.showProceedButton = true;
            }
        }

    private static class CloseCurrentScreenLocator extends SpireInsertLocator {
        public int[] Locate(CtBehavior ctMethodToPatch) throws CannotCompileException, PatchingException {
            Matcher matcher = new Matcher.MethodCallMatcher(AbstractDungeon.class, "closeCurrentScreen");
            return LineFinder.findInOrder(ctMethodToPatch, matcher);
        }
    }
}
}
