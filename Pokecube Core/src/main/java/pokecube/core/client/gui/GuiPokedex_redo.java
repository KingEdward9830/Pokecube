/**
 *
 */
package pokecube.core.client.gui;

import static pokecube.core.database.PokedexEntry.SpawnData.CAVE;
import static pokecube.core.database.PokedexEntry.SpawnData.INDUSTRIAL;
import static pokecube.core.utils.PokeType.flying;
import static pokecube.core.utils.PokeType.getTranslatedName;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import com.google.common.collect.Lists;

import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import pokecube.core.PokecubeCore;
import pokecube.core.PokecubeItems;
import pokecube.core.client.ClientProxyPokecube;
import pokecube.core.client.Resources;
import pokecube.core.database.Pokedex;
import pokecube.core.database.PokedexEntry;
import pokecube.core.database.PokedexEntry.SpawnData;
import pokecube.core.database.abilities.Ability;
import pokecube.core.database.stats.CaptureStats;
import pokecube.core.database.stats.EggStats;
import pokecube.core.database.stats.KillStats;
import pokecube.core.events.handlers.SpawnHandler;
import pokecube.core.interfaces.IMoveConstants;
import pokecube.core.interfaces.IPokemob;
import pokecube.core.interfaces.Move_Base;
import pokecube.core.interfaces.PokecubeMod;
import pokecube.core.moves.MovesUtils;
import pokecube.core.network.PokecubePacketHandler;
import pokecube.core.network.PokecubePacketHandler.PokecubeServerPacket;
import pokecube.core.utils.PokeType;
import pokecube.core.utils.PokecubeSerializer;
import pokecube.core.utils.PokecubeSerializer.TeleDest;
import pokecube.core.utils.Tools;
import thut.api.entity.IMobColourable;
import thut.api.maths.Vector3;
import thut.api.maths.Vector4;
import thut.api.terrain.BiomeDatabase;
import thut.api.terrain.BiomeType;
import thut.api.terrain.TerrainManager;
import thut.api.terrain.TerrainSegment;

public class GuiPokedex_redo extends GuiScreen
{
    public static PokedexEntry                    pokedexEntry       = null;
    public static Vector3                         closestVillage     = Vector3.getNewVector();

    private static final int                      PAGECOUNT          = 5;

    private static HashMap<Integer, EntityLiving> entityToDisplayMap = new HashMap<Integer, EntityLiving>();

    /** to pass as last parameter when rendering the mob so that the render
     * knows the rendering is asked by the pokedex gui */
    public final static float                     POKEDEX_RENDER     = 1.5f;

    public IPokemob                               pokemob            = null;
    protected EntityPlayer                        entityPlayer       = null;
    protected GuiTextField                        nicknameTextField;
    /** The X size of the inventory window in pixels. */
    protected int                                 xSize;
    /** The Y size of the inventory window in pixels. */
    protected int                                 ySize;                                                    // old:166

    private float                                 xRenderAngle       = 0;
    private float                                 yHeadRenderAngle   = 10;
    private float                                 xHeadRenderAngle   = 0;

    private int                                   mouseRotateControl;

    private int                                   page               = 0;

    private int                                   index              = 0;
    private int                                   index2             = 0;
    private boolean                               mode               = false;

    List<Integer>                                 biomes             = new ArrayList<Integer>();

    int                                           prevX              = 0;
    int                                           prevY              = 0;

    String                                        oldName            = "";
    boolean                                       nicknameChange     = false;

    /**
     *
     */
    public GuiPokedex_redo(IPokemob pokemob, EntityPlayer entityPlayer)
    {
        xSize = 256;
        ySize = 197;
        this.pokemob = pokemob;
        this.entityPlayer = entityPlayer;
        ItemStack item = entityPlayer.getHeldItemMainhand();
        if (item != null)
        {
            page = item.getItemDamage();
        }

        if (pokemob != null)
        {
            pokedexEntry = pokemob.getPokedexEntry();
        }
        else if (pokedexEntry == null)
        {
            pokedexEntry = Pokedex.getInstance().getFirstEntry();
        }

        for (ResourceLocation key : Biome.REGISTRY.getKeys())
        {
            Biome b = Biome.REGISTRY.getObject(key);
            if (b != null)
            {
                int id = Biome.getIdForBiome(b);
                biomes.add(id);
            }
        }
        for (BiomeType b : BiomeType.values())
        {
            biomes.add(b.getType());
        }
    }

    private boolean canEditPokemob()
    {
        return pokemob != null && pokedexEntry.getPokedexNb() == pokemob.getPokedexNb()
                && ((pokemob.getPokemonAIState(IMoveConstants.TAMED) && entityPlayer == pokemob.getPokemonOwner())
                        || entityPlayer.capabilities.isCreativeMode);
    }

    @Override
    public boolean doesGuiPauseGame()
    {
        return false;
    }

    @Override
    /** Draws the background (i is always 0 as of 1.2.2) */
    public void drawBackground(int tint)
    {
        super.drawBackground(tint);
    }

