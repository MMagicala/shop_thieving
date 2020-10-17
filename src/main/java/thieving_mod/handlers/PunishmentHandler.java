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
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.cutscenes.Cutscene;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.neow.NeowEvent;
import com.megacrit.cardcrawl.helpers.BlightHelper;
import com.megacrit.cardcrawl.potions.AbstractPotion;
import com.megacrit.cardcrawl.potions.PotionSlot;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.relics.EmptyCage;
import com.megacrit.cardcrawl.rooms.ShopRoom;
import com.megacrit.cardcrawl.screens.select.GridCardSelectScreen;
import com.megacrit.cardcrawl.shop.Merchant;
import com.megacrit.cardcrawl.vfx.ExhaustBlurEffect;
import com.megacrit.cardcrawl.vfx.ExhaustEmberEffect;
import com.megacrit.cardcrawl.vfx.GainPennyEffect;
import com.megacrit.cardcrawl.vfx.cardManip.PurgeCardEffect;
import com.megacrit.cardcrawl.vfx.combat.SmokeBombEffect;
import javassist.CannotCompileException;
import javassist.CtBehavior;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;
import thieving_mod.Punishment;
import thieving_mod.ThievingMod;
import thieving_mod.effects.LosePotionEffect;
import thieving_mod.effects.LoseRelicEffect;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.*;
import java.util.stream.Collectors;

import static thieving_mod.Punishment.*;

public class PunishmentHandler {
    public static Punishment decidedPunishment;
    public static boolean isPunishmentIssued = false;
    public static boolean dontPlayCardSound = false;

    private static boolean dontInitializeNeowEvent = false;
    private static final int RELIC_STEAL_COUNT = 2;
    private static final int CARD_STEAL_COUNT = 5;
    private static final float ITEM_RENDER_X_OFFSET = 40f;
    private static final ArrayList<AbstractCard> cardsToSteal = new ArrayList<>();

    public static void selectRandomPunishment() {
        // Randomly pick punishment in advance
        ArrayList<Punishment> punishmentPool = new ArrayList<>(Arrays.asList(Punishment.values()));
        // Filter punishments
        if (AbstractDungeon.player.gold < 99) {
            punishmentPool.remove(LOSE_ALL_GOLD);
        }
        if (!AbstractDungeon.player.hasAnyPotions()) {
            punishmentPool.remove(LOSE_POTIONS);
        }
        if (AbstractDungeon.player.relics.size() < RELIC_STEAL_COUNT) {
            punishmentPool.remove(LOSE_RELICS);
        }
        // Get last five cards in deck excluding curses
        cardsToSteal.clear();
        ArrayList<AbstractCard> deck = AbstractDungeon.player.masterDeck.group;
        for (int i = 0; i < deck.size() && cardsToSteal.size() < CARD_STEAL_COUNT; i++) {
            int idx = deck.size() - 1 - i;
            if (deck.get(idx).type != AbstractCard.CardType.CURSE) {
                cardsToSteal.add(deck.get(idx));
            }
        }
        ;
        // Filter lose cards punishment if we can't burn enough cards
        if (cardsToSteal.size() < CARD_STEAL_COUNT) {
            punishmentPool.remove(LOSE_CARDS);
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
            case LOSE_CARDS:
                // Lose 5 cards
                float displayCount = 0.0F;
                for (Iterator<AbstractCard> i = cardsToSteal.iterator(); i.hasNext(); ) {
                    AbstractCard card = i.next();
                    card.untip();
                    card.unhover();
                    dontPlayCardSound = true;
                    AbstractDungeon.topLevelEffects.add(new PurgeCardEffect(card, Settings.WIDTH / 3.0F + displayCount,
                            Settings.HEIGHT / 2.0F));
                    dontPlayCardSound = false;
                    displayCount += Settings.WIDTH / 6.0F;
                    AbstractDungeon.player.masterDeck.removeCard(card);
                }
                CardCrawlGame.sound.play("CARD_BURN");
                CutsceneHandler.showProceedButton = true;
                break;
            case LOSE_POTIONS:
                // Remove all potions
                List<AbstractPotion> potionsToRemove = AbstractDungeon.player.potions.stream()
                        .filter(potion -> !(potion instanceof PotionSlot)).collect(Collectors.toList());
                for (int j = 0; j < potionsToRemove.size(); j++) {
                    // Set potion to potion slot and play fx
                    float targetX = Settings.WIDTH / 2f + (j - (potionsToRemove.size() - 1) / 2f) * ITEM_RENDER_X_OFFSET;
                    AbstractDungeon.effectsQueue.add(new LosePotionEffect(potionsToRemove.get(j),
                            potionsToRemove.get(j).posX, potionsToRemove.get(j).posY, targetX,
                            Settings.HEIGHT / 2f));

                    AbstractDungeon.player.removePotion(potionsToRemove.get(j));
                }
                CardCrawlGame.sound.play("CARD_BURN");
                CutsceneHandler.showProceedButton = true;
                break;
            case LOSE_RELICS:
                // Steal 2 randomly chosen relics
                List<AbstractRelic> randomizedRelics = new ArrayList<>(AbstractDungeon.player.relics);
                Collections.shuffle(randomizedRelics);

                List<AbstractRelic> chosenRelics = randomizedRelics.stream()
                        .limit(RELIC_STEAL_COUNT)
                        .sorted((r1, r2) -> Float.compare(r1.currentX, r2.currentX))
                        .collect(Collectors.toList());

                List<Float> chosenRelicXValues = chosenRelics.stream()
                        .map(r -> r.currentX).collect(Collectors.toList());

                for (int k = 0; k < RELIC_STEAL_COUNT; k++) {
                    float targetX = Settings.WIDTH / 2f + (k - (RELIC_STEAL_COUNT - 1) / 2f) * ITEM_RENDER_X_OFFSET;
                    AbstractDungeon.effectsQueue.add(new LoseRelicEffect(chosenRelics.get(k),
                            chosenRelicXValues.get(k), chosenRelics.get(k).currentY, targetX,
                            Settings.HEIGHT / 2f));
                    AbstractDungeon.player.loseRelic(chosenRelics.get(k).relicId);
                }
                CardCrawlGame.sound.play("CARD_BURN");
                CutsceneHandler.showProceedButton = true;
                break;
        }
        // Set flag
        isPunishmentIssued = true;
    }

    @SpirePatch(
            clz = NeowEvent.class,
            method = "uniqueBlight"
    )
    public static class UniqueBlightPatch {
        // Set punishment issued flag to true once we receive blights
        @SpirePrefixPatch
        public static void patch(NeowEvent __instance) {
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

    // Don't play card swoosh sound when it is "purged" by the shopkeeper
    @SpirePatch(
            clz = PurgeCardEffect.class,
            method = SpirePatch.CONSTRUCTOR,
            paramtypez = {AbstractCard.class, float.class, float.class}
    )
    public static class MutePurgeCardEffectPatch {
        public static ExprEditor Instrument() {
            return new ExprEditor() {
                @Override
                public void edit(MethodCall m) throws CannotCompileException {
                    if (m.getMethodName().equals("play")) {
                        m.replace("if(!"+PunishmentHandler.class.getName()+".dontPlayCardSound){" +
                                "$_ = $proceed($$);" +
                                "}");
                    }
                }
            };
        }
    }
}