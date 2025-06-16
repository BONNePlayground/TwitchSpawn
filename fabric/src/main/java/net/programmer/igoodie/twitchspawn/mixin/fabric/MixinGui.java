//
// Created by BONNe
// Copyright - 2023
//


package net.programmer.igoodie.twitchspawn.mixin.fabric;


import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.programmer.igoodie.twitchspawn.events.TwitchSpawnClientGuiEvent;


/**
 * This mixin injects into render method to simulate similar entry points as in forge.
 */
@Mixin(value = Gui.class)
public class MixinGui
{
    @Inject(method = "renderDemoOverlay", at = @At(value = "HEAD"))
    private void preRenderHotbar(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci)
    {
        // Change to text
        TwitchSpawnClientGuiEvent.OVERLAY_RENDER_PRE.invoker().renderHud(
            guiGraphics,
            "demo_overlay");
    }


    @Inject(method = "renderDemoOverlay", at = @At(value = "RETURN"))
    private void postRenderHotbar(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci)
    {
        TwitchSpawnClientGuiEvent.OVERLAY_RENDER_POST.invoker().renderHud(
            guiGraphics,
            "demo_overlay");
    }
}
