package shoplifting_mod;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.cards.CardGroup;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.rooms.ShopRoom;
import com.megacrit.cardcrawl.shop.Merchant;
import com.megacrit.cardcrawl.vfx.GainPennyEffect;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class PunishmentManager {
    public static Punishment decidedPunishment;
    public static boolean isPunishmentIssued = false;

    public static void chooseRandomPunishment(){
        // Randomly pick punishment in advance
        ArrayList<Punishment> punishmentPool = new ArrayList<>(Arrays.asList(Punishment.values()));
        // Don't include lose all gold punishment if player has <100 gold
        if (AbstractDungeon.player.gold < 99) {
            punishmentPool.remove(Punishment.LOSE_ALL_GOLD);
        }
        int bound = punishmentPool.size();
        int randomIndex = ShopliftingMod.random.nextInt(bound);
        decidedPunishment = punishmentPool.get(randomIndex);
    }

    public static void issuePunishment(){
        switch (decidedPunishment) {
            case LOSE_ALL_GOLD:
                // Sfx and vfx
                CardCrawlGame.sound.play("GOLD_JINGLE");
                Merchant merchant = ((ShopRoom) AbstractDungeon.getCurrRoom()).merchant;
                float playerX = AbstractDungeon.player.hb.cX;
                float playerY = AbstractDungeon.player.hb.cY;
                DummyEntity dummyEntity = new DummyEntity();
                for (int j = 0; j < AbstractDungeon.player.gold; j++) {
                    AbstractDungeon.effectList.add(new GainPennyEffect(dummyEntity, playerX, playerY, merchant.hb.cX, merchant.hb.cY, false));
                }
                // Apply
                AbstractDungeon.player.loseGold(AbstractDungeon.player.gold);
                break;
            case CURSES:
                // Sfx and vfx
                CardCrawlGame.sound.play("BELL");
                CardGroup cardGroup = new CardGroup(CardGroup.CardGroupType.UNSPECIFIED);
                for (int j = 0; j < 2; j++) {
                    cardGroup.addToBottom(AbstractDungeon.returnRandomCurse());
                }
                // Apply
                AbstractDungeon.gridSelectScreen.openConfirmationGrid(cardGroup, "You shoplifted!");
                break;
        }
        isPunishmentIssued = true;
    }

    // This "entity" will receive the gold stolen from the player
    private static class DummyEntity extends AbstractCreature {
        @Override
        public void damage(DamageInfo damageInfo) {

        }

        @Override
        public void render(SpriteBatch spriteBatch) {

        }
    }
}
