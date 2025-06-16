package net.programmer.igoodie.twitchspawn.network.packet;


import org.jetbrains.annotations.NotNull;

import dev.architectury.networking.NetworkManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.programmer.igoodie.twitchspawn.TwitchSpawn;
import net.programmer.igoodie.twitchspawn.client.gui.GlobalChatCooldownOverlay;


public record GlobalChatCooldownPacket(long timestamp) implements CustomPacketPayload
{
    /**
     * This method handles incoming packet on server.
     * @param data The incoming packet.
     * @param packetContext The packet context.
     */
    public static void handle(GlobalChatCooldownPacket data, NetworkManager.PacketContext packetContext)
    {
        long timestamp = data.timestamp();
        packetContext.queue(() -> GlobalChatCooldownOverlay.setCooldownTimestamp(timestamp));
    }


    @Override
    @NotNull
    public Type<? extends CustomPacketPayload> type()
    {
        return GlobalChatCooldownPacket.ID;
    }


    public static final Type<GlobalChatCooldownPacket> ID =
        new Type<>(new ResourceLocation(TwitchSpawn.MOD_ID, "global_chat_cooldown_packet"));


    public static final StreamCodec<RegistryFriendlyByteBuf, GlobalChatCooldownPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_LONG, GlobalChatCooldownPacket::timestamp,
        GlobalChatCooldownPacket::new
    );
}
