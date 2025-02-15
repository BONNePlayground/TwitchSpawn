package net.programmer.igoodie.twitchspawn.network.packet;


import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.programmer.igoodie.twitchspawn.tslanguage.action.OsRunAction;

public class OsRunPacket {

    public OsRunPacket(OsRunAction.Shell shell, String script) {
        this.shell = shell;
        this.script = script;
    }

    public static void encode(OsRunPacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.shell.ordinal());
        buffer.writeUtf(packet.script);
    }

    public static OsRunPacket decode(FriendlyByteBuf buffer) {
        OsRunAction.Shell shell = OsRunAction.Shell.values()[buffer.readInt()];
        String script = buffer.readUtf();

        return new OsRunPacket(shell, script);
    }

    public void handle(CustomPayloadEvent.Context context) {
        context.enqueueWork(() -> OsRunAction.handleLocalScript(this.shell, this.script));
        context.setPacketHandled(true);
    }


    /**
     * Shell to run the script with.
     */
    private final OsRunAction.Shell shell;

    /**
     * Script to run.
     */
    private final String script;
}
