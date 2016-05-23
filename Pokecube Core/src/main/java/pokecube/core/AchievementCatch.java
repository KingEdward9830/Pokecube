/**
 *
 */
package pokecube.core;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.stats.Achievement;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraft.util.text.translation.I18n;
import pokecube.core.database.Pokedex;

/** @author Manchou */
public class AchievementCatch extends Achievement
{
    int pokedexNb;

    public AchievementCatch(int par1, String par2Str, int par3, int par4, Block block, Achievement par6Achievement)
    {
        super(par2Str, par2Str, par3, par4, block, par6Achievement);// super(2000+par1,
                                                                    // par2Str,
                                                                    // par3,
                                                                    // par4,
                                                                    // block,
                                                                    // par6Achievement);
        pokedexNb = par1;
    }

    public AchievementCatch(int par1, String par2Str, int par3, int par4, Item item, Achievement par6Achievement)
    {
        super(par2Str, par2Str, par3, par4, item, par6Achievement);
        pokedexNb = par1;
    }

    @Override
    public String getDescription()
    {
        if ("get1stPokemob".equals(statId)) { return I18n.translateToLocal("achievement." + statId + ".desc"); }
        return I18n.translateToLocalFormatted("achievement.catch", getPokemobTranslatedName());
    }

    protected String getPokemobTranslatedName()
    {
        if (pokedexNb > 0 && Pokedex.getInstance().getEntry(pokedexNb) != null)
        {
            return Pokedex.getInstance().getEntry(pokedexNb).getTranslatedName();
        }
        else
        {
            System.out.println("shouldn't happen");
            return "AchievementCatch"; // should not happen
        }
    }

    @Override
    public ITextComponent getStatName()
    {
        if ("get1stPokemob".equals(statId)) { return super.getStatName(); }
        ITextComponent iTextComponent = new TextComponentTranslation(statId, new Object[0]);
        iTextComponent.getStyle().setColor(TextFormatting.GRAY);
        iTextComponent.getStyle().setHoverEvent(
                new HoverEvent(HoverEvent.Action.SHOW_ACHIEVEMENT, new TextComponentString(this.statId)));
        return iTextComponent;
    }

    @Override
    public String toString()
    {
        if ("get1stPokemob".equals(statId)) { return statId; }
        return getPokemobTranslatedName();
    }
}
