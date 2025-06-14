package net.programmer.igoodie.twitchspawn.client.fabric;


import net.fabricmc.api.ClientModInitializer;
import net.programmer.igoodie.twitchspawn.client.TwitchSpawnClient;


public final class TwitchSpawnFabricClient implements ClientModInitializer
{
    @Override
    public void onInitializeClient()
    {
        TwitchSpawnClient.init();
    }
}
