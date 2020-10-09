package shoplifting_mod.handlers;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.*;
import com.evacipated.cardcrawl.modthespire.patcher.PatchingException;
import com.megacrit.cardcrawl.blights.AbstractBlight;
import com.megacrit.cardcrawl.cards.CardGroup;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.BlightHelper;
import com.megacrit.cardcrawl.rooms.ShopRoom;
import com.megacrit.cardcrawl.screens.select.GridCardSelectScreen;
import com.megacrit.cardcrawl.shop.Merchant;
import com.megacrit.cardcrawl.vfx.GainPennyEffect;
import javassist.CannotCompileException;
import javassist.CtBehavior;
import shoplifting_mod.Punishment;
import shoplifting_mod.ShopliftingMod;

import java.util.ArrayList;
import java.util.Arrays;

public class PunishmentHandler {
    public static Punishment decidedPunishment;
    public static boolean isPunishmentIssued = false;

    public static void selectRandomPunishment(){
        // Randomly pick punishment in advance
        ArrayList<Punishment> punishmentPool = new ArrayList<>(Arrays.asList(Punishment.values()));
        // Don't include lose all gold punishment if player has <100 gold
        if (AbstractDungeon.player.gold < 99) {
            punishmentPool.remove(Punishment.LOSE_ALL_GOLD);
        }
        // TODO: use streams?
        int bound = punishmentPool.size();
        int randomIndex = ShopliftingMod.random.nextInt(bound);
        decidedPunishment = punishmentPool.get(randomIndex);
    }

    public static void issuePunishment(){
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
            case BLIGHT:
                for(int i = 0; i < 3; i++) {
                    int index = ShopliftingMod.random.nextInt(BlightHelper.blights.size());
                    AbstractBlight blight = BlightHelper.getBlight(BlightHelper.blights.get(index));
                    AbstractDungeon.getCurrRoom().spawnBlightAndObtain(Settings.WIDTH / 2.0F, Settings.HEIGHT / 2.0F, blight);
                }
                isPunishmentIssued = true;
                break;
        }
    }

    // Set punishment issued flag to true once curses are received
    @SpirePatch(
            clz = GridCardSelectScreen.class,
            method="update"
    )
    public static class GridScreenConfirmPatch{
        @SpireInsertPatch(
                locator = CloseCurrentScreenLocator.class
        )
        public static void Insert(GridCardSelectScreen __instance){
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
