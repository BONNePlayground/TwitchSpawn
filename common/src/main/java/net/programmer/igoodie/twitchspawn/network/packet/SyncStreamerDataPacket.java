//
// Created by BONNe
// Copyright - 2025
//


package net.programmer.igoodie.twitchspawn.network.packet;


import org.jetbrains.annotations.NotNull;

import dev.architectury.networking.NetworkManager;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import net.fabricmc.api.EnvType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.programmer.igoodie.twitchspawn.TwitchSpawn;
import net.programmer.igoodie.twitchspawn.client.TwitchSpawnClient;
import net.programmer.igoodie.twitchspawn.configuration.ConfigManager;
import net.programmer.igoodie.twitchspawn.configuration.CredentialsConfig;


public record SyncStreamerDataPacket(String playerName,
                                     String twitchToken,
                                     String twitchRefreshToken,
                                     String subscribeScopes) implements CustomPacketPayload
{
    public static void handle(SyncStreamerDataPacket data, NetworkManager.PacketContext packetContext)
    {
        packetContext.queue(() ->
        {
            boolean updated = false;

            for (CredentialsConfig.Streamer streamer : ConfigManager.CREDENTIALS.streamers)
            {
                if (streamer.minecraftNick.equalsIgnoreCase(data.playerName))
                {
                    streamer.twitchAccessToken = data.twitchToken;
                    streamer.twitchRefreshToken = data.twitchRefreshToken;
                    streamer.twitchScopes = data.subscribeScopes;

                    updated = true;
                }
            }

            if (!updated)
            {
                CredentialsConfig.Streamer streamer = new CredentialsConfig.Streamer();
                streamer.minecraftNick = data.playerName;
                streamer.twitchAccessToken = data.twitchToken;
                streamer.twitchRefreshToken = data.twitchRefreshToken;
                streamer.twitchScopes = data.subscribeScopes;

                ConfigManager.CREDENTIALS.streamers.add(streamer);
            }

            if (packetContext.getEnv() == EnvType.CLIENT)
            {
                EnvExecutor.runInEnv(Env.CLIENT, () -> TwitchSpawnClient::openAuth);
            }
            else
            {
                ConfigManager.CREDENTIALS.save();
            }
        });
    }


    @Override
    @NotNull
    public Type<? extends CustomPacketPayload> type()
    {
        return SyncStreamerDataPacket.ID;
    }


    public static final Type<SyncStreamerDataPacket> ID =
        new Type<>(new ResourceLocation(TwitchSpawn.MOD_ID, "sync_data_packet"));


    public static final StreamCodec<RegistryFriendlyByteBuf, SyncStreamerDataPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8, SyncStreamerDataPacket::playerName,
        ByteBufCodecs.STRING_UTF8, SyncStreamerDataPacket::twitchToken,
        ByteBufCodecs.STRING_UTF8, SyncStreamerDataPacket::twitchRefreshToken,
        ByteBufCodecs.STRING_UTF8, SyncStreamerDataPacket::subscribeScopes,
        SyncStreamerDataPacket::new
    );
}
