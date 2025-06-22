//
// Created by BONNe
// Copyright - 2025
//


package net.programmer.igoodie.twitchspawn.network.packet;


import java.util.function.Supplier;

import dev.architectury.networking.NetworkManager;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import net.minecraft.network.FriendlyByteBuf;
import net.programmer.igoodie.twitchspawn.client.TwitchSpawnClient;
import net.programmer.igoodie.twitchspawn.configuration.ConfigManager;
import net.programmer.igoodie.twitchspawn.configuration.CredentialsConfig;


public class SyncStreamerDataPacket

{
    public SyncStreamerDataPacket(
        String playerName,
        String twitchToken,
        String twitchRefreshToken,
        String subscribeScopes)
    {
        this.playerName = playerName;
        this.twitchToken = twitchToken;
        this.twitchRefreshToken = twitchRefreshToken;
        this.subscribeScopes = subscribeScopes;
    }


    public static void encode(SyncStreamerDataPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeUtf(packet.playerName);
        buffer.writeUtf(packet.twitchToken);
        buffer.writeUtf(packet.twitchRefreshToken);
        buffer.writeUtf(packet.subscribeScopes);
    }


    public static SyncStreamerDataPacket decode(FriendlyByteBuf buffer)
    {
        return new SyncStreamerDataPacket(buffer.readUtf(),
            buffer.readUtf(),
            buffer.readUtf(),
            buffer.readUtf());
    }


    public void handle(Supplier<NetworkManager.PacketContext> context)
    {
        context.get().queue(() ->
        {
            boolean updated = false;

            for (CredentialsConfig.Streamer streamer : ConfigManager.CREDENTIALS.streamers)
            {
                if (streamer.minecraftNick.equalsIgnoreCase(this.playerName))
                {
                    streamer.twitchAccessToken = this.twitchToken;
                    streamer.twitchRefreshToken = this.twitchRefreshToken;
                    streamer.twitchScopes = this.subscribeScopes;

                    updated = true;
                }
            }

            if (!updated)
            {
                CredentialsConfig.Streamer streamer = new CredentialsConfig.Streamer();
                streamer.minecraftNick = this.playerName;
                streamer.twitchAccessToken = this.twitchToken;
                streamer.twitchRefreshToken = this.twitchRefreshToken;
                streamer.twitchScopes = this.subscribeScopes;

                ConfigManager.CREDENTIALS.streamers.add(streamer);
            }

            EnvExecutor.runInEnv(Env.CLIENT, () -> TwitchSpawnClient::openAuth);
            EnvExecutor.runInEnv(Env.SERVER, () -> () -> ConfigManager.CREDENTIALS.save());
        });
    }

    private final String playerName;

    private final String twitchToken;

    private final String twitchRefreshToken;

    private final String subscribeScopes;
}