    /** Draws the first page of the pokedex, this is the page with the pokemob's
     * stats and current moves, if no pokemob is selected, it tries to show an
     * arrow to the nearest village
     * 
     * @param xOffset
     * @param yOffset */
    private void drawPage0(int xOffset, int yOffset)
    {
        if (pokemob != null && pokemob.getPokedexNb() == pokedexEntry.getPokedexNb())
        {
            int genderColor = 0xBBBBBB;
            String gender = "";

            if (pokemob.getSexe() == IPokemob.MALE)
            {
                genderColor = 0x0011CC;
                gender = "\u2642";
            }
            else if (pokemob.getSexe() == IPokemob.FEMALE)
            {
                genderColor = 0xCC5555;
                gender = "\u2640";
            }

            String level = "L. " + pokemob.getLevel();
            drawString(fontRendererObj, level, xOffset + 15, yOffset + 1, 0xffffff);
            drawCenteredString(fontRendererObj, gender, xOffset - 20, yOffset + 122, genderColor);
            byte[] nature = pokemob.getNature().getStatsMod();
            int[] stats = Tools.getStats(pokemob);

            if (canEditPokemob() || PokecubeMod.debug)
            {
                int HP = stats[0];
                int ATT = stats[1];
                int DEF = stats[2];
                int ATTSPE = stats[3];
                int DEFSPE = stats[4];
                int VIT = stats[5];
                int statYOffSet = yOffset + 15;
                String[] nat = new String[6];
                int[] colours = new int[6];
                for (int n = 0; n < 6; n++)
                {
                    nat[n] = "";
                    colours[n] = 0;
                    if (nature[n] == -1)
                    {
                        nat[n] = "-";
                        colours[n] = 0x6890F0;
                    }
                    if (nature[n] == 1)
                    {
                        nat[n] = "+";
                        colours[n] = 0xFF0000;
                    }
                }
                drawString(fontRendererObj, "HP", xOffset + 20, statYOffSet + 18, 0xFF0000);
                drawString(fontRendererObj, "ATT", xOffset + 20, statYOffSet + 27, 0xF08030);
                drawString(fontRendererObj, "DEF", xOffset + 20, statYOffSet + 36, 0xF8D030);
                drawString(fontRendererObj, "ATTSPE", xOffset + 20, statYOffSet + 45, 0x6890F0);
                drawString(fontRendererObj, "DEFSPE", xOffset + 20, statYOffSet + 54, 0x78C850);
                drawString(fontRendererObj, "VIT", xOffset + 20, statYOffSet + 63, 0xF85888);
                drawString(fontRendererObj, ": " + HP, xOffset + 60, statYOffSet + 18, 0xFF0000);
                drawString(fontRendererObj, ": " + ATT, xOffset + 60, statYOffSet + 27, 0xF08030);
                drawString(fontRendererObj, ": " + DEF, xOffset + 60, statYOffSet + 36, 0xF8D030);
                drawString(fontRendererObj, ": " + ATTSPE, xOffset + 60, statYOffSet + 45, 0x6890F0);
                drawString(fontRendererObj, ": " + DEFSPE, xOffset + 60, statYOffSet + 54, 0x78C850);
                drawString(fontRendererObj, ": " + VIT, xOffset + 60, statYOffSet + 63, 0xF85888);

                drawString(fontRendererObj, nat[0], xOffset + 100, statYOffSet + 18, colours[0]);
                drawString(fontRendererObj, nat[1], xOffset + 100, statYOffSet + 27, colours[1]);
                drawString(fontRendererObj, nat[2], xOffset + 100, statYOffSet + 36, colours[2]);
                drawString(fontRendererObj, nat[3], xOffset + 100, statYOffSet + 45, colours[3]);
                drawString(fontRendererObj, nat[4], xOffset + 100, statYOffSet + 54, colours[4]);
                drawString(fontRendererObj, nat[5], xOffset + 100, statYOffSet + 63, colours[5]);

                statYOffSet += 75;
                drawString(fontRendererObj, "IV       EV", xOffset + 67, statYOffSet + 9, 0xFFFFFF);
                drawString(fontRendererObj, "HP", xOffset + 20, statYOffSet + 18, 0xFF0000);
                drawString(fontRendererObj, "ATT", xOffset + 20, statYOffSet + 27, 0xF08030);
                drawString(fontRendererObj, "DEF", xOffset + 20, statYOffSet + 36, 0xF8D030);
                drawString(fontRendererObj, "ATTSPE", xOffset + 20, statYOffSet + 45, 0x6890F0);
                drawString(fontRendererObj, "DEFSPE", xOffset + 20, statYOffSet + 54, 0x78C850);
                drawString(fontRendererObj, "VIT", xOffset + 20, statYOffSet + 63, 0xF85888);
                byte[] stats2 = pokemob.getIVs();
                HP = stats2[0];
                ATT = stats2[1];
                DEF = stats2[2];
                ATTSPE = stats2[3];
                DEFSPE = stats2[4];
                VIT = stats2[5];
                stats2 = pokemob.getEVs();
                int HP2 = stats2[0] + 128;
                int ATT2 = stats2[1] + 128;
                int DEF2 = stats2[2] + 128;
                int ATTSPE2 = stats2[3] + 128;
                int DEFSPE2 = stats2[4] + 128;
                int VIT2 = stats2[5] + 128;

                int shift = 60;
                drawString(fontRendererObj, ": " + HP, xOffset + shift, statYOffSet + 18, 0xFF0000);
                drawString(fontRendererObj, ": " + ATT, xOffset + shift, statYOffSet + 27, 0xF08030);
                drawString(fontRendererObj, ": " + DEF, xOffset + shift, statYOffSet + 36, 0xF8D030);
                drawString(fontRendererObj, ": " + ATTSPE, xOffset + shift, statYOffSet + 45, 0x6890F0);
                drawString(fontRendererObj, ": " + DEFSPE, xOffset + shift, statYOffSet + 54, 0x78C850);
                drawString(fontRendererObj, ": " + VIT, xOffset + shift, statYOffSet + 63, 0xF85888);
                shift = 105;
                drawString(fontRendererObj, "" + HP2, xOffset + shift, statYOffSet + 18, 0xFF0000);
                drawString(fontRendererObj, "" + ATT2, xOffset + shift, statYOffSet + 27, 0xF08030);
                drawString(fontRendererObj, "" + DEF2, xOffset + shift, statYOffSet + 36, 0xF8D030);
                drawString(fontRendererObj, "" + ATTSPE2, xOffset + shift, statYOffSet + 45, 0x6890F0);
                drawString(fontRendererObj, "" + DEFSPE2, xOffset + shift, statYOffSet + 54, 0x78C850);
                drawString(fontRendererObj, "" + VIT2, xOffset + shift, statYOffSet + 63, 0xF85888);
            }
        }
        else
        {
            int num = 1;
            PokedexEntry test = Pokedex.getInstance().getFirstEntry();
            while (test != pokedexEntry && num < 1500)
            {
                test = Pokedex.getInstance().getNext(test, 1);
                num++;
            }

            String level = "N. " + num;
            drawString(fontRendererObj, level, xOffset + 15, yOffset + 1, 0xffffff);

            if (!closestVillage.isEmpty())
            {
                GL11.glPushMatrix();
                GL11.glTranslated(xSize + 45, ySize / 2 + 23, 0);
                Vector3 v = Vector3.getNewVector().set(entityPlayer).subtractFrom(closestVillage);
                v.reverse();
                v.set(v.normalize());
                double angle = Math.atan2(v.z, v.x) * 180 / Math.PI - entityPlayer.rotationYaw % 360 + 180;

                GL11.glRotated(angle, 0, 0, 1);
                drawString(fontRendererObj, "--->", 0, 0, 0xFF0000);
                GL11.glPopMatrix();
                String mess = I18n.format("gui.pokedex.village");
                int width = fontRendererObj.getStringWidth(mess);
                drawString(fontRendererObj, mess, xOffset - width / 2 + 60, yOffset + 75, 0x78C850);
                mess = ((int) v.set(closestVillage).distToEntity(entityPlayer)) + "";
                width = fontRendererObj.getStringWidth(mess);
                drawString(fontRendererObj, mess, xOffset - width / 2 + 60, yOffset + 89, 0x78C850);
            }
        }
    }

