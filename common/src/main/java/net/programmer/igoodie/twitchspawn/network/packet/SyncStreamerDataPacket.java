//
// Created by BONNe
// Copyright - 2025
//

package net.programmer.igoodie.twitchspawn.network.packet;


import com.mojang.datafixers.util.Function4;
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


public abstract class SyncStreamerDataPacket implements CustomPacketPayload {
    protected final String playerName, twitchToken, twitchRefreshToken, subscribeScopes;

    protected SyncStreamerDataPacket(String playerName, String twitchToken, String twitchRefreshToken, String subscribeScopes) {
        this.playerName = playerName;
        this.twitchToken = twitchToken;
        this.twitchRefreshToken = twitchRefreshToken;
        this.subscribeScopes = subscribeScopes;
    }

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

    public static <T extends SyncStreamerDataPacket> StreamCodec<RegistryFriendlyByteBuf, T> streamCodec(Function4<String, String, String, String, T> constructor) {
        return StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, p -> p.playerName,
                ByteBufCodecs.STRING_UTF8, p -> p.twitchToken,
                ByteBufCodecs.STRING_UTF8, p -> p.twitchRefreshToken,
                ByteBufCodecs.STRING_UTF8, p -> p.subscribeScopes,
                constructor
        );
    }

    public static class C2S extends SyncStreamerDataPacket {
        public static final StreamCodec<? super RegistryFriendlyByteBuf, C2S> STREAM_CODEC = streamCodec(C2S::new);

        public C2S(String playerName, String twitchToken, String twitchRefreshToken, String subscribeScopes) {
            super(playerName, twitchToken, twitchRefreshToken, subscribeScopes);
        }

        @Override
        @NotNull
        public Type<? extends CustomPacketPayload> type() {
            return ID;
        }
        public static final Type<C2S> ID = new Type<>(ResourceLocation.fromNamespaceAndPath(TwitchSpawn.MOD_ID, "serverbound_sync_data_packet"));
    }

    public static class S2C extends SyncStreamerDataPacket {
        public static final StreamCodec<? super RegistryFriendlyByteBuf, S2C> STREAM_CODEC = streamCodec(S2C::new);
        public S2C(String playerName, String twitchToken, String twitchRefreshToken, String subscribeScopes) {
            super(playerName, twitchToken, twitchRefreshToken, subscribeScopes);
        }

        @Override
        @NotNull
        public Type<? extends CustomPacketPayload> type() {
            return ID;
        }
        public static final Type<S2C> ID = new Type<>(ResourceLocation.fromNamespaceAndPath(TwitchSpawn.MOD_ID, "clientbound_sync_data_packet"));
    }
}
