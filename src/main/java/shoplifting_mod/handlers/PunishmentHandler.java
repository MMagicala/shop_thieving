package shoplifting_mod.handlers;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.*;
import com.evacipated.cardcrawl.modthespire.patcher.PatchingException;
import com.megacrit.cardcrawl.cards.CardGroup;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.monsters.MonsterGroup;
import com.megacrit.cardcrawl.monsters.exordium.GremlinNob;
import com.megacrit.cardcrawl.rewards.RewardItem;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import com.megacrit.cardcrawl.rooms.ShopRoom;
import com.megacrit.cardcrawl.screens.CombatRewardScreen;
import com.megacrit.cardcrawl.screens.select.GridCardSelectScreen;
import com.megacrit.cardcrawl.shop.Merchant;
import com.megacrit.cardcrawl.vfx.GainPennyEffect;
import javassist.CannotCompileException;
import javassist.CtBehavior;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;
import shoplifting_mod.Punishment;
import shoplifting_mod.ShopliftingMod;
import shoplifting_mod.events.GremlinFight;

import java.util.ArrayList;
import java.util.Arrays;

public class PunishmentHandler {
    public static Punishment decidedPunishment;
    public static boolean isPunishmentIssued = false;

    public static void selectRandomPunishment() {
        // Randomly pick punishment in advance
        ArrayList<Punishment> punishmentPool = new ArrayList<>(Arrays.asList(Punishment.values()));
        // Don't include lose all gold punishment if player has <100 gold
        if (AbstractDungeon.player.gold < 99) {
            punishmentPool.remove(Punishment.LOSE_ALL_GOLD);
        }
        // TODO: use streams?
        int bound = punishmentPool.size();
        int randomIndex = ShopliftingMod.random.nextInt(bound);
        decidedPunishment = punishmentPool.get(1);
    }

    public static void issuePunishment() {
        switch (decidedPunishment) {
            case LOSE_ALL_GOLD:
                Merchant merchant = ((ShopRoom) AbstractDungeon.getCurrRoom()).merchant;
                // Sfx
                CardCrawlGame.sound.play("GOLD_JINGLE");
                // Play steal gold effect
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
                isPunishmentIssued = true;
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
            case GREMLIN_FIGHT:
                // Spawn gremlin nob
                // TODO: CLEAN UP
                AbstractDungeon.getCurrRoom().phase = AbstractRoom.RoomPhase.COMBAT;
                AbstractDungeon.lastCombatMetricKey = GremlinNob.ID;
                AbstractDungeon.getCurrRoom().monsters = new MonsterGroup(new GremlinNob(250f, 0f));
                AbstractDungeon.getCurrRoom().event = new GremlinFight();
                // AbstractDungeon.getCurrRoom().rewards.clear();
                AbstractDungeon.getCurrRoom().monsters.init();
                for (AbstractMonster m : AbstractDungeon.getCurrRoom().monsters.monsters) {
                    m.usePreBattleAction();
                    m.useUniversalPreBattleAction();
                }

                AbstractRoom.waitTimer = 0.1f;
                AbstractDungeon.player.preBattlePrep();
                break;
        }
    }

    // Clear rewards when fight is over
    @SpirePatch(clz = CombatRewardScreen.class, method = "setupItemReward")
    public static class PostRewardGeneration {
        @SpireInsertPatch(locator = PostLocator.class, localvars = {"rewards"})
        public static void postRewards(CombatRewardScreen __instance, ArrayList<RewardItem> rewards) {
            rewards.clear();
            isPunishmentIssued = true;
        }

        private static class PostLocator extends SpireInsertLocator {
            public int[] Locate(CtBehavior ctMethodToPatch) throws CannotCompileException, PatchingException {
                Matcher finalMatcher = new Matcher.MethodCallMatcher(CombatRewardScreen.class, "positionRewards");
                return LineFinder.findInOrder(ctMethodToPatch, new ArrayList<>(), finalMatcher);
            }
        }
    }

    // Set punishment issued flag to true once curses are received
    @SpirePatch(
            clz = GridCardSelectScreen.class,
            method = "update"
    )
    public static class GridScreenConfirmPatch {
        @SpireInsertPatch(
                locator = CloseCurrentScreenLocator.class
        )
        public static void Insert(GridCardSelectScreen __instance) {
            isPunishmentIssued = true;
        }

        private static class CloseCurrentScreenLocator extends SpireInsertLocator {
            public int[] Locate(CtBehavior ctMethodToPatch) throws CannotCompileException, PatchingException {
                Matcher matcher = new Matcher.MethodCallMatcher(AbstractDungeon.class, "closeCurrentScreen");
                return LineFinder.findInOrder(ctMethodToPatch, matcher);
            }
        }
    }
}
