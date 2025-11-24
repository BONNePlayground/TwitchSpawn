package net.programmer.igoodie.twitchspawn.client.screens;


import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import dev.architectury.networking.NetworkManager;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.programmer.igoodie.twitchspawn.configuration.ConfigManager;
import net.programmer.igoodie.twitchspawn.configuration.CredentialsConfig;
import net.programmer.igoodie.twitchspawn.network.packet.SyncStreamerDataPacket;
import net.programmer.igoodie.twitchspawn.twitchauth.TwitchApiClient;


/**
 * This screen opens to connect
 */
public class TwitchAuthScreen extends Screen
{
    public TwitchAuthScreen()
    {
        super(Component.translatable("gui.twitchspawn.auth_screen_title"));
        this.twitchApiClient = new TwitchApiClient();
        this.eventCheckboxes = new ArrayList<>();
    }


    @Override
    protected void init()
    {
        super.init();

        // Initialize event checkboxes
        this.initEventCheckboxes();

        // Validate token and initialize everything.
        this.validateToken();

        // Connect button positioned below checkboxes
        int buttonY = this.height / 2 + 80;
        this.authorizeButton = Button.builder(Component.translatable(this.currentState != AuthState.CONNECTED ?
                "gui.twitchspawn.auth_button" : "gui.twitchspawn.disconnect_button"),
            button ->
            {
                if (this.currentState == AuthState.READY)
                {
                    this.startDeviceFlow();
                }
                else if (this.currentState == AuthState.WAITING_FOR_USER)
                {
                    this.openBrowser(this.verificationUri);
                }
                else if (this.currentState == AuthState.CONNECTED)
                {
                    this.disconnect();
                }
            }).
            size(200, 20).
            pos(this.width / 2 - 100, buttonY).
            build();

        this.addRenderableWidget(this.authorizeButton);
    }


    private void initEventCheckboxes()
    {
        this.eventCheckboxes.clear();

        int startY = this.height / 2 - 90;
        int checkboxWidth = 300;
        int checkboxHeight = 20;
        int spacing = 22;

        for (int i = 0; i < AVAILABLE_EVENTS.length; i++)
        {
            EventInfo eventInfo = AVAILABLE_EVENTS[i];
            int x = this.width / 2 - checkboxWidth / 2;
            int y = startY + (i * spacing);

            ScopeCheckbox checkbox = new ScopeCheckbox(
                x, y,
                Component.translatable(eventInfo.description),
                eventInfo.eventType,
                eventInfo.requiredScopes,
                eventInfo.defaultSelected
            );

            this.eventCheckboxes.add(checkbox);
            this.addRenderableWidget(checkbox.getCheckbox());
        }
    }


