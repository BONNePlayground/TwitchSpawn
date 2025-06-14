//
// Created by BONNe
// Copyright - 2023
//


package net.programmer.igoodie.twitchspawn.events.fabric;


import com.mojang.blaze3d.vertex.PoseStack;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ErrorScreen;
import net.minecraft.client.gui.screens.Overlay;
import net.programmer.igoodie.twitchspawn.events.TwitchSpawnCommonEvent;


/**
 * This class manages the registration of events.
 */
public class TwitchSpawnEventHandlerImpl
{
    /**
     * Register client events.
     */
    public static void registerClient()
    {
        // Register error for incorrect configs.
        TwitchSpawnCommonEvent.SETUP_EVENT.register(exception -> {
            ClientLifecycleEvents.CLIENT_STARTED.register(client ->
                Minecraft.getInstance().setOverlay(new Overlay()
                {
                    @Override
                    public void render(PoseStack poseStack, int i, int j, float f)
                    {
                        ErrorScreen errorScreen = new CustomErrorScreen(exception.getExceptions());
                        errorScreen.init(client,
                            client.getWindow().getGuiScaledWidth(),
                            client.getWindow().getGuiScaledHeight());
                        errorScreen.render(poseStack, i, j, f);
                    }
                }));
        });
    }
}
