package pokecube.core.moves.animations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.lwjgl.opengl.GL11;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.EntityViewRenderEvent.RenderFogEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent.Unload;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import pokecube.core.interfaces.IMoveAnimation.MovePacketInfo;
import pokecube.core.interfaces.Move_Base;
import thut.api.maths.Vector3;

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

    HashMap<Entity, HashSet<MoveAnimation>> moves = new HashMap<Entity, HashSet<MoveAnimation>>();

    public void addMove(Entity attacker, MoveAnimation move)
    {
        HashSet<MoveAnimation> moves = this.moves.get(attacker);
        if (moves == null)
        {
            moves = new HashSet<MoveAnimation>();
            this.moves.put(attacker, moves);
        }
        moves.add(move);
    }

    public void clear()
    {
        moves.clear();
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void onRenderWorldPost(RenderFogEvent event)
    {
        try
        {
            GL11.glPushMatrix();
            ArrayList<Entity> entities = Lists.newArrayList(moves.keySet());
            for (Entity e : entities)
            {
                if (!moves.containsKey(e)) continue;
                HashSet<MoveAnimation> moves = Sets.newHashSet(this.moves.get(e));

                for (MoveAnimation move : moves)
                {
                    Vector3 target = Vector3.getNewVector().set(move.targetLoc);
                    EntityPlayer player = Minecraft.getMinecraft().thePlayer;
                    Vector3 source = Vector3.getNewVector().set(player);
                    GL11.glPushMatrix();
                    source.set(target.subtract(source));

                    GL11.glTranslated(source.x, source.y, source.z);
                    // Clear out the jitteryness from rendering
                    source.x = player.prevPosX - player.posX;
                    source.y = player.prevPosY - player.posY;
                    source.z = player.prevPosZ - player.posZ;
                    source.scalarMultBy(event.getRenderPartialTicks());
                    GL11.glTranslated(source.x, source.y, source.z);
                    // TODO see about fixing the slight movement that occurs
                    // when the player stops or starts moving

                    move.render(event.getRenderPartialTicks());
                    GL11.glPopMatrix();
                }

            }
            GL11.glPopMatrix();
            for (Object e : moves.keySet())
            {
                HashSet<MoveAnimation> moves = this.moves.get(e);
                for (MoveAnimation move : moves)
                {
                    if (move.lastDrop != event.getEntity().getEntityWorld().getTotalWorldTime())
                    {
                        move.duration--;
                        move.lastDrop = event.getEntity().getEntityWorld().getTotalWorldTime();
                    }
                }
            }
            HashSet<Object> toRemove = new HashSet<Object>();
            for (Object e : moves.keySet())
            {
                HashSet<MoveAnimation> moves = this.moves.get(e);
                HashSet<MoveAnimation> remove = new HashSet<MoveAnimation>();
                for (MoveAnimation move : moves)
                {
                    if (move.duration < 0)
                    {
                        remove.add(move);
                    }
                }
                moves.removeAll(remove);
                if (moves.size() == 0) toRemove.add(e);
            }
            for (Object o : toRemove)
            {
                moves.remove(o);
            }
        }
        catch (Throwable e)
        {
            e.printStackTrace();
        }
    }

    @SubscribeEvent
    public void WorldUnloadEvent(Unload evt)
    {
        if (evt.getWorld().provider.getDimension() == 0 && FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT)
        {
            clear();
        }

    }
}
