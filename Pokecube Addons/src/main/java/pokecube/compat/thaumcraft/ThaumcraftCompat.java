package pokecube.compat.thaumcraft;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.Lists;
import com.sun.jna.platform.unix.X11.XSizeHints.Aspect;

import net.minecraft.entity.EntityList;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import pokecube.adventures.entity.trainers.EntityTrainer;
import pokecube.core.PokecubeItems;
import pokecube.core.database.Pokedex;
import pokecube.core.database.PokedexEntry;
import pokecube.core.events.PostPostInit;
import pokecube.core.interfaces.PokecubeMod;
import pokecube.core.utils.PokeType;

public class ThaumcraftCompat
{

    protected static Map<PokeType, AspectList> pokeTypeToAspects = new HashMap<PokeType, AspectList>();

    public static void addLoot()
    {
        ItemStack pokecube = PokecubeItems.getStack("pokecube");
        ItemStack greatcube = PokecubeItems.getStack("greatcube");
        ItemStack ultracube = PokecubeItems.getStack("ultracube");
        ItemStack mastercube = PokecubeItems.getStack("mastercube");
        ItemStack expshare = PokecubeItems.getStack("exp_share");

        pokecube.stackSize = 4;
        greatcube.stackSize = 2;

        ThaumcraftApi.addLootBagItem(pokecube, 50, 0);
        ThaumcraftApi.addLootBagItem(greatcube, 10, 0);
        ThaumcraftApi.addLootBagItem(ultracube, 5, 0);

        ThaumcraftApi.addLootBagItem(pokecube, 500, 1);
        ThaumcraftApi.addLootBagItem(greatcube, 100, 1);
        ThaumcraftApi.addLootBagItem(ultracube, 50, 1);

        ThaumcraftApi.addLootBagItem(ultracube, 200, 2);
        ThaumcraftApi.addLootBagItem(mastercube, 20, 2);
        ThaumcraftApi.addLootBagItem(expshare, 100, 2);

        for (ItemStack stack : PokecubeMod.HMs)
        {
            ThaumcraftApi.addLootBagItem(stack.copy(), 10, 0);
        }
    }

    private static void addMapping(PokeType type, AspectList aspects)
    {
        if (pokeTypeToAspects
                .containsKey(type)) { throw new IllegalArgumentException("PokeType aspects already registered"); }
        pokeTypeToAspects.put(type, aspects);
    }

    public static void addPage()
    {
        ResearchCategories.registerCategory("POKECUBE", null,
                new ResourceLocation("pokecube", "textures/items/thaumiumpokecubefront.png"),
                new ResourceLocation("pokecube", "textures/items/pokedex.png"));
    }

    public static void addResearch()
    {
        ArrayList<ResearchPage> pagelist = Lists.newArrayList();
        pagelist.add(new ResearchPage("tc.research_page.THAUMIUMPOKECUBE.1"));
        pagelist.add(new ResearchPage(RecipeThaumiumPokecube()));
        ArrayList<InfusionRecipe> recipes = Lists.newArrayList();
        for (PokeType type : PokeType.values())
        {
            recipes.add(InfusedThaumiumPokecube(type));
        }
        pagelist.add(new ResearchPage(recipes.toArray(new InfusionRecipe[0])));
        new ResearchItem("THAUMIUMPOKECUBE", "POKECUBE",
                (new AspectList()).add(Aspect.METAL, 3).add(Aspect.AURA, 2).add(Aspect.TRAP, 2), 0, 3, 2,
                new ItemStack(PokecubeItems.getEmptyCube(98))).setPages(pagelist.toArray(new ResearchPage[0]))
                        .registerResearchItem();
    }

    static Aspect fromType(PokeType type)
    {
        AspectList list = pokeTypeToAspects.get(type);
        return list.getAspects()[0];
    }

