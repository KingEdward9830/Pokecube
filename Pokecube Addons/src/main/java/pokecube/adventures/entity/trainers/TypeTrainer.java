package pokecube.adventures.entity.trainers;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResource;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import pokecube.adventures.PokecubeAdv;
import pokecube.core.PokecubeItems;
import pokecube.core.database.BiomeMatcher;
import pokecube.core.database.Pokedex;
import pokecube.core.database.PokedexEntry;
import pokecube.core.database.PokedexEntry.EvolutionData;
import pokecube.core.interfaces.IPokemob;
import pokecube.core.interfaces.PokecubeMod;
import pokecube.core.items.pokecubes.PokecubeManager;
import pokecube.core.utils.Tools;
import thut.api.terrain.BiomeType;

public class TypeTrainer
{
    public static HashMap<String, TypeTrainer>      typeMap     = new HashMap<String, TypeTrainer>();
    public static HashMap<Biome, List<TypeTrainer>> biomeMap    = Maps.newHashMap();
    public static ArrayList<String>                 maleNames   = new ArrayList<String>();
    public static ArrayList<String>                 femaleNames = new ArrayList<String>();

    public static void addTrainer(String name, TypeTrainer type)
    {
        typeMap.put(name, type);
    }

    public static void initSpawns()
    {
        biomeMap.clear();
        for (TypeTrainer t : typeMap.values())
        {
            if (t.matcher != null)
            {
                t.matcher.parse();
                if (!t.matcher.validBiomes.isEmpty())
                {
                    for (Biome b : t.matcher.validBiomes)
                    {
                        addSpawn(b, t);
                    }
                }
                else if (!t.matcher.validSubBiomes.contains(BiomeType.NONE))
                {
                    for (ResourceLocation key : Biome.REGISTRY.getKeys())
                    {
                        Biome b = Biome.REGISTRY.getObject(key);
                        if (b != null)
                        {
                            addSpawn(b, t);
                        }
                    }
                }
            }
        }
    }

    private static void addSpawn(Biome b, TypeTrainer t)
    {
        List<TypeTrainer> types = biomeMap.get(b);
        if (types == null)
        {
            biomeMap.put(b, types = Lists.newArrayList());
        }
        for (int i = 0; i < t.weight; i++)
            types.add(t);
    }

    public static void getRandomTeam(EntityTrainer trainer, int level, ItemStack[] team, World world)
    {
        TypeTrainer type = trainer.getType();

        for (int i = 0; i < 6; i++)
            team[i] = null;

        if (level == 0) level = 5;
        int variance = PokecubeMod.core.getConfig().levelVariance;
        int number = 1 + new Random().nextInt(7);
        number = Math.min(number, 6);

        for (int i = 0; i < number; i++)
        {
            Collections.shuffle(type.pokemon);
            ItemStack item = null;
            for (PokedexEntry s : type.pokemon)
            {
                if (s != null)
                {
                    variance = new Random().nextInt(Math.max(1, variance));
                    item = makeStack(s, trainer, world, level + variance);
                }
                if (item != null) break;
            }
            team[i] = item;
        }
    }

    public static TypeTrainer getTrainer(String name)
    {
        TypeTrainer ret = typeMap.get(name);
        if (ret == null) for (TypeTrainer t : typeMap.values())
        {
            if (t != null) return t;
        }
        return ret;
    }

    public static ItemStack makeStack(PokedexEntry entry, EntityLivingBase trainer, World world, int level)
    {
        int num = entry.getPokedexNb();
        if (Pokedex.getInstance().getEntry(num) == null) return null;

        IPokemob entity = (IPokemob) PokecubeMod.core.createEntityByPokedexEntry(entry, world);
        if (entity != null)
        {
            for (int i = 1; i < level; i++)
            {
                if (entity.getPokedexEntry().canEvolve(i))
                {
                    for (EvolutionData d : entity.getPokedexEntry().getEvolutions())
                    {
                        if (d.shouldEvolve(entity))
                        {
                            Entity temp = d.getEvolution(world);
                            if (temp != null)
                            {
                                entity = (IPokemob) temp;
                                break;
                            }
                        }
                    }
                }
            }
            ((EntityLivingBase) entity).setHealth(((EntityLivingBase) entity).getMaxHealth());

            entity.changeForme(entry.getName());
            entity.setPokemonOwner(trainer);
            entity.setPokecubeId(0);
            int exp = Tools.levelToXp(entity.getExperienceMode(), level);
            entity.setExp(exp, true, false);
            ItemStack item = PokecubeManager.pokemobToItem(entity);

            ((Entity) entity).isDead = true;
            return item;
        }

        return null;
    }

