//
// Created by BONNe
// Copyright - 2025
//


package net.programmer.igoodie.twitchspawn.client;


import dev.architectury.event.events.client.ClientPlayerEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.programmer.igoodie.twitchspawn.TwitchSpawnLoadingErrors;
import net.programmer.igoodie.twitchspawn.client.gui.GlobalChatCooldownOverlay;
import net.programmer.igoodie.twitchspawn.client.gui.StatusIndicatorOverlay;
import net.programmer.igoodie.twitchspawn.client.screens.LoadingErrorScreen;
import net.programmer.igoodie.twitchspawn.client.screens.TwitchAuthScreen;
import net.programmer.igoodie.twitchspawn.events.TwitchSpawnClientGuiEvent;
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
