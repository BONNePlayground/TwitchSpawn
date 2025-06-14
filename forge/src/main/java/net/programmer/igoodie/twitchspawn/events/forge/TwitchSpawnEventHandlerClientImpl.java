package net.programmer.igoodie.twitchspawn.events.forge;


import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.programmer.igoodie.twitchspawn.events.TwitchSpawnClientGuiEvent;


/**
 * This class manages the registration of events.
 */
public class TwitchSpawnEventHandlerClientImpl
{
    @SubscribeEvent
    public static void onRenderGuiOverlayPre(RenderGameOverlayEvent.Pre event)
    {
        TwitchSpawnClientGuiEvent.OVERLAY_RENDER_PRE.invoker().renderHud(event.getMatrixStack(), event.getType().name());
    }

    @SubscribeEvent
    public static void onRenderGuiOverlayPost(RenderGameOverlayEvent.Post event)
    {
        TwitchSpawnClientGuiEvent.OVERLAY_RENDER_POST.invoker().renderHud(event.getMatrixStack(), event.getType().name());
    }
}
