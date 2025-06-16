package net.programmer.igoodie.twitchspawn.network.packet;


import org.jetbrains.annotations.NotNull;

import dev.architectury.networking.NetworkManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.programmer.igoodie.twitchspawn.TwitchSpawn;
import net.programmer.igoodie.twitchspawn.client.gui.StatusIndicatorOverlay;


public record StatusChangedPacket(boolean status) implements CustomPacketPayload
{
    /**
     * This method handles incoming packet on server.
     * @param data The incoming packet.
     * @param packetContext The packet context.
     */
    public static void handle(StatusChangedPacket data, NetworkManager.PacketContext packetContext)
    {
        boolean status = data.status();
        packetContext.queue(() -> StatusIndicatorOverlay.setRunning(status));
    }


    @Override
    @NotNull
    public Type<? extends CustomPacketPayload> type()
    {
        return StatusChangedPacket.ID;
    }


    public static final Type<StatusChangedPacket> ID =
        new Type<>(new ResourceLocation(TwitchSpawn.MOD_ID, "status_changed_packet"));


    public static final StreamCodec<RegistryFriendlyByteBuf, StatusChangedPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.BOOL, StatusChangedPacket::status,
        StatusChangedPacket::new
    );
}