    static Object infuse(Aspect aspect)
    {
        ItemStack ret = new ItemStack(PokecubeItems.getEmptyCube(98));
        ret.setTagCompound(new NBTTagCompound());
        AspectList list = new AspectList();
        list.add(aspect, 3);
        list.writeToNBT(ret.getTagCompound(), "Aspects");
        return ret;
    }

    static InfusionRecipe InfusedThaumiumPokecube(PokeType type)
    {
        Aspect aspect = fromType(type);
        String item = "crystal_essence";
        ItemStack stack = PokecubeItems.getStack(item);
        stack.setTagCompound(new NBTTagCompound());
        AspectList list = new AspectList();
        list.add(aspect, 1);
        list.writeToNBT(stack.getTagCompound(), "Aspects");
        return ThaumcraftApi.addInfusionCraftingRecipe("THAUMIUMPOKECUBE", infuse(aspect), 1,
                (new AspectList()).add(Aspect.TRAP, 16).add(aspect, 16), new ItemStack(PokecubeItems.getEmptyCube(98)),
                new ItemStack[] { stack.copy(), stack.copy(), stack });
    }

    static void init()
    {
        addMapping(PokeType.unknown, new AspectList().add(Aspect.ELDRITCH, 2));
        addMapping(PokeType.normal, new AspectList().add(Aspect.ORDER, 2));

        addMapping(PokeType.fighting, new AspectList().add(Aspect.BEAST, 2));

        addMapping(PokeType.flying, new AspectList().add(Aspect.FLIGHT, 2));

        addMapping(PokeType.poison, new AspectList().add(Aspect.TRAP, 2));

        addMapping(PokeType.ground, new AspectList().add(Aspect.EARTH, 2));

        addMapping(PokeType.rock, new AspectList().add(Aspect.CRYSTAL, 2));
        addMapping(PokeType.bug, new AspectList().add(Aspect.AVERSION, 2));

        addMapping(PokeType.ghost, new AspectList().add(Aspect.SOUL, 2));
        addMapping(PokeType.steel, new AspectList().add(Aspect.METAL, 2));
        addMapping(PokeType.fire, new AspectList().add(Aspect.FIRE, 2));
        addMapping(PokeType.water, new AspectList().add(Aspect.WATER, 2));
        addMapping(PokeType.grass, new AspectList().add(Aspect.PLANT, 2));
        addMapping(PokeType.electric, new AspectList().add(Aspect.ENERGY, 2));
        addMapping(PokeType.psychic, new AspectList().add(Aspect.MIND, 2));
        addMapping(PokeType.ice, new AspectList().add(Aspect.COLD, 2));

        addMapping(PokeType.dragon, new AspectList().add(Aspect.PROTECT, 2));

        addMapping(PokeType.dark, new AspectList().add(Aspect.DARKNESS, 2));
        addMapping(PokeType.fairy, new AspectList().add(Aspect.AURA, 2));
    }

    static InfusionRecipe RecipeThaumiumPokecube()
    {
        ItemStack stack = new ItemStack(thaumcraft.api.items.ItemsTC.ingots);
        return ThaumcraftApi.addInfusionCraftingRecipe("THAUMIUMPOKECUBE",
                new ItemStack(PokecubeItems.getEmptyCube(98)), 1,
                (new AspectList()).add(Aspect.METAL, 8).add(Aspect.TRAP, 8).add(Aspect.AURA, 4),
                new ItemStack(PokecubeItems.getEmptyCube(0)), new ItemStack[] { stack.copy(), stack.copy(), stack });
    }

