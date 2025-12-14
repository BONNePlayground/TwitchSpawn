//
// Created by BONNe
// Copyright - 2025
//


package net.programmer.igoodie.twitchspawn.client;


import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.architectury.networking.NetworkManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.programmer.igoodie.twitchspawn.TwitchSpawnLoadingErrors;
import net.programmer.igoodie.twitchspawn.client.gui.GlobalChatCooldownOverlay;
import net.programmer.igoodie.twitchspawn.client.gui.StatusIndicatorOverlay;
import net.programmer.igoodie.twitchspawn.client.screens.LoadingErrorScreen;
import net.programmer.igoodie.twitchspawn.client.screens.TwitchAuthScreen;
import net.programmer.igoodie.twitchspawn.events.TwitchSpawnClientGuiEvent;
import net.programmer.igoodie.twitchspawn.network.packet.GlobalChatCooldownPacket;
import net.programmer.igoodie.twitchspawn.network.packet.OsRunPacket;
import net.programmer.igoodie.twitchspawn.network.packet.StatusChangedPacket;
import net.programmer.igoodie.twitchspawn.network.packet.SyncStreamerDataPacket;
import net.programmer.igoodie.twitchspawn.udl.NotepadUDLUpdater;


public class TwitchSpawnClient
{
    public static void init()
    {
        NotepadUDLUpdater.attemptUpdate();

        ClientPlayerEvent.CLIENT_PLAYER_JOIN.register(player ->
        {
            StatusIndicatorOverlay.register();
            GlobalChatCooldownOverlay.register();
        });

        ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(player ->
        {
            StatusIndicatorOverlay.unregister();
            GlobalChatCooldownOverlay.unregister();
        });

        NetworkManager.registerReceiver(NetworkManager.Side.S2C,
            GlobalChatCooldownPacket.ID,
            GlobalChatCooldownPacket.STREAM_CODEC,
            GlobalChatCooldownPacket::handle);

        NetworkManager.registerReceiver(NetworkManager.Side.S2C,
            OsRunPacket.ID,
            OsRunPacket.STREAM_CODEC,
            OsRunPacket::handle);

        NetworkManager.registerReceiver(NetworkManager.Side.S2C,
            StatusChangedPacket.ID,
            StatusChangedPacket.STREAM_CODEC,
            StatusChangedPacket::handle);

        NetworkManager.registerReceiver(NetworkManager.Side.S2C,
            SyncStreamerDataPacket.S2C.ID,
            SyncStreamerDataPacket.S2C.STREAM_CODEC,
            SyncStreamerDataPacket::handle);
    }


    /**
     * Notify the client about the config crash.
     * @param errors The errors that caused the config crash.
     */
    public static void notifyCrash(TwitchSpawnLoadingErrors errors)
    {
        TwitchSpawnClientGuiEvent.FINISH_LOADING_OVERLAY.register((client, screen) ->
        {
            if (screen instanceof TitleScreen)
            {
                LoadingErrorScreen errorScreen = new LoadingErrorScreen(errors.getExceptions());
                errorScreen.init(client,
                    client.getWindow().getGuiScaledWidth(),
                    client.getWindow().getGuiScaledHeight());
                client.setScreen(errorScreen);
            }
        });
    }


    public static void openAuth()
    {
        Minecraft.getInstance().setScreen(new TwitchAuthScreen());
    }
}
