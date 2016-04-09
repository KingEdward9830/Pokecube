/**
 *
 */
package pokecube.core.moves.templates;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import pokecube.core.PokecubeCore;
import pokecube.core.database.abilities.Ability;
import pokecube.core.interfaces.IMoveConstants;
import pokecube.core.interfaces.IPokemob;
import pokecube.core.interfaces.IPokemob.MovePacket;
import pokecube.core.interfaces.Move_Base;
import pokecube.core.interfaces.PokecubeMod;
import pokecube.core.moves.MovesUtils;
import pokecube.core.moves.animations.Thunder;
import pokecube.core.network.PokecubePacketHandler;
import pokecube.core.network.PokecubePacketHandler.PokecubeClientPacket;
import pokecube.core.utils.PokeType;
import thut.api.maths.Vector3;

/** @author Manchou */
public class Move_Basic extends Move_Base implements IMoveConstants
{
    protected static ItemStack createStackedBlock(IBlockState state)
    {
        int i = 0;
        Item item = Item.getItemFromBlock(state.getBlock());

        if (item != null && item.getHasSubtypes())
        {
            i = state.getBlock().getMetaFromState(state);
        }
        return new ItemStack(item, 1, i);
    }

    protected static boolean shouldSilk(IPokemob pokemob)
    {
        if (pokemob.getAbility() == null) return false;
        Ability ability = pokemob.getAbility();
        return pokemob.getLevel() > 90 && ability.toString().equalsIgnoreCase("hypercutter");
    }

    protected static void silkHarvest(IBlockState state, BlockPos pos, World worldIn, EntityPlayer player)
    {
        java.util.ArrayList<ItemStack> items = new java.util.ArrayList<ItemStack>();
        ItemStack itemstack = createStackedBlock(state);

        if (itemstack != null)
        {
            items.add(itemstack);
        }

        net.minecraftforge.event.ForgeEventFactory.fireBlockHarvesting(items, worldIn, pos, worldIn.getBlockState(pos),
                0, 1.0f, true, player);
        for (ItemStack stack : items)
        {
            Block.spawnAsEntity(worldIn, pos, stack);
        }
    }

    Vector3 v  = Vector3.getNewVector();

    Vector3 v1 = Vector3.getNewVector();

    /** Constructor for a Pokemob move. <br/>
     * The attack category defines the way the mob will move in order to make
     * its attack.
     *
     * @param name
     *            the English name of the attack, used as identifier and
     *            translation key
     * @param attackCategory
     *            can be either {@link MovesUtils#CATEGORY_CONTACT} or
     *            {@link MovesUtils#CATEGORY_DISTANCE} */
    public Move_Basic(String name)
    {
        super(name);
    }

    @Override
    public void attack(IPokemob attacker, Entity attacked, float f)
    {
        if (attacker.getStatus() == STATUS_SLP)
        {
            MovesUtils.displayStatusMessages(attacker, attacked, STATUS_SLP, false);
            return;
        }

        if (attacker.getStatus() == STATUS_FRZ)
        {
            MovesUtils.displayStatusMessages(attacker, attacked, STATUS_FRZ, false);
            return;
        }

        if (attacker.getStatus() == STATUS_PAR && Math.random() > 0.75)
        {
            MovesUtils.displayStatusMessages(attacker, attacked, STATUS_PAR, false);
            return;
        }

        if ((getAttackCategory() & CATEGORY_CONTACT) != 0)
        {
            if (MovesUtils.contactAttack(attacker, attacked, f))
            {
                finalAttack(attacker, attacked, f);
            }
        }
        else if ((getAttackCategory() & CATEGORY_DISTANCE) != 0)
        {
            finalAttack(attacker, attacked, f);
        }
        else if ((getAttackCategory() & CATEGORY_SELF) != 0)
        {
            doSelfAttack(attacker, f);
        }
    }

    @Override
    public void attack(IPokemob attacker, Vector3 location, float f)
    {
        int finalAttackStrength = 0;
        List<Entity> targets = new ArrayList<Entity>();

        Entity entity = (Entity) attacker;

        if (!move.notIntercepable)
        {
            Vec3d loc1 = new Vec3d(entity.posX, entity.posY + entity.getEyeHeight(), entity.posZ);
            Vec3d loc2 = new Vec3d(location.x, location.y, location.z);
            RayTraceResult pos = entity.worldObj.rayTraceBlocks(loc1, loc2, false);
            if (pos != null)
            {
                location.set(pos.hitVec);
            }

        }
        if (move.multiTarget)
        {
            targets.addAll(MovesUtils.targetsHit(((Entity) attacker), location));
        }
        else if (!move.notIntercepable)
        {
            targets.add(MovesUtils.targetHit(entity, location));
        }
        else
        {
            List<Entity> subTargets = new ArrayList<Entity>();
            if (subTargets.contains(attacker)) subTargets.remove(attacker);

            targets.addAll(subTargets);
        }

        if ((getAttackCategory() & CATEGORY_SELF) == 0)
        {
            int n = targets.size();
            notifyClient((Entity) attacker, location, null);
            if (n > 0)
            {
                for (Entity e : targets)
                {
                    if (e != null) finalAttack(attacker, e, finalAttackStrength);
                }
            }
            else
            {
                MovesUtils.displayMoveMessages(attacker, null, this.name);
                MovesUtils.displayEfficiencyMessages(attacker, null, -1, 0);
            }

        }
        else if ((getAttackCategory() & CATEGORY_SELF) != 0)
        {
            doSelfAttack(attacker, f);
        }
        doWorldAction(attacker, location);
    }

