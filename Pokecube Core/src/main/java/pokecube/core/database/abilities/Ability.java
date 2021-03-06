package pokecube.core.database.abilities;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.StatCollector;
import pokecube.core.interfaces.IPokemob;
import pokecube.core.interfaces.IPokemob.MovePacket;

public abstract class Ability
{
    /** Called for the attacked target right before damage is dealt, after other
     * calculations are done.
     * 
     * @param mob
     * @param move
     * @param damage
     * @return the actual damage dealt */
    public int beforeDamage(IPokemob mob, MovePacket move, int damage)
    {
        return damage;
    }

    /** Called when the pokemob is set to dead. */
    public void destroy()
    {
    }

    public String getName()
    {
        String translated = StatCollector.translateToLocal("ability." + toString() + ".name").trim();
        if (translated.contains(".")) { return toString(); }
        return translated;
    }

    /** Inits the Ability, if args isn't null, it will usually have the Pokemob
     * passed in as the first argument.<br>
     * If there is a second argument, it should be and integer range for the
     * expected distance the ability affects.
     * 
     * @param args
     * @return */
    public Ability init(Object... args)
    {
        return this;
    }

    /** Calls when the pokemob first agresses the target. This is called by the
     * agressor, so mob is the pokemob doing the agression. Target is the
     * agressed mob.
     * 
     * @param mob
     * @param target */
    public void onAgress(IPokemob mob, EntityLivingBase target)
    {
    }

    /** Called whenever a move is used.
     * 
     * @param mob
     * @param move */
    public void onMoveUse(IPokemob mob, MovePacket move)
    {
    }

    /** Called during the pokemob's update tick.
     * 
     * @param mob */
    public void onUpdate(IPokemob mob)
    {
    }

    @Override
    public String toString()
    {
        return AbilityManager.getNameForAbility(this);
    }
}