    private void validateToken()
    {
        LocalPlayer player = Minecraft.getInstance().player;

        CredentialsConfig.Streamer streamer = ConfigManager.CREDENTIALS.streamers.stream().
            filter(s -> s.minecraftNick.equalsIgnoreCase(player.getName().getString())).
            findAny().
            orElse(null);

        if (streamer == null)
        {
            streamer = new CredentialsConfig.Streamer();
            streamer.minecraftNick = player.getName().getString();
        }

        if (this.twitchApiClient.isValidToken(streamer))
        {
            this.currentState = AuthState.CONNECTED;
            Set<String> activeScopes = new HashSet<>(Arrays.stream(streamer.twitchScopes.split(" ")).toList());

            this.eventCheckboxes.forEach(scopeCheckbox ->
            {
                if (scopeCheckbox.selected() != activeScopes.containsAll(
                    Arrays.stream(scopeCheckbox.getEventType().split(" ")).toList()))
                {
                    scopeCheckbox.onPress();
                }

                // Disable checkboxes
                scopeCheckbox.setActive(false);
            });
        }
    }


    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick)
    {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // Title
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);

        // Event selection header
        Component eventHeader = Component.translatable("gui.twitchspawn.choose_events");
        guiGraphics.drawCenteredString(this.font, eventHeader, this.width / 2, this.height / 2 - 120, 0xCCCCCC);

        // Status message
        if (!this.statusMessage.getString().isEmpty())
        {
            int statusY = this.height / 2 + 120;
            guiGraphics.drawCenteredString(this.font, this.statusMessage, this.width / 2, statusY,
                this.currentState == AuthState.ERROR ? 0xFF5555 : 0x55FF55);
        }

        // User code display
        if (this.currentState == AuthState.WAITING_FOR_USER && this.userCode != null)
        {
            Component codeText = Component.translatable("gui.twitchspawn.enter_code", this.userCode);
            guiGraphics.drawCenteredString(this.font, codeText, this.width / 2, this.height / 2 + 140, 0xFFFF55);
        }
    }


    private void disconnect()
    {
        this.currentState = AuthState.READY;
        this.statusMessage = Component.translatable("gui.twitchspawn.status_disconnected");

        // Reset button
        this.authorizeButton.setMessage(Component.translatable("gui.twitchspawn.auth_button"));
        this.authorizeButton.active = true;

        this.eventCheckboxes.forEach(scopeCheckbox -> scopeCheckbox.setActive(true));

        // rework access token
        ConfigManager.CREDENTIALS.streamers.
            stream().
            filter(s -> s.minecraftNick.equalsIgnoreCase(Minecraft.getInstance().player.getName().getString())).
            findFirst().
            ifPresent(s -> {
                try
                {
                    this.twitchApiClient.revokeToken(s.twitchAccessToken);
                }
                catch (IOException e)
                {
                }
            });

        NetworkManager.sendToServer(new SyncStreamerDataPacket.C2S(
            Minecraft.getInstance().player.getName().getString(),
            "",
            "",
            ""
        ));
    }


    private void startDeviceFlow()
    {
        this.eventCheckboxes.forEach(scopeCheckbox -> scopeCheckbox.setActive(false));

        // Get selected events and their required scopes
        List<String> selectedEvents = this.getSelectedEvents();
        String requiredScopes = this.getRequiredScopes();

        if (selectedEvents.isEmpty())
        {
            this.statusMessage = Component.translatable("gui.twitchspawn.status_select_event");
            return;
        }

        this.currentState = AuthState.LOADING;
        this.authorizeButton.active = false;
        this.statusMessage = Component.translatable("gui.twitchspawn.status_connecting");

        CompletableFuture.runAsync(() ->
        {
            try
            {
                TwitchApiClient.DeviceAuthResponse authResponse = this.twitchApiClient.requestDeviceCode(requiredScopes);

                this.deviceCode = authResponse.deviceCode();
                this.userCode = authResponse.userCode();
                this.verificationUri = authResponse.verificationUri();
                this.expiresIn = 5 * 60;
                this.interval = authResponse.interval();

                this.currentState = AuthState.WAITING_FOR_USER;
                this.authorizeButton.setMessage(Component.translatable("gui.twitchspawn.open_browser_button"));
                this.authorizeButton.active = true;
                this.statusMessage = Component.translatable("gui.twitchspawn.status_click_browser");

                this.startPolling();
            }
            catch (Exception e)
            {
                this.eventCheckboxes.forEach(scopeCheckbox -> scopeCheckbox.setActive(true));

                this.currentState = AuthState.ERROR;
                this.statusMessage = Component.translatable("gui.twitchspawn.status_error", e.getMessage());
                this.authorizeButton.active = true;
                this.authorizeButton.setMessage(Component.translatable("gui.twitchspawn.retry_button"));
            }
        });
    }


    private List<String> getSelectedEvents()
    {
        return this.eventCheckboxes.stream().
            filter(ScopeCheckbox::selected).
            map(ScopeCheckbox::getEventType).
            collect(Collectors.toList());
    }


    private String getRequiredScopes()
    {
        Set<String> allRequiredScopes = new HashSet<>();

        this.eventCheckboxes.stream().
            filter(ScopeCheckbox::selected).
            forEach(checkbox ->
            {
                String[] scopes = checkbox.getRequiredScopes().split(" ");
                for (String scope : scopes)
                {
                    if (!scope.trim().isEmpty())
                    {
                        allRequiredScopes.add(scope.trim());
                    }
                }
            });

        return String.join(" ", allRequiredScopes);
    }


    private void startPolling()
    {
        this.pollingTask = CompletableFuture.runAsync(() ->
        {
            long startTime = System.currentTimeMillis();
            long timeoutMs = this.expiresIn * 1000L;

            while (System.currentTimeMillis() - startTime < timeoutMs)
            {
                try
                {
                    Thread.sleep(this.interval * 1000L);

                    if (this.currentState != AuthState.WAITING_FOR_USER)
                    {
                        break;
                    }

                    TwitchApiClient.TokenResponse tokenResponse = this.twitchApiClient.pollForToken(this.deviceCode);

                    if (tokenResponse.success())
                    {
                        this.saveTokensAndEvents(tokenResponse.accessToken(),
                            tokenResponse.refreshToken(),
                            this.getSelectedEvents());

                        this.currentState = AuthState.SUCCESS;
                        this.statusMessage = Component.translatable("gui.twitchspawn.status_authorized");
                        this.authorizeButton.setMessage(Component.translatable("gui.twitchspawn.connected_button"));
                        this.authorizeButton.active = false;
                        break;
                    }
                    else
                    {
                        switch (tokenResponse.error())
                        {
                            case "authorization_pending" ->
                            {
                            }
                            case "slow_down" -> this.interval += 5;
                            case "expired_token" ->
                            {
                                this.currentState = AuthState.ERROR;
                                this.statusMessage = Component.translatable("gui.twitchspawn.status_expired_token");
                            }
                            case "access_denied" ->
                            {
                                this.currentState = AuthState.ERROR;
                                this.statusMessage = Component.translatable("gui.twitchspawn.status_denied");
                            }
                            default ->
                            {
                                this.currentState = AuthState.ERROR;
                                this.statusMessage = Component.translatable("gui.twitchspawn.status_failed", tokenResponse.error());
                            }
                        }
                    }
                }
                catch (InterruptedException e)
                {
                    Thread.currentThread().interrupt();
                    break;
                }
                catch (Exception e)
                {
                    this.currentState = AuthState.ERROR;
                    this.statusMessage = Component.translatable("gui.twitchspawn.status_polling_error", e.getMessage());
                    break;
                }
            }

            if (this.currentState == AuthState.WAITING_FOR_USER)
            {
                this.currentState = AuthState.ERROR;
                this.statusMessage = Component.translatable("gui.twitchspawn.status_timeout");
            }
        });
    }


    private void saveTokensAndEvents(String accessToken, String refreshToken, List<String> selectedEvents)
    {
        // send token to server
        NetworkManager.sendToServer(new SyncStreamerDataPacket.C2S(
            Minecraft.getInstance().player.getName().getString(),
            accessToken,
            refreshToken,
            String.join(" ", selectedEvents)
        ));
    }


    private void openBrowser(String url)
    {
        try
        {
            Util.getPlatform().openUri(url);
            this.statusMessage = Component.translatable("gui.twitchspawn.status_open_browser");
        }
        catch (Exception e)
        {
            this.statusMessage = Component.translatable("gui.twitchspawn.status_failed_to_open", url);
        }
    }


    @Override
    public void onClose()
    {
        if (this.pollingTask != null && !this.pollingTask.isDone())
        {
            this.pollingTask.cancel(true);
        }

        this.twitchApiClient.close();

        super.onClose();
    }


    /**
     * The checkbox that allows to select and deselect scopes.
     */
    private static class ScopeCheckbox
    {
        public ScopeCheckbox(int x, int y, Component message,
            String eventType, String requiredScopes, boolean selected)
        {
            this.checkbox = Checkbox.builder(message, Minecraft.getInstance().font).
                pos(x, y).
                selected(selected).
                build();
            this.eventType = eventType;
            this.requiredScopes = requiredScopes;
        }

        public Checkbox getCheckbox()
        {
            return this.checkbox;
        }

        public void onPress()
        {
            if (this.checkbox.active)
            {
                this.checkbox.onPress();
            }
        }

        public boolean selected()
        {
            return this.checkbox.selected();
        }

        public void setActive(boolean active)
        {
            this.checkbox.active = active;
        }

        public String getEventType()
        {
            return this.eventType;
        }

        public String getRequiredScopes()
        {
            return this.requiredScopes;
        }

        private final Checkbox checkbox;
        private final String eventType;
        private final String requiredScopes;
    }


    /**
     * This is dummy record to easily store information about available events.
     *
     * @param eventType Event type to subscribe.
     * @param description Event description text.
     * @param requiredScopes Event scope.
     * @param defaultSelected Boolean that allows to toggle it.
     */
    private record EventInfo(String eventType, String description, String requiredScopes, boolean defaultSelected)
    {
    }


    private enum AuthState
    {
        READY, LOADING, WAITING_FOR_USER, SUCCESS, ERROR, CONNECTED
    }

    private final TwitchApiClient twitchApiClient;

    private final List<ScopeCheckbox> eventCheckboxes;

    private Button authorizeButton;

    private String userCode;

    private String verificationUri;

    private String deviceCode;

    private int expiresIn;

    private int interval = 5;

    private AuthState currentState = AuthState.READY;

    private Component statusMessage = Component.empty();

    private CompletableFuture<Void> pollingTask;

    // Available events with their required scopes and user-friendly descriptions
    private static final EventInfo[] AVAILABLE_EVENTS = {
        new EventInfo("channel.channel_points_custom_reward_redemption.add",
            "gui.twitchspawn.event_channel_points",
            "channel:read:redemptions", true),
        new EventInfo("channel.chat.message",
            "gui.twitchspawn.event_chat_messages",
            "user:read:chat", true),
        new EventInfo("channel.follow",
            "gui.twitchspawn.event_follows",
            "moderator:read:followers", false),
        new EventInfo("channel.subscribe channel.subscription.message",
            "gui.twitchspawn.event_subscriptions",
            "channel:read:subscriptions", false),
        new EventInfo("channel.subscription.gift",
            "gui.twitchspawn.event_gift_subscriptions",
            "channel:read:subscriptions", false),
        new EventInfo("channel.raid",
            "gui.twitchspawn.event_raids",
            "", false),
        new EventInfo("channel.cheer",
            "gui.twitchspawn.event_bits",
            "bits:read", false)
    };
}
