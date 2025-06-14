package net.programmer.igoodie.twitchspawn.client.forge;


import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.programmer.igoodie.twitchspawn.TwitchSpawn;
import net.programmer.igoodie.twitchspawn.client.TwitchSpawnClient;


@Mod.EventBusSubscriber(modid = TwitchSpawn.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class AnimalPenForgeClient
{
    @SubscribeEvent
    public static void init(FMLClientSetupEvent event)
    {
        TwitchSpawnClient.init();
    }
}
