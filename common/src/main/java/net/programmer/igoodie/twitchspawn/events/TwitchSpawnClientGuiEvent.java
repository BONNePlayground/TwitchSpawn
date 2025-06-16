package net.programmer.igoodie.twitchspawn.events;


import dev.architectury.event.Event;
import dev.architectury.event.EventFactory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;


public interface TwitchSpawnClientGuiEvent
{
    /**
     * This event is fired before overlay is rendered.
     */
    Event<OverlayRenderPre> OVERLAY_RENDER_PRE = EventFactory.createLoop();

    /**
     * This event is fired after overlay is rendered.
     */
    Event<OverlayRenderPost> OVERLAY_RENDER_POST = EventFactory.createLoop();

    /**
     * This event is fired after loading screen is removed.
     */
    Event<LoadingScreenFinish> FINISH_LOADING_OVERLAY = EventFactory.createLoop();


    interface OverlayRenderPre
    {
        void renderHud(GuiGraphics guiGraphics, String type);
    }


    interface OverlayRenderPost
    {
        void renderHud(GuiGraphics guiGraphics, String type);
    }

    /**
     * This event is fired when the debug text is rendered.
     */
    interface LoadingScreenFinish
    {
        void removeOverlay(Minecraft client, Screen screen);
    }
}
