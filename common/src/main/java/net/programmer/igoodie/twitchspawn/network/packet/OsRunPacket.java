package net.programmer.igoodie.twitchspawn.network.packet;


import org.jetbrains.annotations.NotNull;

import dev.architectury.networking.NetworkManager;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.programmer.igoodie.twitchspawn.TwitchSpawn;
import net.programmer.igoodie.twitchspawn.tslanguage.action.OsRunAction;


public record OsRunPacket(OsRunAction.Shell shell, String script) implements CustomPacketPayload
{
    /**
     * This method handles incoming packet on server.
     * @param data The incoming packet.
     * @param packetContext The packet context.
     */
    public static void handle(OsRunPacket data, NetworkManager.PacketContext packetContext)
    {
        OsRunAction.Shell shell = data.shell();
        String script = data.script();

        packetContext.queue(() -> OsRunAction.handleLocalScript(shell, script));
    }


    @Override
    @NotNull
    public Type<? extends CustomPacketPayload> type()
    {
        return OsRunPacket.ID;
    }


    public static final Type<OsRunPacket> ID =
        new Type<>(ResourceLocation.fromNamespaceAndPath(TwitchSpawn.MOD_ID, "os_run_packet"));


    private static final StreamCodec<ByteBuf, OsRunAction.Shell> SHELL_CODEC = StreamCodec.of(
        (buf, shell) -> buf.writeInt(shell.ordinal()),
        buf -> OsRunAction.Shell.values()[buf.readInt()]
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, OsRunPacket> STREAM_CODEC = StreamCodec.composite(
        SHELL_CODEC, OsRunPacket::shell,
        ByteBufCodecs.STRING_UTF8, OsRunPacket::script,
        OsRunPacket::new
    );
}
