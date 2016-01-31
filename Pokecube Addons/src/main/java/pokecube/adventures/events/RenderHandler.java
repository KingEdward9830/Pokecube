package pokecube.adventures.events;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import baubles.common.container.InventoryBaubles;
import baubles.common.lib.PlayerHandler;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.client.event.EntityViewRenderEvent.RenderFogEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent.KeyInputEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import pokecube.adventures.blocks.cloner.ContainerCloner;
import pokecube.adventures.client.ClientProxy;
import pokecube.adventures.handlers.PlayerAsPokemobManager;
import pokecube.adventures.handlers.TeamManager;
import pokecube.adventures.items.bags.ItemBag;
import pokecube.adventures.network.PacketPokeAdv;
import pokecube.adventures.network.PacketPokeAdv.MessageServer;
import pokecube.core.client.ClientProxyPokecube;
import pokecube.core.interfaces.IPokemob;
import pokecube.core.interfaces.PokecubeMod;
import pokecube.core.items.pokemobeggs.ItemPokemobEgg;
import pokecube.core.utils.ChunkCoordinate;
import thut.api.maths.Vector3;
import thut.api.terrain.TerrainManager;
import thut.api.terrain.TerrainSegment;
import vazkii.botania.api.item.IBaubleRender;
import vazkii.botania.api.item.IBaubleRender.RenderType;
import vazkii.botania.api.item.ICosmeticAttachable;

@SideOnly(Side.CLIENT)
public class RenderHandler
{

    public static float   partialTicks = 0.0F;
    public static boolean BOTANIA      = false;

    public RenderHandler()
    {
    }

    @SubscribeEvent
    public void onPlayerRender(RenderPlayerEvent.Post event)
    {

        if (BOTANIA) { return; }

        EntityPlayer player = event.entityPlayer;
        InventoryBaubles inv = PlayerHandler.getPlayerBaubles(player);

        GL11.glPushMatrix();
        float yaw = 180;
        float yawOffset = player.prevRenderYawOffset
                + (player.renderYawOffset - player.prevRenderYawOffset) * event.partialRenderTick;
        yaw += yawOffset;

        if (player != Minecraft.getMinecraft().thePlayer)
        {
            Vector3 source = Vector3.getNewVectorFromPool().set(Minecraft.getMinecraft().thePlayer);
            Vector3 target = Vector3.getNewVectorFromPool().set(player);

            source.set(target.subtract(source));

            GL11.glTranslated(source.x, source.y, source.z);
            // Clear out the jitteryness from rendering
            // TODO get this interpolated correctly, it needs to be done for
            // both player's movement.
            // source.x = player.prevPosX - player.posX;
            // source.y = player.prevPosY - player.posY;
            // source.z = player.prevPosZ - player.posZ;
            //
            // source.scalarMultBy(event.partialRenderTick);
            // GL11.glTranslated(source.x, source.y, source.z);
        }
        GL11.glRotatef(-yaw, 0, 1, 0);
        GL11.glTranslated(0, 1.4, 0);

        StackTraceElement[] trace = Thread.currentThread().getStackTrace();
        boolean loop = false;
        for (int i = 2; i < trace.length; i++)
        {
            if (trace[i].getClassName().toLowerCase().contains("pokedex"))
            {
                loop = true;
                break;
            }
        }
        if (loop)
        {
            GL11.glTranslated(-1, 0.35, 2);
        }

        GL11.glRotatef(180, 0, 0, 1);

        dispatchRenders(inv, event, RenderType.BODY, event.partialRenderTick);
        GL11.glPopMatrix();

        yaw = player.prevRotationYawHead
                + (player.rotationYawHead - player.prevRotationYawHead) * event.partialRenderTick;
        yawOffset = player.prevRenderYawOffset
                + (player.renderYawOffset - player.prevRenderYawOffset) * event.partialRenderTick;
        float pitch = player.prevRotationPitch
                + (player.rotationPitch - player.prevRotationPitch) * event.partialRenderTick;

        GL11.glPushMatrix();
        GL11.glRotatef(yawOffset, 0, -1, 0);
        GL11.glRotatef(yaw - 270, 0, 1, 0);
        GL11.glRotatef(pitch, 0, 0, 1);
        dispatchRenders(inv, event, RenderType.HEAD, event.partialRenderTick);

        GL11.glPopMatrix();
    }

    @SubscribeEvent
    public void keyInput(KeyInputEvent evt)
    {
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        InventoryBaubles inv = PlayerHandler.getPlayerBaubles(player);
        boolean bag = false;
        for (int i = 0; i < inv.getSizeInventory(); i++)
        {
            ItemStack stack = inv.getStackInSlot(i);
            if (stack != null)
            {
                Item item = stack.getItem();
                if (item instanceof ItemBag)
                {
                    bag = true;
                    break;
                }
            }
        }
        if (bag && Keyboard.getEventKey() == ClientProxy.bag.getKeyCode())
        {
            PacketPokeAdv.sendBagOpenPacket(false, Vector3.empty);
        }

        IPokemob entity = PlayerAsPokemobManager.getInstance().getTransformed(player);
        if (entity != null && Keyboard.getEventKey() == ClientProxyPokecube.mobAttack.getKeyCode())
        {
            Vector3 here = Vector3.getNewVectorFromPool().set(player, false);
            Entity hit = here.firstEntityExcluding(16, Vector3.getNewVectorFromPool().set(player.getLookVec()),
                    player.worldObj, false, player);
            if (hit != null)
            {
                PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
                buffer.writeByte(10);
                buffer.writeInt(hit.getEntityId());
                MessageServer message = new MessageServer(buffer);
                PokecubeMod.packetPipeline.sendToServer(message);
            }
        }
    }