    public static void postInitTrainers()
    {
        List<TypeTrainer> toRemove = new ArrayList<TypeTrainer>();
        for (TypeTrainer t : typeMap.values())
        {
            if (t.pokemon.size() == 0)
            {
                toRemove.add(t);
            }
        }
        for (TypeTrainer t : toRemove)
        {
            typeMap.remove(t.name);
        }
    }

    public final String       name;
    /** 1 = male, 2 = female, 3 = both */
    public byte               genders  = 1;

    public Material           material = Material.AIR;
    public BiomeMatcher       matcher  = null;
    public int                weight;
    public boolean            hasBag   = false;
    private ResourceLocation  texture;

    private ResourceLocation  femaleTexture;

    public List<PokedexEntry> pokemon  = new ArrayList<PokedexEntry>();

    private ItemStack[]       loot     = new ItemStack[4];

    public String             drops    = "";
    public ItemStack          held;

    public TypeTrainer(String name)
    {
        this.name = name;
        typeMap.put(name, this);
    }

    public TypeTrainer(String name, Material material)
    {
        this(name);
        this.material = material;
    }

    @SideOnly(Side.CLIENT)
    public ResourceLocation getTexture(EntityTrainer trainer)
    {
        if (texture == null && (genders == 1 || genders == 2))
        {
            texture = new ResourceLocation(PokecubeAdv.TRAINERTEXTUREPATH + name + ".png");
            if (!texExists(texture)) texture = null;
            if (genders == 2 && texture == null)
            {
                texture = new ResourceLocation(PokecubeAdv.TRAINERTEXTUREPATH + "female.png");
            }
            if (genders == 1 && texture == null)
            {
                texture = new ResourceLocation(PokecubeAdv.TRAINERTEXTUREPATH + "male.png");
            }
        }
        else if (genders == 3)
        {

            if (femaleTexture == null)
            {
                femaleTexture = new ResourceLocation(PokecubeAdv.TRAINERTEXTUREPATH + name + "female.png");
                if (!texExists(femaleTexture)) femaleTexture = null;
            }
            if (texture == null)
            {
                texture = new ResourceLocation(PokecubeAdv.TRAINERTEXTUREPATH + name + ".png");
                if (!texExists(texture)) texture = null;
            }
            if (femaleTexture == null)
            {
                femaleTexture = new ResourceLocation(PokecubeAdv.TRAINERTEXTUREPATH + "female.png");
            }
            if (texture == null)
            {
                texture = new ResourceLocation(PokecubeAdv.TRAINERTEXTUREPATH + "male.png");
            }
            return trainer.male ? texture : femaleTexture;
        }
        return texture;
    }

    private void initLoot()
    {
        if (loot[0] != null) return;

        if (!drops.equals(""))
        {
            String[] args = drops.split(":");
            int num = 0;
            for (String s : args)
            {
                if (s == null) continue;
                String[] stackinfo = s.split("`");
                ItemStack stack = PokecubeItems.getStack(stackinfo[0]);
                if (stackinfo.length > 1)
                {
                    try
                    {
                        int count = Integer.parseInt(stackinfo[1]);
                        stack.stackSize = count;
                    }
                    catch (NumberFormatException e)
                    {
                    }
                }
                if (stackinfo.length > 2)
                {
                    try
                    {
                        int count = Integer.parseInt(stackinfo[2]);
                        stack.setItemDamage(count);
                    }
                    catch (NumberFormatException e)
                    {
                    }
                }
                loot[num] = stack;
                num++;
            }
        }
        if (loot[0] == null) loot[0] = new ItemStack(Items.EMERALD);
    }

    public void initTrainerItems(EntityTrainer trainer)
    {
        initLoot();
        for (int i = 1; i < 5; i++)
        {
            EntityEquipmentSlot slotIn = EntityEquipmentSlot.values()[i];
            trainer.setItemStackToSlot(slotIn, loot[i - 1]);
        }
    }

    @SideOnly(Side.CLIENT)
    private boolean texExists(ResourceLocation texture)
    {
        try
        {
            IResource res = Minecraft.getMinecraft().getResourceManager().getResource(texture);
            InputStream stream = res.getInputStream();
            stream.close();
            return true;
        }
        catch (Exception e)
        {
            return false;
        }
    }

    @Override
    public String toString()
    {
        return "" + name + " " + pokemon;
    }

    public boolean validMaterial(Material m)
    {
        if (this.material == Material.WATER) return m == Material.WATER;
        if (this.material == Material.AIR) { return m == material
                || (!m.isLiquid() && !m.isSolid() && m.isReplaceable()); }

        return false;
    }
}
