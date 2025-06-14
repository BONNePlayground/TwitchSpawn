package net.programmer.igoodie.twitchspawn;

import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import net.minecraft.commands.synchronization.ArgumentTypes;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.MinecraftServer;
import net.programmer.igoodie.twitchspawn.client.TwitchSpawnClient;
import net.programmer.igoodie.twitchspawn.command.RulesetNameArgumentType;
import net.programmer.igoodie.twitchspawn.command.StreamerArgumentType;
import net.programmer.igoodie.twitchspawn.command.TwitchSpawnCommand;
import net.programmer.igoodie.twitchspawn.command.serializer.RulesetNameArgumentSerializer;
import net.programmer.igoodie.twitchspawn.command.serializer.StreamerArgumentSerializer;
import net.programmer.igoodie.twitchspawn.configuration.ConfigManager;
import net.programmer.igoodie.twitchspawn.configuration.PreferencesConfig;
import net.programmer.igoodie.twitchspawn.network.NetworkManager;
import net.programmer.igoodie.twitchspawn.network.packet.StatusChangedPacket;
import net.programmer.igoodie.twitchspawn.registries.TwitchSpawnSoundEvent;
import net.programmer.igoodie.twitchspawn.tracer.TraceManager;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TwitchSpawn {

    /**
     * The plugin mod-id
     */
    public static final String MOD_ID = "twitchspawn";

    /**
     * Minecraft server instance.
     */
    public static MinecraftServer SERVER;

    /**
     * Trace manager.
     */
    public static TraceManager TRACE_MANAGER;

    /**
     * Logger.
     */
    public static final Logger LOGGER = LogManager.getLogger();


    /**
     * The main init class.
     */
    public static void init()
    {
        CommandRegistrationEvent.EVENT.register(
            (dispatcher, selection) -> TwitchSpawnCommand.register(dispatcher));

        // Trigger tracer on server start.
        LifecycleEvent.SERVER_BEFORE_START.register(server -> {
            SERVER = server;
            TRACE_MANAGER = new TraceManager();
        });

        // Trigger autostart if that is enabled.
        LifecycleEvent.SERVER_STARTING.register(server -> {
            if (ConfigManager.PREFERENCES.autoStart == PreferencesConfig.AutoStartEnum.ENABLED) {
                LOGGER.info("Auto-start is enabled. Attempting to start tracers.");
                TRACE_MANAGER.start();
            }
        });

        // Trigger server stop that would disable tracers.
        LifecycleEvent.SERVER_STOPPING.register(server -> {
            SERVER = null;

            if (TRACE_MANAGER.isRunning())
            {
                TRACE_MANAGER.stop(null, "Server stopping");
            }

            ConfigManager.RULESET_COLLECTION.clearQueue();
        });

        // Do stuff on player joining the server.
        PlayerEvent.PLAYER_JOIN.register(player ->
        {
            String translationKey = TRACE_MANAGER.isRunning() ?
                "commands.twitchspawn.status.on" : "commands.twitchspawn.status.off";

            player.sendMessage(new TranslatableComponent(translationKey), player.getUUID());

            if (TRACE_MANAGER.isRunning())
            {
                TRACE_MANAGER.connectStreamer(player.getName().getString());
            }

            NetworkManager.CHANNEL.sendToPlayer(player, new StatusChangedPacket(TRACE_MANAGER.isRunning()));
        });

        // Do stuff on player leaving the server.
        PlayerEvent.PLAYER_QUIT.register(player ->
        {
            if (TRACE_MANAGER.isRunning())
            {
                TRACE_MANAGER.disconnectStreamer(player.getName().getString());
            }
        });

        try
        {
            TwitchSpawnSoundEvent.register();

            ArgumentTypes.register("twitchspawn:streamer", StreamerArgumentType.class,
                new StreamerArgumentSerializer());
            ArgumentTypes.register("twitchspawn:ruleset", RulesetNameArgumentType.class,
                new RulesetNameArgumentSerializer());

            ConfigManager.loadConfigs();
            NetworkManager.initialize();
        }
        catch (TwitchSpawnLoadingErrors exception)
        {
            EnvExecutor.runInEnv(Env.CLIENT, () -> () -> TwitchSpawnClient.notifyCrash(exception));
            EnvExecutor.runInEnv(Env.SERVER, () -> () -> notifyCrash(exception));
        }
    }


    public static void notifyCrash(TwitchSpawnLoadingErrors errors)
    {
        LOGGER.warn("TwitchSpawn config contains errors:");
        LOGGER.warn("\n" + errors.toString());
    }
}