    public AspectList getAspects(PokedexEntry entry)
    {
        PokeType type1 = entry.getType1();
        PokeType type2 = entry.getType2();

        AspectList aspects = new AspectList();

        aspects.add(Aspect.BEAST, 2);

        if (type2 != PokeType.unknown)
        {
            aspects.add(pokeTypeToAspects.get(type1));
            aspects.add(pokeTypeToAspects.get(type1));
            aspects.add(pokeTypeToAspects.get(type2));
        }
        else
        {
            aspects.add(pokeTypeToAspects.get(type1));
            aspects.add(pokeTypeToAspects.get(type1));
            aspects.add(pokeTypeToAspects.get(type1));
            aspects.add(pokeTypeToAspects.get(type1));
        }

        if (entry.swims())
        {
            aspects.add(new AspectList().add(Aspect.WATER, 2));
        }

        if (entry.flys())
        {
            aspects.add(new AspectList().add(Aspect.AIR, 2));
        }

        if (entry.getFoodDrop(0) != null)
        {
            aspects.add(new AspectList().add(Aspect.LIFE, 1));
        }

        return aspects;
    }

    @SubscribeEvent
    public void init(PostPostInit evt)
    {
        System.out.println("Attempting to register pokecube entities with Thaumcraft...");
        init();
        registerPokemobsThaumcraft();
        registerTrainersThaumcraft();
        addPage();
        addResearch();
        addLoot();
    }

    @SubscribeEvent
    public void onToolTip(ItemTooltipEvent evt)
    {
        EntityPlayer player = evt.entityPlayer;
        ItemStack stack = evt.itemStack;
        if (player == null || player.openContainer == null || stack == null) return;
        if (PokecubeItems.getCubeId(stack) == 98 && stack.hasTagCompound() && stack.getTagCompound().hasKey("Aspects")
                && Loader.isModLoaded("Thaumcraft"))
        {
            NBTTagList tlist = stack.getTagCompound().getTagList("Aspects", (byte) 10);
            for (int j = 0; j < tlist.tagCount(); j++)
            {
                NBTTagCompound rs = tlist.getCompoundTagAt(j);
                if (rs.hasKey("key"))
                {
                    thaumcraft.api.aspects.Aspect a = thaumcraft.api.aspects.Aspect.getAspect(rs.getString("key"));
                    int num = rs.getInteger("amount");
                    evt.toolTip.add(a.getName() + " x" + num);
                }
            }
        }
    }

    public void registerPokemobsThaumcraft()
    {
        for (Integer i : Pokedex.getInstance().getEntries())
        {
            PokedexEntry entry = Pokedex.getInstance().getEntry(i);
            Class<?> klass = PokecubeMod.core.getEntityClassFromPokedexNumber(entry.getPokedexNb());

            if (klass != null && entry != null)
            {
                String name = EntityList.classToStringMapping.get(klass);

                if (name != null)
                {
                    try
                    {
                        ThaumcraftApi.registerEntityTag(name, getAspects(entry),
                                new ThaumcraftApi.EntityTagsNBT("pokedexNb", Integer.valueOf(entry.getPokedexNb())));
                    }
                    catch (Exception e)
                    {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
                else
                {
                    Integer nb = entry.getPokedexNb();
                    System.out.println("Error: name for pokemon with nb " + nb.toString() + " is null");
                }
            }
            else
            {
                Integer nb = entry.getPokedexNb();
                System.out.println("Pokemob " + nb.toString() + " is genericMob");

                ThaumcraftApi.registerEntityTag("pokecube.pokecube:genericMob", getAspects(entry),
                        new ThaumcraftApi.EntityTagsNBT("pokedexNb", Integer.valueOf(entry.getPokedexNb())));
            }
        }
    }

    public void registerTrainersThaumcraft()
    {
        Class<EntityTrainer> klass = pokecube.adventures.entity.trainers.EntityTrainer.class;

        String name = EntityList.classToStringMapping.get(klass);

        AspectList aspects = new AspectList();

        aspects.add(Aspect.MAN, 4);
        aspects.add(Aspect.BEAST, 2);
        aspects.add(Aspect.ORDER, 2);
        aspects.add(Aspect.TRAP, 1);
        aspects.add(Aspect.EXCHANGE, 1);

        if (name != null)
        {
            ThaumcraftApi.registerEntityTag(name, aspects);
        }
    }
}
