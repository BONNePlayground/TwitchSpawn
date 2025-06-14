package net.programmer.igoodie.twitchspawn.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.programmer.igoodie.twitchspawn.TwitchSpawn;
import net.programmer.igoodie.twitchspawn.configuration.ConfigManager;
import net.programmer.igoodie.twitchspawn.configuration.PreferencesConfig;
import net.programmer.igoodie.twitchspawn.events.TwitchSpawnClientGuiEvent;

public class StatusIndicatorOverlay {

    private static final ResourceLocation indicatorIcons =
            new ResourceLocation(TwitchSpawn.MOD_ID, "textures/indicators.png");

    private static boolean running = false;

    private static boolean drew = false;

    /**
     * Render indicator
     */
    private static final TwitchSpawnClientGuiEvent.OverlayRenderPre PRE_RENDER =
        (matrixStack, type) -> drew = false;


    /**
     * Register rendering events.
     */
    public static void register() {
        TwitchSpawnClientGuiEvent.OVERLAY_RENDER_PRE.register(PRE_RENDER);
    }


    /**
     * Unregister rendering events.
     */
    public static void unregister() {
        TwitchSpawnClientGuiEvent.OVERLAY_RENDER_PRE.unregister(PRE_RENDER);
    }


    public static void setRunning(boolean running) {
        StatusIndicatorOverlay.running = running;

        String soundName = running ? "pop_in" : "pop_out";

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer self = minecraft.player;

        if (self != null) { // Here to hopefully fix an obscure Null Pointer (From UNKNOWN PENGUIN's log)
            self.playSound(new SoundEvent(new ResourceLocation(TwitchSpawn.MOD_ID, soundName)), 1f, 1f);
        }
    }


    private static void onRenderGuiPost(PoseStack matrixStack, String type) {
        if (ConfigManager.PREFERENCES.indicatorDisplay == PreferencesConfig.IndicatorDisplay.DISABLED)
            return; // The display is disabled, stop here

        Minecraft minecraft = Minecraft.getInstance();

        if (!type.equals("TEXT"))
            return; // Render only on HOTBAR

        // Already drew, stop here
        if (drew) return;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, indicatorIcons);

        matrixStack.pushPose();

        int x = 5, y = 5;
        int ux = -1, uy = -1;
        int w = -1, h = -1;

        if (ConfigManager.PREFERENCES.indicatorDisplay == PreferencesConfig.IndicatorDisplay.ENABLED) {
            ux = 0;
            uy = running ? 22 : 0;
            w = 65;
            h = 22;
        } else if (ConfigManager.PREFERENCES.indicatorDisplay == PreferencesConfig.IndicatorDisplay.CIRCLE_ONLY) {
            ux = 0;
            uy = running ? 56 : 44;
            w = 12;
            h = 12;
        }

        matrixStack.scale(1f, 1f, 1f);
        minecraft.gui.blit(matrixStack, x, y, ux, uy, w, h);

        matrixStack.popPose();

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
        RenderSystem.setShaderTexture(0, Gui.GUI_ICONS_LOCATION);

        drew = true;
    }

}
