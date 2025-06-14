//
// Created by BONNe
// Copyright - 2023
//


package net.programmer.igoodie.twitchspawn.events.fabric;


import com.electronwill.nightconfig.core.io.ParsingException;
import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ErrorScreen;
import net.minecraft.network.chat.*;
import net.minecraft.util.FormattedCharSequence;
import net.programmer.igoodie.twitchspawn.mixin.fabric.ScreenAccessor;
import net.programmer.igoodie.twitchspawn.tslanguage.parser.TSLSyntaxError;


/**
 * Very simplistic error screen for fabric.
 */
public class CustomErrorScreen extends ErrorScreen
{
    private final List<Exception> modLoadErrors;

    private Component errorHeader;


    public CustomErrorScreen(List<Exception> warnings)
    {
        super(new TextComponent("Loading Error"), new TextComponent("Twitch Spawn"));
        this.modLoadErrors = warnings;
    }


    @Override
    public void init()
    {
        super.init();
        this.clearWidgets();

        this.errorHeader = new TextComponent(ChatFormatting.WHITE + "Twitch Spawn Loading Error" + ChatFormatting.RESET);

        MutableComponent component = new TextComponent("");

        this.modLoadErrors.forEach(exception ->
        {
            String i18nMessage;

            if (exception instanceof TSLSyntaxError)
            {
                i18nMessage = "modloader.twitchspawn.error.tsl";
            }
            else if (exception instanceof ParsingException)
            {
                i18nMessage = "modloader.twitchspawn.error.toml";
            }
            else if (exception instanceof JsonSyntaxException)
            {
                i18nMessage = "modloader.twitchspawn.error.json";
            }
            else
            {
                i18nMessage = "modloader.twitchspawn.error.unknown";
            }

            component.append(new TranslatableComponent(i18nMessage)).
                append(new TextComponent(exception.getMessage())).
                append("\n");
        });

        this.addRenderableWidget(new Button(this.width / 2 - 100, 140, 200, 20, CommonComponents.GUI_CANCEL, (button) -> this.minecraft.setScreen(null)));
    }


    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick)
    {
        this.renderBackground(poseStack);

        drawMultiLineCenteredString(poseStack, font, errorHeader, this.width / 2, 10);
        ((ScreenAccessor) this).getRenderables().forEach(button -> button.render(poseStack, mouseX, mouseY, partialTick));
    }


    private void drawMultiLineCenteredString(PoseStack poseStack, Font fr, Component str, int x, int y)
    {
        for (FormattedCharSequence s : fr.split(str, this.width))
        {
            fr.drawShadow(poseStack, s.toString(), (float) (x - fr.width(s) / 2.0), (float) y, 0xFFFFFF, true);
            y += fr.lineHeight;
        }
    }
}