    @Override
    public boolean doAttack(IPokemob attacker, Entity attacked, float f)
    {
        if (attacked == null && getAttackCategory() != CATEGORY_SELF) return false;

        if (getAttackCategory() == CATEGORY_SELF && this.hasStatModSelf)
        {
            return attacker.getMoveStats().SELFRAISECOUNTER == 0;
        }
        else if (this.hasStatModTarget && f == 0) return attacker.getMoveStats().TARGETLOWERCOUNTER == 0;

        return true;
    }

    /** Do anything special for self attacks, usually raising/lowering of stats.
     * 
     * @param mob */
    @Override
    public void doSelfAttack(IPokemob mob, float f)
    {
        if (doAttack(mob, (Entity) mob, f))
        {
            MovesUtils.displayMoveMessages(mob, (Entity) mob, name);
            if (sound != null)
            {
                ((Entity) mob).playSound(sound, 0.5F, 1F / (MovesUtils.rand.nextFloat() * 0.4F + 0.8F));
            }
            Vector3 v = Vector3.getNewVector().set(mob);
            notifyClient((Entity) mob, v, (Entity) mob);
            MovesUtils.attack(
                    new MovePacket(mob, (Entity) mob, name, move.type, getPWR(), move.crit, (byte) 0, (byte) 0, false));
            postAttack(mob, (Entity) mob, f, 0);
        }
    }