    /** Draws the learnable moves page.
     * 
     * @param xOffset
     * @param yOffset */
    private void drawPage1(int xOffset, int yOffset)
    {
        ArrayList<String> names = new ArrayList<String>();
        ArrayList<String> levels = new ArrayList<String>();
        if (pokedexEntry != null)
        {
            for (int i = 0; i < 100; i++)
            {
                List<String> moves = pokedexEntry.getMovesForLevel(i, i - 1);
                for (String s : moves)
                {
                    levels.add("" + i);
                    names.add(s);
                }
            }
            List<String> allMoves = pokedexEntry.getMoves();
            for (String s : allMoves)
            {
                if (!names.contains(s))
                {
                    levels.add("TM");
                    names.add(s);
                }
            }
        }
        index = Math.max(0, Math.min(index, names.size() - 6));
        int n;
        drawString(fontRendererObj, "Moves", xOffset + 16, yOffset + 15, 0xFFFFFF);
        for (n = 0; n < Math.min(names.size(), 6); n++)
        {
            drawString(fontRendererObj, MovesUtils.getLocalizedMove(names.get(n + index)), xOffset + 18,
                    yOffset + 30 + n * 10, 0xFF0000);
            drawString(fontRendererObj, levels.get(n + index), xOffset + 92, yOffset + 30 + n * 10, 0xFF0000);
        }

        drawSelectedMoves(xOffset + 5, yOffset + 0);
    }

    /** Draws the biome info page, as well as the various statistics at the
     * bottom
     * 
     * @param xOffset
     * @param yOffset */
    private void drawPage2(int xOffset, int yOffset)
    {
        if (!mode)
        {
            if (index2 < 0) index2 = biomes.size() - 2;
            index2 = Math.max(0, index2 % (biomes.size() - 1));

            String biomeName = BiomeDatabase.getNameFromType(biomes.get(index2));
            ArrayList<PokedexEntry> names = new ArrayList<PokedexEntry>();

            if (SpawnHandler.spawnLists.containsKey(biomes.get(index2)))
            {
                for (PokedexEntry dbe : SpawnHandler.spawnLists.get(biomes.get(index2)))
                {
                    if (!dbe.getSpawnData().types[SpawnData.LEGENDARY] && dbe.getPokedexNb() != 151) names.add(dbe);
                }
            }

            index = Math.max(0, Math.min(index, names.size() - 6));
            String title = BiomeDatabase.getReadableNameFromType(biomes.get(index2));
            if (title.equalsIgnoreCase("mushroomislandshore")) title = "Mushroom Shore";
            if (title.equalsIgnoreCase("birch forest hills m")) title = "Birch ForestHills M";

            drawString(fontRendererObj, title, xOffset + 16, yOffset + 15, 0xFFFFFF);

            for (int n = 0; n < Math.min(names.size(), 6); n++)
            {
                int yO = yOffset + 30;
                PokedexEntry dbe = names.get((n + index));

                if (dbe.getSpawnData() == null)
                {
                    System.err.println("FATAL ERROR MISSING SPAWN DATA FOR " + dbe);
                    continue;
                }
                String numbers = "";// "+dbe.getSpawnData().global[0];
                if (!entityPlayer.capabilities.isCreativeMode) numbers = "";
                drawString(fontRendererObj, I18n.format(dbe.getUnlocalizedName()) + numbers, xOffset + 18, yO + n * 10,
                        0xFF0000);
                String time = "";
                boolean cave = dbe.getSpawnData().types[CAVE];

                boolean industrial = dbe.getSpawnData().types[INDUSTRIAL];

                if (dbe.getSpawnData().types[SpawnData.DAY]) time = time + "D";
                if (dbe.getSpawnData().types[SpawnData.NIGHT]) time = time + "N";
                if (cave && !biomeName.toLowerCase().contains("cave"))
                {
                    time = time + "C";
                }
                if (industrial) time = time + "I";

                drawString(fontRendererObj, time, xOffset + 85, yO + n * 10, 0xFF0000);
            }
        }
        else if (getEntityToDisplay() != null)
        {
            drawString(fontRendererObj, getEntityToDisplay().getDisplayName().getFormattedText(), xOffset + 16,
                    yOffset + 15, 0xFFFFFF);
            List<String> biomes = Lists.newArrayList();

            lists:
            for (Integer i : this.biomes)
            {
                if (SpawnHandler.spawnLists.containsKey(i))
                {
                    for (PokedexEntry dbe : SpawnHandler.spawnLists.get(i))
                    {
                        if (dbe.getPokedexNb() == ((IPokemob) getEntityToDisplay()).getPokedexNb())
                        {
                            String title = BiomeDatabase.getReadableNameFromType(i);
                            if (title.equalsIgnoreCase("mushroomislandshore")) title = "Mushroom Shore";
                            if (title.equalsIgnoreCase("birch forest hills m")) title = "Birch ForestHills M";
                            biomes.add(title);
                            continue lists;
                        }
                    }
                }
            }
            // index = Math.max(0, Math.min(index, biomes.size() - 6));
            int ind = 0;
            for (int n = 0; n < Math.min(biomes.size(), 6); n++)
            {
                String s = biomes.get((ind + index) % biomes.size());
                int yO = yOffset + 30;
                int length = fontRendererObj.getStringWidth(s);
                fontRendererObj.drawSplitString(s, xOffset + 18, yO + n * 10, 90, 0xFF0000);
                if (length > 90)
                {
                    n++;
                }
                ind++;
            }
        }

        drawString(fontRendererObj, "User Stats", xOffset + 19, yOffset + 99, 0xFFFFFF);

        int count = KillStats.getNumberUniqueKilledBy(entityPlayer.getUniqueID().toString());
        int count2 = KillStats.getTotalNumberKilledBy(entityPlayer.getUniqueID().toString());

        drawString(fontRendererObj, "Kills", xOffset + 19, yOffset + 112, 0xFFFFFF);
        drawString(fontRendererObj, count + "/" + count2,
                xOffset + 120 - fontRendererObj.getStringWidth((count + "/" + count2)), yOffset + 112, 0xffffff);

        count = CaptureStats.getNumberUniqueCaughtBy(entityPlayer.getUniqueID().toString());
        count2 = CaptureStats.getTotalNumberCaughtBy(entityPlayer.getUniqueID().toString());
        drawString(fontRendererObj, "Captures", xOffset + 19, yOffset + 126, 0xFFFFFF);
        drawString(fontRendererObj, count + "/" + count2,
                xOffset + 120 - fontRendererObj.getStringWidth((count + "/" + count2)), yOffset + 126, 0xffffff);

        count = EggStats.getNumberUniqueHatchedBy(entityPlayer.getUniqueID().toString());
        count2 = EggStats.getTotalNumberHatchedBy(entityPlayer.getUniqueID().toString());
        drawString(fontRendererObj, "Hatched", xOffset + 19, yOffset + 140, 0xFFFFFF);
        drawString(fontRendererObj, count + "/" + count2,
                xOffset + 120 - fontRendererObj.getStringWidth((count + "/" + count2)), yOffset + 140, 0xffffff);
        World world = entityPlayer.worldObj;
        List<Object> entities = new ArrayList<Object>(world.loadedEntityList);
        count = 0;
        count2 = 0;
        for (Object o : entities)
        {
            if (o instanceof IPokemob)
            {
                count++;
            }
        }
        drawString(fontRendererObj, "Around", xOffset + 19, yOffset + 154, 0xFFFFFF);
        drawString(fontRendererObj, "" + count, xOffset + 120 - fontRendererObj.getStringWidth((count + "")),
                yOffset + 154, 0xffffff);
    }

