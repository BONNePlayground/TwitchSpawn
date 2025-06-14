package net.programmer.igoodie.twitchspawn.events;


import com.mojang.blaze3d.vertex.PoseStack;

import dev.architectury.event.Event;
import dev.architectury.event.EventFactory;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;


@Environment(EnvType.CLIENT)
public interface TwitchSpawnClientGuiEvent
{
    Event<OverlayRenderPre> OVERLAY_RENDER_PRE = EventFactory.createLoop();

    Event<OverlayRenderPost> OVERLAY_RENDER_POST = EventFactory.createLoop();


    @Environment(EnvType.CLIENT)
    interface OverlayRenderPre
    {
        void renderHud(PoseStack matrixStack, String type);
    }


    @Environment(EnvType.CLIENT)
    interface OverlayRenderPost
    {
        void renderHud(PoseStack matrixStack, String type);
    }
}
