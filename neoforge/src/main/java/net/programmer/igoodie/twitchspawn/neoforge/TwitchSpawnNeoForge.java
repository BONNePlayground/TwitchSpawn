package net.programmer.igoodie.twitchspawn.neoforge;


import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.programmer.igoodie.twitchspawn.TwitchSpawn;
import net.programmer.igoodie.twitchspawn.registries.neoforge.TwitchSpawnArgumentTypesImpl;


@Mod(TwitchSpawn.MOD_ID)
public class TwitchSpawnNeoForge
{
    public TwitchSpawnNeoForge(IEventBus modBus) {
		// Submit our event bus to let architectury register our content on the right time
        TwitchSpawn.init();

        // Register Registry
        TwitchSpawnArgumentTypesImpl.REGISTRY.register(modBus);
    }
}