    /** Draws the breeding information page
     * 
     * @param xOffset
     * @param yOffset */
    private void drawPage3(int xOffset, int yOffset)
    {
        ArrayList<String> names = new ArrayList<String>();
        if (pokedexEntry != null)
        {
            for (PokedexEntry p : pokedexEntry.related)
            {
                if (p != null) names.add(I18n.format(p.getUnlocalizedName()));
            }
        }
        index = Math.max(0, Math.min(index, names.size() - 6));
        int n;
        drawString(fontRendererObj, "Possible Mates", xOffset + 16, yOffset + 15, 0xFFFFFF);
        for (n = 0; n < Math.min(names.size(), 6); n++)
        {
            drawString(fontRendererObj, MovesUtils.getLocalizedMove(names.get(n + index)), xOffset + 18,
                    yOffset + 30 + n * 10, 0xFF0000);
        }
        if (pokemob != null)
        {
            Ability ability = pokemob.getAbility();
            if (ability != null)
            {
                drawString(fontRendererObj, "AB: " + I18n.format(ability.getName()), xOffset + 19, yOffset + 99,
                        0xFFFFFF);
            }
            int happiness = pokemob.getHappiness();
            String message = "";
            if (happiness == 0)
            {
                message = "pokemob.info.happy0";
            }
            if (happiness > 0)
            {
                message = "pokemob.info.happy1";
            }
            if (happiness > 49)
            {
                message = "pokemob.info.happy2";
            }
            if (happiness > 99)
            {
                message = "pokemob.info.happy3";
            }
            if (happiness > 149)
            {
                message = "pokemob.info.happy4";
            }
            if (happiness > 199)
            {
                message = "pokemob.info.happy5";
            }
            if (happiness > 254)
            {
                message = "pokemob.info.happy6";
            }
            message = I18n.format(message);
            fontRendererObj.drawSplitString(message, xOffset + 19, yOffset + 145, 100, 0xFFFFFF);

            message = "Size: " + pokemob.getSize();
            fontRendererObj.drawSplitString(message, xOffset + 19, yOffset + 122, 100, 0xFFFFFF);
        }
    }

    /** Draws the teleport locations page
     * 
     * @param xOffset
     * @param yOffset */
    private void drawPage4(int xOffset, int yOffset)
    {
        Minecraft minecraft = (Minecraft) PokecubeCore.getMinecraftInstance();
        List<TeleDest> locations = PokecubeSerializer.getInstance()
                .getTeleports(minecraft.thePlayer.getUniqueID().toString());

        if (locations.size() == 0) { return; }

        TeleDest location = locations.get((GuiTeleport.instance().indexLocation) % locations.size());

        if (location != null) drawString(fontRendererObj, location.loc.toIntString(), xOffset + 16,
                yOffset + 99 + 14 * index, PokeType.fire.colour);
    }

