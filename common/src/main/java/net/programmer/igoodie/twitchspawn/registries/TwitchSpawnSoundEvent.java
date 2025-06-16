package net.programmer.igoodie.twitchspawn.registries;


import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.programmer.igoodie.twitchspawn.TwitchSpawn;


/**
 * This class registers sound events.
 */
public class TwitchSpawnSoundEvent
{
    public static void register()
    {
    }

    /**
     * Registry for sound events.
     */
    public static final DeferredRegister<SoundEvent> REGISTRY =
        DeferredRegister.create(TwitchSpawn.MOD_ID, Registries.SOUND_EVENT);

    /**
     * Sound event for popping in.
     */
    public static final RegistrySupplier<SoundEvent> POP_IN = REGISTRY.register("pop_in",
        () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(TwitchSpawn.MOD_ID, "pop_in")));

    /**
     * Sound event for popping out.
     */
    public static final RegistrySupplier<SoundEvent> POP_OUT = REGISTRY.register("pop_out",
        () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(TwitchSpawn.MOD_ID, "pop_out")));
}