    @Override
    public void doWorldAction(IPokemob attacker, Vector3 location)
    {
        if (!PokecubeMod.pokemobsDamageBlocks) return;
        World world = ((Entity) attacker).worldObj;
        IBlockState state = location.getBlockState(world);
        Block block = state.getBlock();
        if (getType(attacker) == PokeType.ice && (move.attackCategory & CATEGORY_DISTANCE) > 0 && move.power > 0)
        {
            if (block.isAir(state, world, location.getPos()))
            {
                if (location.offset(EnumFacing.DOWN).getBlockState(world).isNormalCube())
                {
                    try
                    {
                        world.setBlockState(location.getPos(), Blocks.snow_layer.getDefaultState(), 2);
                    }
                    catch (Exception e)
                    {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
            else if (block == Blocks.water && state.getValue(BlockLiquid.LEVEL) == 0)
            {
                location.setBlock(world, Blocks.ice.getDefaultState());
            }
            else if (block.isReplaceable(world, location.getPos()))
            {
                if (location.offset(EnumFacing.DOWN).getBlockState(world).isNormalCube())
                    location.setBlock(world, Blocks.snow_layer.getDefaultState());
            }
        }
        int strong = 100;
        if (getType(attacker) == PokeType.water && getPWR() >= strong)
        {
            Vector3 nextBlock = Vector3.getNewVector().set(attacker).subtractFrom(location).reverse().norm()
                    .addTo(location);
            IBlockState nextState = nextBlock.getBlockState(world);
            if (block == Blocks.lava)
            {
                location.setBlock(world, Blocks.obsidian);
            }
            else if (block.isReplaceable(world, location.getPos()) && nextState.getBlock() == Blocks.lava)
            {
                nextBlock.setBlock(world, Blocks.obsidian);
            }
        }
        if (getType(attacker) == PokeType.electric && getPWR() >= strong)
        {
            Vector3 nextBlock = Vector3.getNewVector().set(attacker).subtractFrom(location).reverse().norm()
                    .addTo(location);
            IBlockState nextState = nextBlock.getBlockState(world);
            if (block == Blocks.sand)
            {
                location.setBlock(world, Blocks.glass);
            }
            else if (block.isReplaceable(world, location.getPos()) && nextState.getBlock() == Blocks.sand)
            {
                nextBlock.setBlock(world, Blocks.glass);
            }
        }
        if (getType(attacker) == PokeType.fire && getPWR() >= strong)
        {
            Vector3 nextBlock = Vector3.getNewVector().set(attacker).subtractFrom(location).reverse().norm()
                    .addTo(location);
            IBlockState nextState = nextBlock.getBlockState(world);
            if (block == Blocks.obsidian)
            {
                location.setBlock(world, Blocks.lava);
            }
            else if (block.isReplaceable(world, location.getPos()) && nextState.getBlock() == Blocks.obsidian)
            {
                nextBlock.setBlock(world, Blocks.lava);
            }
        }
    }

    @Override
    protected void finalAttack(IPokemob attacker, Entity attacked, float f)
    {
        finalAttack(attacker, attacked, f, true);
    }

    @Override
    protected void finalAttack(IPokemob attacker, Entity attacked, float f, boolean message)
    {
        if (doAttack(attacker, attacked, f))
        {
            if (getAnimation() instanceof Thunder && attacked != null)
            {
                EntityLightningBolt lightning = new EntityLightningBolt(attacked.worldObj, 0, 0, 0, false);
                attacked.onStruckByLightning(lightning);
            }
            if (f > 0 && attacked instanceof EntityCreeper)
            {
                EntityCreeper creeper = (EntityCreeper) attacked;
                if (move.type == PokeType.psychic && creeper.getHealth() > 0)
                {
                    creeper.explode();
                }
            }
            if (message) MovesUtils.displayMoveMessages(attacker, attacked, name);
            if (move.multiTarget && (getAttackCategory() & CATEGORY_SELF) == 0)
            {
                if (sound != null)
                {
                    ((Entity) attacker).playSound(sound, 0.25F, 1f);
                }
                List<EntityLivingBase> hit = MovesUtils.targetsHit(((Entity) attacker),
                        v.set(attacked).addTo(0, attacked.height / 3, 0));
                notifyClient((Entity) attacker, v.set(attacked), attacked);
                for (Entity e : hit)
                {
                    attacked = e;
                    byte statusChange = STATUS_NON;
                    byte changeAddition = CHANGE_NONE;
                    if (move.statusChange != STATUS_NON && MovesUtils.rand.nextInt(100) <= move.statusChance)
                    {
                        statusChange = move.statusChange;
                    }
                    if (move.change != CHANGE_NONE && MovesUtils.rand.nextInt(100) <= move.chanceChance)
                    {
                        changeAddition = move.change;
                    }
                    int pwr;
                    int finalAttackStrength = MovesUtils.attack(new MovePacket(attacker, attacked, name, move.type,
                            pwr = getPWR(attacker, attacked), move.crit, statusChange, changeAddition));

                    postAttack(attacker, attacked, pwr, finalAttackStrength);
                }
            }
            else if ((getAttackCategory() & CATEGORY_SELF) == 0)
            {
                Entity temp = attacked;
                if (!move.notIntercepable)
                {
                    attacked = MovesUtils.targetHit(((Entity) attacker),
                            v.set(attacked).addTo(0, attacked.height / 3, 0));
                }
                if (sound != null)
                {
                    ((Entity) attacker).playSound(sound, 0.5F, 0.4F / (MovesUtils.rand.nextFloat() * 0.4F + 0.8F));
                }
                if (attacked == null) attacked = temp;
                notifyClient((Entity) attacker, v.set(attacked), attacked);
                byte statusChange = STATUS_NON;
                byte changeAddition = CHANGE_NONE;
                if (move.statusChange != STATUS_NON && MovesUtils.rand.nextInt(100) <= move.statusChance)
                {
                    statusChange = move.statusChange;
                }
                if (move.change != CHANGE_NONE && MovesUtils.rand.nextInt(100) <= move.chanceChance)
                {
                    changeAddition = move.change;
                }
                int finalAttackStrength = MovesUtils.attack(new MovePacket(attacker, attacked, name, move.type,
                        getPWR(attacker, attacked), move.crit, statusChange, changeAddition));
                postAttack(attacker, attacked, f, finalAttackStrength);
            }
        }
    }

    @Override
    public int getAttackDelay(IPokemob attacker)
    {
        return 0;
    }

    @Override
    public Move_Base getMove(String name)
    {
        return MovesUtils.getMoveFromName(name);
    }

    @Override
    public boolean isMoveImplemented(String s)
    {
        return MovesUtils.isMoveImplemented(s);
    }

    /** Sends a message to clients to display specific animation on the client
     * side.
     *
     * @param attacker
     * @param attacked */
    @Override
    public void notifyClient(Entity attacker, Vector3 attacked, Entity target)
    {
        if (!PokecubeCore.isOnClientSide())
        {
            String toSend = getName();

            double shift = target != null ? target.height / 2 : 0;

            toSend += "`" + attacker.getEntityId();
            toSend += "`" + attacked.x + "`" + (attacked.y + shift) + "`" + attacked.z;
            if (target != null)
            {
                toSend += "`" + target.getEntityId();
            }
            else
            {
                toSend += "`" + 0;
            }
            PokecubeClientPacket packet = PokecubePacketHandler.makeClientPacket(PokecubeClientPacket.MOVEANIMATION,
                    toSend.getBytes());
            PokecubePacketHandler.sendToAllNear(packet, v1.set(attacker), attacker.dimension, 64);

        }
    }

    /** Called after the attack for special post attack treatment.
     * 
     * @param attacker
     * @param attacked
     * @param f
     * @param finalAttackStrength
     *            the number of HPs the attack takes from target */
    @Override
    public void postAttack(IPokemob attacker, Entity attacked, float f, int finalAttackStrength)
    {
    }

    /** Specify the sound this move should play when executed.
     * 
     * @param sound
     *            the string id of the sound to play
     * @return the move */
    @Override
    public Move_Basic setSound(String sound)
    {
        this.sound = new SoundEvent(new ResourceLocation(sound));

        return this;
    }
}