    @Override
    public void drawScreen(int i, int j, float f)
    {
        super.drawScreen(i, j, f);

        Minecraft minecraft = (Minecraft) PokecubeCore.getMinecraftInstance();

        minecraft.renderEngine.bindTexture(Resources.GUI_POKEDEX_REDO);
        int j2 = (width - xSize) / 2;
        int k2 = (height - ySize) / 2;
        drawTexturedModalRect(j2, k2, 0, 0, xSize, ySize);

        GL11.glPushMatrix();

        int yOffset = height / 2 - 80;
        int xOffset = width / 2;

        GL11.glPushMatrix();
        renderMob();// TODO find out why rendering player messes up shaders
        GL11.glPopMatrix();
        nicknameTextField.drawTextBox();
        drawCenteredString(fontRendererObj, "" + (page + 1), xOffset + 55, yOffset + 1, 0xffffff);
        if (page == 0)
        {
            drawPage0(xOffset, yOffset);
        }
        else if (page == 1)
        {
            drawPage1(xOffset, yOffset);
        }
        else if (page == 2)
        {
            drawPage2(xOffset, yOffset);
        }
        else if (page == 3)
        {
            drawPage3(xOffset, yOffset);
        }
        else if (page == 4)
        {
            drawPage4(xOffset, yOffset);
        }

        if (pokedexEntry != null)
        {
            drawCenteredString(fontRendererObj, I18n.format(pokedexEntry.getUnlocalizedName()), xOffset - 65,
                    yOffset + 123, 0xffffff);
            drawCenteredString(fontRendererObj, "#" + pokedexEntry.getPokedexNb(), xOffset - 28, yOffset + 02,
                    0xffffff);
            try
            {
                drawCenteredString(fontRendererObj, getTranslatedName(pokedexEntry.getType1()), xOffset - 88,
                        yOffset + 137, pokedexEntry.getType1().colour);
                drawCenteredString(fontRendererObj, getTranslatedName(pokedexEntry.getType2()), xOffset - 44,
                        yOffset + 137, pokedexEntry.getType2().colour);
            }
            catch (Exception e)
            {
                // System.out.println(pokedexEntry);
                // e.printStackTrace();
            }
        }
        GL11.glPopMatrix();
    }

    /** Used to draw in the list of moves that the pokemob has in the bottom
     * section of some pages
     * 
     * @param xOffset
     * @param yOffset */
    public void drawSelectedMoves(int xOffset, int yOffset)
    {
        if (pokemob == null || !canEditPokemob()) return;

        Move_Base move = MovesUtils.getMoveFromName(pokemob.getMove(0));

        String pwr = "";

        if (move != null)
        {
            if (move.getPWR(pokemob, entityPlayer) > 0)
            {
                pwr = "" + move.getPWR(pokemob, entityPlayer);
            }
            else
            {
                pwr = "-";
            }
            drawString(fontRendererObj, MovesUtils.getLocalizedMove(move.getName()), xOffset + 14, yOffset + 99,
                    move.getType(pokemob).colour);
            drawString(fontRendererObj, "" + pwr, xOffset + 102, yOffset + 99, 0xffffff);
        }

        move = MovesUtils.getMoveFromName(pokemob.getMove(1));

        if (move != null)
        {
            if (move.getPWR(pokemob, entityPlayer) > 0)
            {
                pwr = "" + move.getPWR(pokemob, entityPlayer);
            }
            else
            {
                pwr = "-";
            }
            drawString(fontRendererObj, MovesUtils.getLocalizedMove(move.getName()), xOffset + 14, yOffset + 113,
                    move.getType(pokemob).colour);
            drawString(fontRendererObj, "" + pwr, xOffset + 102, yOffset + 113, 0xffffff);
        }

        move = MovesUtils.getMoveFromName(pokemob.getMove(2));

        if (move != null)
        {
            if (move.getPWR(pokemob, entityPlayer) > 0)
            {
                pwr = "" + move.getPWR(pokemob, entityPlayer);
            }
            else
            {
                pwr = "-";
            }
            drawString(fontRendererObj, MovesUtils.getLocalizedMove(move.getName()), xOffset + 14, yOffset + 127,
                    move.getType(pokemob).colour);
            drawString(fontRendererObj, "" + pwr, xOffset + 102, yOffset + 127, 0xffffff);
        }

        move = MovesUtils.getMoveFromName(pokemob.getMove(3));

        if (move != null)
        {
            if (move.getPWR(pokemob, entityPlayer) > 0)
            {
                pwr = "" + move.getPWR(pokemob, entityPlayer);
            }
            else
            {
                pwr = "-";
            }
            drawString(fontRendererObj, MovesUtils.getLocalizedMove(move.getName()), xOffset + 14, yOffset + 141,
                    move.getType(pokemob).colour);
            drawString(fontRendererObj, "" + pwr, xOffset + 102, yOffset + 141, 0xffffff);
        }

        move = MovesUtils.getMoveFromName(pokemob.getMove(4));

        if (move != null)
        {
            if (move.getPWR(pokemob, entityPlayer) > 0)
            {
                pwr = "" + move.getPWR(pokemob, entityPlayer);
            }
            else
            {
                pwr = "-";
            }
            drawString(fontRendererObj, MovesUtils.getLocalizedMove(move.getName()), xOffset + 14, yOffset + 155,
                    move.getType(pokemob).colour);
            drawString(fontRendererObj, "" + pwr, xOffset + 102, yOffset + 155, 0xffffff);
        }
    }

    @Override
    public void drawWorldBackground(int tint)
    {
        super.drawWorldBackground(tint);
    }

    private int getButtonId(int x, int y)
    {
        int xConv = x - ((width - xSize) / 2) - 74;
        int yConv = y - ((height - ySize) / 2) - 107;
        int button = 0;

        if (xConv >= 37 && xConv <= 42 && yConv >= 63 && yConv <= 67)
        {
            button = 1;// Next
        }
        else if (xConv >= 25 && xConv <= 30 && yConv >= 63 && yConv <= 67)
        {
            button = 2;// Previous
        }
        else if (xConv >= 32 && xConv <= 36 && yConv >= 58 && yConv <= 63)
        {
            button = 3;// Next 10
        }
        else if (xConv >= 32 && xConv <= 36 && yConv >= 69 && yConv <= 73)
        {
            button = 4;// Previous 10
        }
        else if (xConv >= -65 && xConv <= -58 && yConv >= 65 && yConv <= 72)
        {
            button = 5;// Sound
        }
        else if (xConv >= 61 && xConv <= 67 && yConv >= 16 && yConv <= 27)
        {
            button = 6;// exchange Move 01
        }
        else if (xConv >= 61 && xConv <= 67 && yConv >= 30 && yConv <= 41)
        {
            button = 7;// exchange Move 12
        }
        else if (xConv >= 61 && xConv <= 67 && yConv >= 44 && yConv <= 55)
        {
            button = 8;// exchange Move 23
        }
        else if (xConv >= 61 && xConv <= 67 && yConv >= 57 && yConv <= 69)
        {
            button = 9;// exchange Move 34
        }
        else if (xConv >= 169 && xConv <= 175 && yConv <= 5 && yConv >= 0)
        {
            button = 13;// single dot button above bottom window.
        }
        else if (xConv >= -55 && xConv <= 30 && yConv >= -60 && yConv <= 15)
        {
            button = 10;// Rotate Mouse control
        }
        else if (xConv >= 167 && xConv <= 176 && yConv <= -53 && yConv >= -60)
        {
            button = 11;// swap page
        }
        else if (xConv >= 167 && xConv <= 176 && yConv <= -41 && yConv >= -46)
        {
            button = 12;// swap page
        }
        return button;
    }

