//
// Created by BONNe
// Copyright - 2025
//


package net.programmer.igoodie.twitchspawn.client;


import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.architectury.event.events.common.PlayerEvent;
import net.programmer.igoodie.twitchspawn.client.gui.GlobalChatCooldownOverlay;
import net.programmer.igoodie.twitchspawn.client.gui.StatusIndicatorOverlay;
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
    }
}
