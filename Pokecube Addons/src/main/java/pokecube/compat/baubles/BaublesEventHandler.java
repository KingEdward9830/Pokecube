package pokecube.compat.baubles;

import java.util.Set;

import com.google.common.collect.Sets;

import baubles.common.container.InventoryBaubles;
import baubles.common.lib.PlayerHandler;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.entity.layers.LayerCustomHead;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import pokecube.adventures.client.render.item.BagRenderer;
import pokecube.core.events.handlers.EventsHandlerClient.RingChecker;
import pokecube.core.items.megastuff.IMegaWearable;

public class BaublesEventHandler
{
    private Set<RenderPlayer> addedBaubles = Sets.newHashSet();

    public BaublesEventHandler()
    {
        pokecube.core.events.handlers.EventsHandlerClient.checker = new RingChecker()
        {
            @Override
            public boolean hasRing(EntityPlayer player)
            {
                InventoryBaubles inv = PlayerHandler.getPlayerBaubles(player);
                for (int i = 0; i < inv.getSizeInventory(); i++)
                {
                    ItemStack stack = inv.getStackInSlot(i);
                    if (stack != null)
                    {
                        Item item = stack.getItem();
                        if (item instanceof IMegaWearable) { return true; }
                    }
                }
                for (int i = 0; i < player.inventory.armorInventory.length; i++)
                {
                    ItemStack stack = player.inventory.armorInventory[i];
                    if (stack != null)
                    {
                        Item item = stack.getItem();
                        if (item instanceof IMegaWearable) { return true; }
                    }
                }
                return false;
            }
        };
    }

    @SubscribeEvent
    public void addBaubleRender(RenderPlayerEvent.Post event)
    {
        if (addedBaubles.contains(event.getRenderer())) { return; }
        LayerRenderer<?> badHeadRenderer = null;
        for (Object o : event.getRenderer().layerRenderers)
        {
            if (o instanceof LayerCustomHead && !(o instanceof BetterCustomHeadLayer))
            {
                badHeadRenderer = (LayerRenderer<?>) o;
            }
        }
        event.getRenderer().removeLayer(badHeadRenderer);
        event.getRenderer().addLayer(new BetterCustomHeadLayer(event.getRenderer().getMainModel().bipedHead));
        event.getRenderer().addLayer(new RingRenderer(event.getRenderer()));
        event.getRenderer().addLayer(new BagRenderer(event.getRenderer()));
        addedBaubles.add(event.getRenderer());
    }
}