    private EntityLiving getEntityToDisplay()
    {
        EntityLiving pokemob = entityToDisplayMap.get(pokedexEntry.getPokedexNb());

        if (pokemob == null)
        {
            // int entityId =
            // mod_Pokecube.getEntityIdFromPokedexNumber(pokedexEntry.getPokedexNb());
            pokemob = (EntityLiving) PokecubeMod.core.createEntityByPokedexNb(pokedexEntry.getPokedexNb(),
                    entityPlayer.worldObj);

            ((IPokemob) pokemob).specificSpawnInit();
            if (pokemob != null)
            {
                entityToDisplayMap.put(pokedexEntry.getPokedexNb(), pokemob);
            }
        }

        return pokemob;
    }

    public void handleGuiButton(int button)
    {
        if ((page != 2 || mode) && button == 1)
        {
            pokedexEntry = Pokedex.getInstance().getNext(pokedexEntry, 1);
        }
        else if ((page != 2 || mode) && button == 2)
        {
            pokedexEntry = Pokedex.getInstance().getPrevious(pokedexEntry, 1);
        }
        else if (page == 0 && button == 3)
        {
            pokedexEntry = Pokedex.getInstance().getNext(pokedexEntry, 10);
        }
        else if (page == 0 && button == 4)
        {
            pokedexEntry = Pokedex.getInstance().getPrevious(pokedexEntry, 10);
        }
        else if (page == 4 && button == 3)
        {
            nicknameTextField.setEnabled(true);
            nicknameTextField.setFocused(true);
            GuiTeleport.instance().nextMove();
            Minecraft minecraft = (Minecraft) PokecubeCore.getMinecraftInstance();
            List<TeleDest> locations = PokecubeSerializer.getInstance()
                    .getTeleports(minecraft.thePlayer.getUniqueID().toString());

            if (locations.size() > 0)
            {
                TeleDest location = locations.get((GuiTeleport.instance().indexLocation) % locations.size());
                nicknameTextField.setText(location.getName());
            }
            nicknameTextField.setEnabled(true);
        }
        else if (page == 4 && button == 4)
        {
            nicknameTextField.setEnabled(true);
            nicknameTextField.setFocused(true);
            GuiTeleport.instance().previousMove();
            Minecraft minecraft = (Minecraft) PokecubeCore.getMinecraftInstance();
            List<TeleDest> locations = PokecubeSerializer.getInstance()
                    .getTeleports(minecraft.thePlayer.getUniqueID().toString());

            if (locations.size() > 0)
            {
                TeleDest location = locations.get((GuiTeleport.instance().indexLocation) % locations.size());
                nicknameTextField.setText(location.getName());
            }
            nicknameTextField.setEnabled(true);
        }

        if (page == 0 && button >= 1 && button <= 5 || button == 5)
        {
            float volume = 0.2F;

            if (button == 5)
            {
                volume = 1F;
            }
            mc.thePlayer.playSound(pokedexEntry.getSoundEvent(), volume, 1.0F);

            nicknameTextField.setVisible(true);
            if (canEditPokemob())
            {
                nicknameTextField.setText(pokemob.getPokemonDisplayName().getFormattedText());
            }
            nicknameTextField.setEnabled(true);
        }
        else if (canEditPokemob() && page == 1)
        {
            if (button == 6)
            {
                pokemob.exchangeMoves(0, 1);
            }
            else if (button == 7)
            {
                pokemob.exchangeMoves(1, 2);
            }
            else if (button == 8)
            {
                pokemob.exchangeMoves(2, 3);
            }
            else if (button == 9)
            {
                pokemob.exchangeMoves(3, 4);
            }
            else if (button == 13)
            {
                pokemob.exchangeMoves(4, 5);
            }
        }
        else if (page == 4)
        {
            if (index > 4) index = 0;
            if (button == 6 && (index == 0 || index == 1))
            {
                index = index == 1 ? 0 : 1;
            }
            else if (button == 7 && (index == 1 || index == 2))
            {
                index = index == 1 ? 2 : 1;
            }
            else if (button == 8 && (index == 2 || index == 3))
            {
                index = index == 2 ? 3 : 2;
            }
            else if (button == 9 && (index == 4 || index == 3))
            {
                index = index == 3 ? 4 : 3;
            }
        }
        if (button == 11)
        {
            page = (page + 1) % PAGECOUNT;

            if (entityPlayer.getHeldItemMainhand() != null
                    && entityPlayer.getHeldItemMainhand().getItem() == PokecubeItems.pokedex)
            {
                // entityPlayer.getHeldItemMainhand().setItemDamage(page);
                PokecubeServerPacket packet = PokecubePacketHandler.makeServerPacket(PokecubeServerPacket.POKEDEX,
                        new byte[] { (byte) page });
                PokecubePacketHandler.sendToServer(packet);

                if (page == 2)
                {
                    TerrainSegment t = TerrainManager.getInstance().getTerrainForEntity(entityPlayer);
                    Vector3 location = Vector3.getNewVector().set(entityPlayer);
                    int type = t.getBiome(location);
                    for (int i = 0; i < biomes.size(); i++)
                    {
                        if (biomes.get(i) == (type))
                        {
                            index2 = i;
                        }
                    }
                    nicknameTextField.setText("");
                    nicknameTextField.setEnabled(false);
                }
            }

        }
        if (button == 12)
        {
            page = (page - 1) % PAGECOUNT;
            if (page < 0) page = PAGECOUNT - 1;

            if (entityPlayer.getHeldItemMainhand() != null
                    && entityPlayer.getHeldItemMainhand().getItem() == PokecubeItems.pokedex)
            {
                // entityPlayer.getHeldItemMainhand().setItemDamage(page);
                PokecubeServerPacket packet = PokecubePacketHandler.makeServerPacket((byte) 5,
                        new byte[] { (byte) page });
                PokecubePacketHandler.sendToServer(packet);

                if (page == 1)
                {
                    TerrainSegment t = TerrainManager.getInstance().getTerrainForEntity(entityPlayer);
                    Vector3 location = Vector3.getNewVector().set(entityPlayer);
                    int type = t.getBiome(location.intX(), location.intY(), location.intZ());
                    for (int i = 0; i < biomes.size(); i++)
                    {
                        if (biomes.get(i) == (type))
                        {
                            index2 = i - 1;// type - 1;
                        }
                    }
                }
            }
        }
        if (button == 13) mode = !mode;

        if (page != 0)
        {

            if (page == 1)
            {

            }
            if (page != 4)
            {
                nicknameTextField.setText("");
                nicknameTextField.setEnabled(false);
                if (button == 1)
                {
                    index2++;
                }
                if (button == 2)
                {
                    index2--;
                }
                if (button == 3)
                {
                    index--;
                }
                if (button == 4)
                {
                    index++;
                }
            }
        }
    }