    @SubscribeEvent
    public void onToolTip(ItemTooltipEvent evt)
    {
        EntityPlayer player = evt.entityPlayer;
        ItemStack stack = evt.itemStack;
        if (player == null || player.openContainer == null || stack == null) return;
        if (player.openContainer instanceof ContainerCloner && stack.getItem() instanceof ItemPokemobEgg)
        {
            if (stack.hasTagCompound() && stack.getTagCompound().hasKey("ivs"))
            {
                evt.toolTip.add("" + stack.getTagCompound().getLong("ivs") + ":"
                        + stack.getTagCompound().getFloat("size") + ":" + stack.getTagCompound().getByte("nature"));
            }
        }
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void ClientRenderTick(RenderFogEvent evt)
    {
        if (TeamEventsHandler.shouldRenderVolume)
        {
            GL11.glPushMatrix();
            EntityPlayer player = Minecraft.getMinecraft().thePlayer;
            TerrainSegment t = TerrainManager.getInstance().getTerrainForEntity(player);
            Vector3 v = Vector3.getNewVectorFromPool();// .set(evt.entity);
            Vector3 source = Vector3.getNewVectorFromPool().set(player);
            Vector3 target = t.getCentre();
            source.set(target.subtract(source));
            Vector3 diff = Vector3.getNewVectorFromPool();
            diff.x = player.prevPosX - player.posX;
            diff.y = player.prevPosY - player.posY;
            diff.z = player.prevPosZ - player.posZ;
            diff.scalarMultBy(evt.renderPartialTicks);
            source.addTo(diff);
            GL11.glTranslated(source.x, source.y, source.z);
            int rgba = 0xFFFFFFFF;
            ChunkCoordinate c = ChunkCoordinate.getChunkCoordFromWorldCoord(t.getCentre().getPos(), player.dimension);
            if (TeamManager.getInstance().isOwned(c))
            {
                if (!TeamManager.getInstance().isTeamLand(c, player.getTeam().getRegisteredName()))
                {
                    rgba = 0xFFFF0000;
                }
                else
                {
                    rgba = 0xFF00FF00;
                }
            }
            renderDebugBoundingBox(v.getAABB().expand(8, 4, 8), rgba);
            renderDebugBoundingBox(v.getAABB().expand(8, 8, 8), rgba);
            renderDebugBoundingBox(v.getAABB().expand(4, 8, 8), rgba);
            renderDebugBoundingBox(v.getAABB().expand(8, 8, 4), rgba);
            renderDebugBoundingBox(v.getAABB().expand(8, 8, 0), rgba);
            renderDebugBoundingBox(v.getAABB().expand(0, 8, 8), rgba);
            renderDebugBoundingBox(v.getAABB().expand(8, 0, 8), rgba);
            GL11.glPopMatrix();
        }
    }

    /** Renders the bounding box around an entity when F3+B is pressed */
    private void renderDebugBoundingBox(AxisAlignedBB box, int rgba)
    {
        GlStateManager.depthMask(false);
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.disableBlend();
        int alpha = ((rgba >> 24) & 255);
        int red = ((rgba >> 16) & 255);
        int green = ((rgba >> 8) & 255);
        int blue = (rgba & 255);
        RenderGlobal.drawOutlinedBoundingBox(box, red, green, blue, alpha);
        GlStateManager.enableTexture2D();
        GlStateManager.enableLighting();
        GlStateManager.enableCull();
        GlStateManager.disableBlend();
        GlStateManager.depthMask(true);
    }

    private void dispatchRenders(InventoryBaubles inv, RenderPlayerEvent event, RenderType type, float partialTick)
    {
        for (int i = 0; i < inv.getSizeInventory(); i++)
        {
            ItemStack stack = inv.getStackInSlot(i);
            if (stack != null)
            {
                Item item = stack.getItem();

                if (item instanceof ICosmeticAttachable)
                {
                    ICosmeticAttachable attachable = (ICosmeticAttachable) item;
                    ItemStack cosmetic = attachable.getCosmeticItem(stack);
                    if (cosmetic != null)
                    {
                        GL11.glPushMatrix();
                        GL11.glColor4f(1F, 1F, 1F, 1F);
                        ((IBaubleRender) cosmetic.getItem()).onPlayerBaubleRender(cosmetic, event.entityPlayer, type, partialTick);
                        GL11.glPopMatrix();
                        continue;
                    }
                }

                if (item instanceof IBaubleRender)
                {
                    GL11.glPushMatrix();
                    GL11.glColor4f(1F, 1F, 1F, 1F);
                    ((IBaubleRender) stack.getItem()).onPlayerBaubleRender(stack, event.entityPlayer, type, partialTick);
                    GL11.glPopMatrix();
                }
            }
        }
    }
}
