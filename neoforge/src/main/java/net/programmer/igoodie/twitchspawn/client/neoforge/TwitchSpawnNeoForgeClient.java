package net.programmer.igoodie.twitchspawn.client.neoforge;


import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.programmer.igoodie.twitchspawn.TwitchSpawn;
import net.programmer.igoodie.twitchspawn.client.TwitchSpawnClient;
import net.programmer.igoodie.twitchspawn.events.TwitchSpawnClientGuiEvent;


@EventBusSubscriber(modid = TwitchSpawn.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class TwitchSpawnNeoForgeClient
{
    @SubscribeEvent
    public static void init(FMLClientSetupEvent event)
    {
        TwitchSpawnClient.init();
        NeoForge.EVENT_BUS.addListener(TwitchSpawnNeoForgeClient::onRenderGuiOverlayPre);
        NeoForge.EVENT_BUS.addListener(TwitchSpawnNeoForgeClient::onRenderGuiOverlayPost);
    }


    public static void onRenderGuiOverlayPre(RenderGuiLayerEvent.Pre event)
    {
        TwitchSpawnClientGuiEvent.OVERLAY_RENDER_PRE.invoker().renderHud(event.getGuiGraphics(),
            event.getName().getPath());
    }


    public static void onRenderGuiOverlayPost(RenderGuiLayerEvent.Post event)
    {
        TwitchSpawnClientGuiEvent.OVERLAY_RENDER_POST.invoker().renderHud(event.getGuiGraphics(),
            event.getName().getPath());
    }
}