    @Override
    public void handleMouseInput() throws IOException
    {
        int x = Mouse.getEventX() * this.width / this.mc.displayWidth;
        int y = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
        this.handleMouseMove(x, y, Mouse.getEventButton());
        super.handleMouseInput();
    }

    private void handleMouseMove(int x, int y, int mouseButton)
    {
        // System.out.println("handleMouseMove("+x+", "+y+",
        // "+mouseButton+")");
        if (mouseButton != -1)
        {
            mouseRotateControl = -1;
        }

        if (mouseRotateControl == 0)
        {
            prevX = x;
            xRenderAngle += y - prevY;
            prevY = y;

            if (xRenderAngle > 20)
            {
                xRenderAngle = 20;
            }

            if (xRenderAngle < -30)
            {
                xRenderAngle = -30;
            }
        }

        if (mouseRotateControl == 1)
        {
            yHeadRenderAngle += (prevX - x) * 2;
            prevX = x;
            xHeadRenderAngle += y - prevY;
            prevY = y;

            if (xHeadRenderAngle > 20)
            {
                xHeadRenderAngle = 20;
            }

            if (xHeadRenderAngle < -30)
            {
                xHeadRenderAngle = -30;
            }

            if (yHeadRenderAngle > 40)
            {
                yHeadRenderAngle = 40;
            }

            if (yHeadRenderAngle < -40)
            {
                yHeadRenderAngle = -40;
            }
        }
    }

    @Override
    public void initGui()
    {
        super.initGui();

        buttonList.clear();
        int yOffset = height / 2 - 80;
        int xOffset = width / 2;
        nicknameTextField = new GuiTextField(0, fontRendererObj, xOffset + 11, yOffset + 14, 110, 10);
        nicknameTextField.setMaxStringLength(20);
        nicknameTextField.setFocused(false);
        nicknameTextField.setEnabled(false);

        if (canEditPokemob() && page == 0)
        {
            String name = pokemob.getPokemonDisplayName().getUnformattedComponentText().trim();
            nicknameTextField.setText(name);
            oldName = nicknameTextField.getText();
            nicknameTextField.setEnabled(true);
        }

        if (page == 2)
        {
            TerrainSegment t = TerrainManager.getInstance().getTerrainForEntity(entityPlayer);
            Vector3 location = Vector3.getNewVector().set(entityPlayer);
            int type = t.getBiome(location);
            for (int i = 0; i < biomes.size(); i++)
            {
                if (biomes.get(i) == (type))
                {
                    index2 = i;
                }
            }
            nicknameTextField.setText("");
            nicknameTextField.setEnabled(false);
        }
        if (page == 4)
        {
            Minecraft minecraft = (Minecraft) PokecubeCore.getMinecraftInstance();
            List<TeleDest> locations = PokecubeSerializer.getInstance()
                    .getTeleports(minecraft.thePlayer.getUniqueID().toString());

            if (locations.size() > 0)
            {
                TeleDest location = locations.get((GuiTeleport.instance().indexLocation) % locations.size());
                nicknameTextField.setText(location.getName());
            }
            nicknameTextField.setEnabled(true);
        }
    }

