package thieving_mod.handlers;

import com.evacipated.cardcrawl.modthespire.lib.SpireField;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.map.MapRoomNode;

public class HideMapHandler {
    public static void hideMap(){
        int currY = AbstractDungeon.currMapNode.y;
        for(int i = currY + 2; i < AbstractDungeon.map.size(); i++){
            for(MapRoomNode node: AbstractDungeon.map.get(i)){
                if(node.hasEdges()){
                    HideNode.hide.set(node, true);
                }
            }
        }
    }

    @SpirePatch(
            clz = MapRoomNode.class,
            method = "render"
    )
    public static class HideNodePatch{
        @SpirePrefixPatch
        public static SpireReturn<Void> Prefix(MapRoomNode __instance){
            if(HideNode.hide.get(__instance)){
                return SpireReturn.Return(null);
            }
            return SpireReturn.Continue();
        }
    }

    // Add field for each map node whether to hide it
    @SpirePatch(
            clz = MapRoomNode.class,
            method = SpirePatch.CLASS
    )
    public static class HideNode{
        public static SpireField<Boolean> hide = new SpireField<>(() -> false);
    }

    // TODO: determine if punishment is applied
}
