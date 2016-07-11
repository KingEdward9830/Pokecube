package pokecube.core.moves.animations;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.EntityViewRenderEvent.RenderFogEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent.Unload;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import pokecube.core.interfaces.IMoveAnimation.MovePacketInfo;
import pokecube.core.moves.PokemobTerrainEffects;
import pokecube.core.interfaces.Move_Base;
import thut.api.maths.Vector3;
import thut.api.terrain.TerrainManager;
import thut.api.terrain.TerrainSegment;

public class MoveAnimationHelper
{

    public static class MoveAnimation
    {
        public final Entity    attacker;
        public final Entity    targetEnt;
        public final Vector3   targetLoc;
        public final Vector3   sourceStart;
        public final Move_Base move;
        public int             duration;
        public long            lastDrop;
        final MovePacketInfo   info;

        public MoveAnimation(Entity attacker, Entity targetEnt, Vector3 targetLoc, Move_Base move, int time)
        {
            this.attacker = attacker;
            this.targetEnt = targetEnt;
            this.targetLoc = targetLoc;
            this.sourceStart = Vector3.getNewVector().set(attacker).addTo(0, attacker.getEyeHeight(), 0);
            this.move = move;
            info = new MovePacketInfo(move, attacker, targetEnt, sourceStart, targetLoc);
            duration = time;
        }

        public void render(double partialTick)
        {
            if (move.getAnimation() != null)
            {
                info.currentTick = move.getAnimation().getDuration() - duration;
                move.getAnimation().clientAnimation(info, Minecraft.getMinecraft().renderGlobal, (float) partialTick);
            }
            else
            {
                throw (new NullPointerException("Who Registered null animation for " + move.name));
            }
        }
    }

    private static MoveAnimationHelper instance;

    public static MoveAnimationHelper Instance()
    {
        if (instance == null)
        {
            instance = new MoveAnimationHelper();
            MinecraftForge.EVENT_BUS.register(instance);
        }
        return instance;
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void onRenderWorldPost(RenderFogEvent event)
    {
        try
        {
            EntityPlayer player = Minecraft.getMinecraft().thePlayer;
            Vector3 source = Vector3.getNewVector().set(player);
            GL11.glPushMatrix();
            for (int i = -4; i <= 4; i++)
            {
                for (int j = -4; j <= 4; j++)
                {
                    for (int k = -4; k <= 4; k++)
                    {
                        source.set(player);
                        TerrainSegment segment = TerrainManager.getInstance().getTerrain(player.getEntityWorld(),
                                player.posX + i * 16, player.posY + j * 16, player.posZ + k * 16);

                        PokemobTerrainEffects teffect = (PokemobTerrainEffects) segment
                                .geTerrainEffect("pokemobEffects");
                        if (teffect == null) continue;

                        Vector3 target = Vector3.getNewVector().set(segment.getCentre());
                        GL11.glPushMatrix();
                        source.set(target.subtract(source));

                        GL11.glTranslated(source.x, source.y, source.z);
                        // Clear out the jitteryness from rendering
                        source.x = player.prevPosX - player.posX;
                        source.y = player.prevPosY - player.posY;
                        source.z = player.prevPosZ - player.posZ;
                        source.scalarMultBy(event.getRenderPartialTicks());
                        GL11.glTranslated(source.x, source.y, source.z);
                        teffect.renderTerrainEffects(event);
                        GL11.glPopMatrix();
                    }
                }
            }
            GL11.glPopMatrix();
        }
        catch (Throwable e)
        {
            e.printStackTrace();
        }
    }

    @SubscribeEvent
    public void WorldUnloadEvent(Unload evt)
    {
    }
}
