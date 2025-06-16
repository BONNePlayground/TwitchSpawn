//
// Created by BONNe
// Copyright - 2023
//


package net.programmer.igoodie.twitchspawn.mixin.fabric;


import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.programmer.igoodie.twitchspawn.events.TwitchSpawnClientGuiEvent;


/**
 * This mixin injects into render method to simulate similar entry points as in forge.
 */
@Mixin(value = Gui.class)
public class MixinGui
{
    @Inject(method = "render", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/Minecraft;isDemo()Z"))
    private void preRenderHotbar(GuiGraphics guiGraphics, float f, CallbackInfo ci)
    {
        // Change to text
        TwitchSpawnClientGuiEvent.OVERLAY_RENDER_PRE.invoker().renderHud(
            guiGraphics,
            "debug_text");
    }


    @Inject(method = "render", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/gui/Gui;renderEffects(Lnet/minecraft/client/gui/GuiGraphics;)V"))
    private void postRenderHotbar(GuiGraphics guiGraphics, float f, CallbackInfo ci)
    {
        TwitchSpawnClientGuiEvent.OVERLAY_RENDER_POST.invoker().renderHud(
            guiGraphics,
            "debug_text");
    }
}
