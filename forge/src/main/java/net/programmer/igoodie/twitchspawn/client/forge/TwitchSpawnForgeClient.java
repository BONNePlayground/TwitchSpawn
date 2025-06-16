package net.programmer.igoodie.twitchspawn.client.forge;


import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.programmer.igoodie.twitchspawn.TwitchSpawn;
import net.programmer.igoodie.twitchspawn.client.TwitchSpawnClient;
import net.programmer.igoodie.twitchspawn.events.TwitchSpawnClientGuiEvent;


@Mod.EventBusSubscriber(modid = TwitchSpawn.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class TwitchSpawnForgeClient
{
    @SubscribeEvent
    public static void init(FMLClientSetupEvent event)
    {
        TwitchSpawnClient.init();
        MinecraftForge.EVENT_BUS.addListener(TwitchSpawnForgeClient::onRenderGuiOverlayPre);
        MinecraftForge.EVENT_BUS.addListener(TwitchSpawnForgeClient::onRenderGuiOverlayPost);
    }


    public static void onRenderGuiOverlayPre(RenderGuiOverlayEvent.Pre event)
    {
        TwitchSpawnClientGuiEvent.OVERLAY_RENDER_PRE.invoker().renderHud(event.getGuiGraphics(),
            event.getOverlay().id().getPath());
    }


    public static void onRenderGuiOverlayPost(RenderGuiOverlayEvent.Post event)
    {
        TwitchSpawnClientGuiEvent.OVERLAY_RENDER_POST.invoker().renderHud(event.getGuiGraphics(),
            event.getOverlay().id().getPath());
    }
}