    @Override
    protected void keyTyped(char par1, int par2) throws IOException
    {
        if (isAltKeyDown()) return;
        if (page == 4)
        {
            nicknameTextField.setEnabled(true);
            nicknameTextField.setFocused(true);

            if ((par2 == ClientProxyPokecube.nextMove.getKeyCode()
                    || par2 == ClientProxyPokecube.previousMove.getKeyCode()))
            {
                GuiTeleport.instance().nextMove();

                Minecraft minecraft = (Minecraft) PokecubeCore.getMinecraftInstance();
                List<TeleDest> locations = PokecubeSerializer.getInstance()
                        .getTeleports(minecraft.thePlayer.getUniqueID().toString());

                if (locations.size() > 0)
                {
                    TeleDest location = locations.get((GuiTeleport.instance().indexLocation) % locations.size());
                    nicknameTextField.setText(location.getName());
                }
                nicknameTextField.setEnabled(true);

            }
            else if (par2 == 28 && index == 0 || index == 4)
            {

                Minecraft minecraft = (Minecraft) PokecubeCore.getMinecraftInstance();
                List<TeleDest> locations = PokecubeSerializer.getInstance()
                        .getTeleports(minecraft.thePlayer.getUniqueID().toString());

                if (locations.size() > 0)
                {
                    PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
                    Vector4 location = locations.get((GuiTeleport.instance().indexLocation) % locations.size()).loc;
                    if (index == 0) buffer.writeByte(-1);
                    else if (index == 4) buffer.writeByte(-2);
                    buffer.writeInt((int) location.w);
                    buffer.writeFloat(location.x);
                    buffer.writeFloat(location.y);
                    buffer.writeFloat(location.z);

                    buffer.writeString(nicknameTextField.getText());

                    PokecubeSerializer.getInstance().unsetTeleport(location,
                            minecraft.thePlayer.getUniqueID().toString());
                    PokecubeServerPacket packet = PokecubePacketHandler.makeServerPacket(PokecubeServerPacket.POKEDEX,
                            buffer.array());
                    PokecubePacketHandler.sendToServer(packet);
                }
            }
        }
        // else
        {
            boolean b = nicknameTextField.textboxKeyTyped(par1, par2);

            if (par2 == Keyboard.KEY_LEFT)
            {
                handleGuiButton(2);
            }
            else if (par2 == Keyboard.KEY_RIGHT)
            {
                handleGuiButton(1);
            }
            else if (par2 == Keyboard.KEY_UP)
            {
                handleGuiButton(3);
            }
            else if (par2 == Keyboard.KEY_DOWN)
            {
                handleGuiButton(4);
            }
            else if (par2 == Keyboard.KEY_PRIOR)
            {
                handleGuiButton(11);
            }
            else if (par2 == Keyboard.KEY_NEXT)
            {
                handleGuiButton(12);
            }
            else if (par2 != 54 && par2 != 58 && par2 != 42 && page == 0 && !b)
            {
                mc.displayGuiScreen(null);
                mc.setIngameFocus();

                String nickname = nicknameTextField.getText();
                if (canEditPokemob() && page == 0 && !oldName.equals(nickname))
                {
                    pokemob.setPokemonNickname(nickname);
                }
                return;
            }

            super.keyTyped(par1, par2);
        }
    }

    /** Called when the mouse is clicked. */
    @Override
    protected void mouseClicked(int x, int y, int mouseButton)
    {
        nicknameTextField.mouseClicked(x, y, mouseButton);
        // System.out.println(mouseButton);
        int button = getButtonId(x, y);

        if (button != 0)
        {
            mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }

        if (button == 10)
        {
            mouseRotateControl = mouseButton;
            prevX = x;
            prevY = y;
        }
        else
        {
            handleGuiButton(button);
        }

        if (page == 0)
        {
            if (!canEditPokemob())
            {
                nicknameTextField.setText("");
                nicknameTextField.setEnabled(false);
            }
            else
            {
                nicknameTextField.setEnabled(true);
                String name = pokemob.getPokemonDisplayName().getUnformattedComponentText().trim();
                nicknameTextField.setText(name);
            }
        }
    }

    private void renderMob()
    {
        try
        {
            EntityLiving entity = getEntityToDisplay();

            float size = 0;
            int j = 0;
            int k = 0;

            IPokemob pokemob = null;
            if (entity instanceof IPokemob)
            {
                pokemob = (IPokemob) entity;
            }

            if (!mc.thePlayer.getStatFileWriter()
                    .hasAchievementUnlocked(PokecubeMod.pokemobAchievements.get(pokedexEntry.getPokedexNb()))
                    && !mc.thePlayer.capabilities.isCreativeMode)
            {

                if (entity instanceof IPokemob)
                {
                    IPokemob mob = (IPokemob) entity;
                    mob.setSize(1);
                    mob.setShiny(false);
                }
                if (entity instanceof IMobColourable) ((IMobColourable) entity).setRGBA(0, 0, 0, 255);
            }
            else
            {
                if (entity instanceof IPokemob)
                {
                    IPokemob mob = (IPokemob) entity;
                    mob.setSize(1);
                    mob.setShiny(false);
                }
                if (entity instanceof IMobColourable) ((IMobColourable) entity).setRGBA(255, 255, 255, 255);
            }

            pokemob.setPokemonAIState(IMoveConstants.EXITINGCUBE, false);

            size = Math.max(entity.width, entity.height);
            j = (width - xSize) / 2;
            k = (height - ySize) / 2;

            GL11.glPushMatrix();
            GL11.glTranslatef(j + 60, k + 100, 50F);
            float zoom = (float) (30F / Math.sqrt(size + 0.6));
            GL11.glScalef(-zoom, zoom, zoom);
            GL11.glRotatef(180F, 0.0F, 0.0F, 1.0F);
            float f5 = ((k + 75) - 50) - ySize;
            GL11.glRotatef(135F, 0.0F, 1.0F, 0.0F);

            GL11.glRotatef(-135F, 0.0F, 1.0F, 0.0F);
            GL11.glRotatef(-(float) Math.atan(f5 / 40F) * 20F, 1.0F, 0.0F, 0.0F);
            entity.renderYawOffset = 0F;
            entity.rotationYaw = yHeadRenderAngle;
            entity.rotationPitch = xHeadRenderAngle;
            entity.rotationYawHead = entity.rotationYaw;
            GL11.glTranslatef(0.0F, (float) entity.getYOffset(), 0.0F);

            entity.setPosition(entityPlayer.posX, entityPlayer.posY + 1, entityPlayer.posZ);

            entity.limbSwing = 0;
            entity.limbSwingAmount = 0;
            entity.prevLimbSwingAmount = 0;
            entity.onGround = ((IPokemob) entity).getType1() != flying && ((IPokemob) entity).getType2() != flying;

            if (isAltKeyDown())
            {
                entity.onGround = true;
                entity.limbSwingAmount = 0.05f;
                entity.prevLimbSwingAmount = entity.limbSwingAmount - 0.5f;
            }
            int i = 15728880;
            int j2 = i % 65536;
            int k2 = i / 65536;
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, j2 / 1.0F, k2 / 1.0F);

            GL11.glEnable(GL12.GL_RESCALE_NORMAL);
            GL11.glEnable(GL11.GL_COLOR_MATERIAL);
            RenderHelper.enableStandardItemLighting();
            Minecraft.getMinecraft().getRenderManager().doRenderEntity(entity, 0, 0, 0, 1, POKEDEX_RENDER, false);
            RenderHelper.disableStandardItemLighting();
            GL11.glDisable(GL12.GL_RESCALE_NORMAL);

            GL11.glPopMatrix();

        }
        catch (Throwable e)
        {
            e.printStackTrace();
        }
    }
}
