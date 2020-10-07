package shoplifting_mod.events;

import com.megacrit.cardcrawl.events.AbstractEvent;

public class GremlinFight extends AbstractEvent
{
    public GremlinFight()
    {
        noCardsInRewards = true;
        enterCombat();
    }

    @Override
    protected void buttonEffect(int i)
    {

    }
}