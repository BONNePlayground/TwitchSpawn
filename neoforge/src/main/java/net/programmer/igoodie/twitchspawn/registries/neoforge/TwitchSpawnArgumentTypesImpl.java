package net.programmer.igoodie.twitchspawn.registries.neoforge;


import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.programmer.igoodie.twitchspawn.TwitchSpawn;
import net.programmer.igoodie.twitchspawn.command.RulesetNameArgumentType;
import net.programmer.igoodie.twitchspawn.command.StreamerArgumentType;
import net.programmer.igoodie.twitchspawn.command.TSLWordsArgumentType;


public class TwitchSpawnArgumentTypesImpl
{
    /**
     * Registry for argument types.
     */
    public static final DeferredRegister<ArgumentTypeInfo<?, ?>> REGISTRY =
        DeferredRegister.create(Registries.COMMAND_ARGUMENT_TYPE, TwitchSpawn.MOD_ID);

    public static void registerArgumentType()
    {
        // Do nothing. Forge is registred on startup.
    }

    static {
        // Argument type for ruleset names.
        REGISTRY.register("ruleset", () -> ArgumentTypeInfos.registerByClass(RulesetNameArgumentType.class,
            SingletonArgumentInfo.contextFree(RulesetNameArgumentType::rulesetName)));

        // Argument type for streamer names.
        REGISTRY.register("streamer", () -> ArgumentTypeInfos.registerByClass(StreamerArgumentType.class,
            SingletonArgumentInfo.contextFree(StreamerArgumentType::streamerNick)));

        // Argument type for TSL words.
        REGISTRY.register("tslwords", () -> ArgumentTypeInfos.registerByClass(TSLWordsArgumentType.class,
            SingletonArgumentInfo.contextFree(TSLWordsArgumentType::tslWords)));
    }
}
