package pokecube.compat.thaumcraft;

import static pokecube.core.PokecubeItems.register;
import static pokecube.core.PokecubeItems.registerItemTexture;
import static pokecube.core.interfaces.PokecubeMod.creativeTabPokecubes;

import com.sun.jna.platform.unix.X11.XSizeHints.Aspect;

import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import pokecube.compat.CompatPokecubes;
import pokecube.core.PokecubeItems;
import pokecube.core.events.CaptureEvent.Post;
import pokecube.core.events.CaptureEvent.Pre;
import pokecube.core.interfaces.IPokecube.PokecubeBehavior;
import pokecube.core.interfaces.IPokemob;
import pokecube.core.items.pokecubes.EntityPokecube;
import pokecube.core.items.pokecubes.Pokecube;
import pokecube.core.utils.PokeType;
import pokecube.core.utils.Tools;

public class ThaumiumPokecube// extends Mod_Pokecube_Helper
{
    static boolean has(AspectList list, AspectList listoriginal)
    {
        for (Aspect a : list.getAspects())
        {
            for (Aspect b : listoriginal.getAspects())
            {
                if (a == b) return true;
            }
        }
        return false;
    }

    static int matches(IPokemob mob, AspectList list)
    {
        AspectList list1 = ThaumcraftCompat.pokeTypeToAspects.get(mob.getType1());
        AspectList list2 = ThaumcraftCompat.pokeTypeToAspects.get(mob.getType2());
        int ret = has(list1, list) ? 3 : 0;
        if (mob.getType1() != mob.getType2() && mob.getType2() != PokeType.unknown) ret += has(list2, list) ? 3 : 0;
        return ret;
    }

    public void addThaumiumPokecube()
    {
        Pokecube thaumiumpokecube = new CompatPokecubes();
        thaumiumpokecube.setUnlocalizedName("thaumiumpokecube").setCreativeTab(creativeTabPokecubes);
        register(thaumiumpokecube);
        if (FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT) registerItemTexture(thaumiumpokecube, 0,
                new ModelResourceLocation("pokecube_compat:thaumiumpokecube", "inventory"));

        PokecubeItems.addCube(98, new Object[] { thaumiumpokecube });

        PokecubeBehavior thaumic = new PokecubeBehavior()
        {
            @Override
            public void onPostCapture(Post evt)
            {

            }

            @Override
            public void onPreCapture(Pre evt)
            {
                EntityPokecube cube = (EntityPokecube) evt.pokecube;
                AspectList aspects = new AspectList();
                if (cube.getEntityItem().hasTagCompound())
                {
                    aspects.readFromNBT(cube.getEntityItem().getTagCompound(), "Aspects");
                }
                int m = matches(evt.caught, aspects);
                double rate = m;
                if (m > 0)
                {
                    cube.tilt = Tools.computeCatchRate(evt.caught, rate);
                    evt.setCanceled(true);
                }
                else
                {
                    evt.pokecube.entityDropItem(((EntityPokecube) evt.pokecube).getEntityItem(), (float) 0.5);
                }
            }
        };
        PokecubeBehavior.addCubeBehavior(98, thaumic);
    }
}
