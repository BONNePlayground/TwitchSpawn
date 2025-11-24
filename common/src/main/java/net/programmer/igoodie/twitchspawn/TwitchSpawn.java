package net.programmer.igoodie.twitchspawn;

import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.networking.NetworkManager;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.programmer.igoodie.twitchspawn.client.TwitchSpawnClient;
import net.programmer.igoodie.twitchspawn.command.TwitchSpawnCommand;
import net.programmer.igoodie.twitchspawn.configuration.ConfigManager;
import net.programmer.igoodie.twitchspawn.configuration.PreferencesConfig;
import net.programmer.igoodie.twitchspawn.network.packet.GlobalChatCooldownPacket;
import net.programmer.igoodie.twitchspawn.network.packet.OsRunPacket;
import net.programmer.igoodie.twitchspawn.network.packet.StatusChangedPacket;
import net.programmer.igoodie.twitchspawn.network.packet.SyncStreamerDataPacket;
import net.programmer.igoodie.twitchspawn.registries.TwitchSpawnArgumentTypes;
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
     * The application ID
     */
    public static final String APP_ID = "zl42yzk933wes9pjom49ocdkv8tstg";

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
        CommandRegistrationEvent.EVENT.register((dispatcher, registry, selection) ->
            TwitchSpawnCommand.register(dispatcher));

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

        NetworkManager.registerReceiver(NetworkManager.Side.C2S,
            SyncStreamerDataPacket.C2S.ID,
            SyncStreamerDataPacket.C2S.STREAM_CODEC,
            SyncStreamerDataPacket::handle);

        try
        {
            TwitchSpawnSoundEvent.register();
            TwitchSpawnArgumentTypes.registerArgumentType();

            ConfigManager.loadConfigs();
        }
        catch (TwitchSpawnLoadingErrors exception)
        {
            EnvExecutor.runInEnv(Env.CLIENT, () -> () -> TwitchSpawnClient.notifyCrash(exception));
            EnvExecutor.runInEnv(Env.SERVER, () -> () -> notifyCrash(exception));
        }

        EnvExecutor.runInEnv(Env.SERVER, () -> TwitchSpawn::initServer);
    }


    public static void initServer()
    {
        NetworkManager.registerS2CPayloadType(GlobalChatCooldownPacket.ID, GlobalChatCooldownPacket.STREAM_CODEC);
        NetworkManager.registerS2CPayloadType(OsRunPacket.ID, OsRunPacket.STREAM_CODEC);
        NetworkManager.registerS2CPayloadType(StatusChangedPacket.ID, StatusChangedPacket.STREAM_CODEC);
        NetworkManager.registerS2CPayloadType(SyncStreamerDataPacket.S2C.ID, SyncStreamerDataPacket.S2C.STREAM_CODEC);

        // Do stuff on player joining the server.
        PlayerEvent.PLAYER_JOIN.register(player ->
        {
            String translationKey = TRACE_MANAGER.isRunning() ?
                "commands.twitchspawn.status.on" : "commands.twitchspawn.status.off";

            player.sendSystemMessage(Component.translatable(translationKey));

            if (TRACE_MANAGER.isRunning())
            {
                TRACE_MANAGER.connectStreamer(player.getName().getString());
            }

            NetworkManager.sendToPlayer(player, new StatusChangedPacket(TRACE_MANAGER.isRunning()));
        });

        // Do stuff on player leaving the server.
        PlayerEvent.PLAYER_QUIT.register(player ->
        {
            if (TRACE_MANAGER.isRunning())
            {
                TRACE_MANAGER.disconnectStreamer(player.getName().getString());
            }
        });
    }


    public static void notifyCrash(TwitchSpawnLoadingErrors errors)
    {
        LOGGER.warn("TwitchSpawn config contains errors:");
        LOGGER.warn("\n" + errors.toString());
    }
}
