package net.programmer.igoodie.twitchspawn.tracer.socket;

import net.programmer.igoodie.twitchspawn.TwitchSpawn;
import net.programmer.igoodie.twitchspawn.configuration.ConfigManager;
import net.programmer.igoodie.twitchspawn.configuration.CredentialsConfig;
import net.programmer.igoodie.twitchspawn.tracer.Platform;
import net.programmer.igoodie.twitchspawn.tracer.TraceManager;
import net.programmer.igoodie.twitchspawn.tracer.WebSocketTracer;
import net.programmer.igoodie.twitchspawn.tslanguage.event.EventArguments;
import net.programmer.igoodie.twitchspawn.util.CooldownBucket;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.*;


/**
 * This is not smart but very rudimental implementation of EventSub.
 */
public class TwitchEventSubTracer extends WebSocketTracer
{

    private static final String HELIX_API_BASE = "https://api.twitch.tv/helix";

    // Streamer Nickname -> CooldownBucket for chat cooldowns
    private final Map<String, CooldownBucket> cooldownBuckets;

    // Keep alive timer
    private Timer keepAliveTimer;


    public TwitchEventSubTracer(TraceManager manager)
    {
        super(Platform.TWITCH_EVENTSUB, manager);
        this.cooldownBuckets = new HashMap<>();
    }


    @Override
    public void start()
    {
        for (CredentialsConfig.Streamer streamer : ConfigManager.CREDENTIALS.streamers)
        {
            // Validate token before connecting
            if (!validateToken(streamer))
            {
                TwitchSpawn.LOGGER.error("Invalid token for streamer: {}", streamer.twitchNick);
                continue;
            }

            WebSocketListener socket = createSocket(streamer);
            this.sockets.add(startClient(socket));

            // Initialize cooldown bucket for chat
            this.cooldownBuckets.put(streamer.twitchNick,
                new CooldownBucket(ConfigManager.PREFERENCES.chatGlobalCooldown,
                    ConfigManager.PREFERENCES.chatIndividualCooldown));
        }
    }


    @Override
    public void stop()
    {
        for (WebSocket socket : this.sockets)
        {
            if (!socket.close(1000, null))
            {
                socket.cancel();
            }
        }

        if (this.keepAliveTimer != null)
        {
            this.keepAliveTimer.cancel();
            this.keepAliveTimer.purge();
        }

        this.cooldownBuckets.clear();
    }


    @Override
    protected void onOpen(CredentialsConfig.Streamer streamer, WebSocket socket, Response response)
    {
        TwitchSpawn.LOGGER.info("Connected to Twitch EventSub WebSocket for {}", streamer.twitchNick);
    }


    @Override
    protected void onMessage(CredentialsConfig.Streamer streamer, WebSocket socket, String text)
    {
        try
        {
            JSONObject message = new JSONObject(text);

            JSONObject metadata = message.getJSONObject("metadata");
            String messageType = metadata.getString("message_type");

            TwitchSpawn.LOGGER.debug("Received EventSub message type: {} for {}", messageType, streamer.twitchNick);

            switch (messageType)
            {
                case "session_welcome":
                    this.handleSessionWelcome(streamer, message);
                    break;
                case "session_keepalive":
                    // Just acknowledge, no action needed
                    break;
                case "notification":
                    this.handleNotification(streamer, message);
                    break;
                case "session_reconnect":
                    this.handleSessionReconnect(streamer, socket, message);
                    break;
                case "revocation":
                    this.handleRevocation(streamer, message);
                    break;
                default:
                    TwitchSpawn.LOGGER.warn("Unknown EventSub message type: {}", messageType);
            }
        }
        catch (JSONException e)
        {
            TwitchSpawn.LOGGER.error("Error parsing EventSub message", e);
        }
    }


    private void handleSessionWelcome(CredentialsConfig.Streamer streamer, JSONObject message)
    {
        try
        {
            JSONObject payload = message.getJSONObject("payload");
            JSONObject session = payload.getJSONObject("session");
            String sessionId = session.getString("id");
            int keepaliveTimeoutSeconds = session.optInt("keepalive_timeout_seconds", 600);

            TwitchSpawn.LOGGER.info("EventSub session established for {} with ID: {}", streamer.twitchNick, sessionId);

            // Start keepalive timer
            this.startKeepAliveTimer(keepaliveTimeoutSeconds);

            // Subscribe to events
            this.subscribeToEvents(streamer, sessionId);
        }
        catch (JSONException e)
        {
            TwitchSpawn.LOGGER.error("Error handling session welcome", e);
        }
    }


    private void handleNotification(CredentialsConfig.Streamer streamer, JSONObject message)
    {
        try
        {
            JSONObject payload = message.getJSONObject("payload");
            JSONObject subscription = payload.getJSONObject("subscription");
            String subscriptionType = subscription.getString("type");
            JSONObject event = payload.getJSONObject("event");

            switch (subscriptionType)
            {
                case "channel.channel_points_custom_reward_redemption.add":
                    this.handleChannelPointRedemption(streamer, event);
                    break;
                case "channel.chat.message":
                    this.handleChatMessage(streamer, event);
                    break;
                case "channel.follow":
                    this.handleFollow(streamer, event);
                    break;
                case "channel.subscribe":
                    this.handleSubscription(streamer, event);
                    break;
                case "channel.subscription.message":
                    this.handleResubscription(streamer, event);
                    break;
                case "channel.subscription.gift":
                    this.handleGiftSubscription(streamer, event);
                    break;
                case "channel.raid":
                    this.handleRaid(streamer, event);
                    break;
                case "channel.cheer":
                    this.handleBits(streamer, event);
                    break;
                default:
                    TwitchSpawn.LOGGER.debug("Unhandled subscription type: {}", subscriptionType);
            }
        }
        catch (JSONException e)
        {
            TwitchSpawn.LOGGER.error("Error handling notification", e);
        }
    }


    private void handleChannelPointRedemption(CredentialsConfig.Streamer streamer, JSONObject event)
    {
        try
        {
            String title = event.getJSONObject("reward").getString("title");
            int cost = event.getJSONObject("reward").getInt("cost");
            String actorNickname = event.optString("user_name", streamer.twitchNick);
            String actorMessage = event.optString("user_input", "");

            EventArguments eventArguments = new EventArguments("channelPointReward", "twitch");
            eventArguments.streamerNickname = streamer.minecraftNick;
            eventArguments.actorNickname = actorNickname;
            eventArguments.message = actorMessage;
            eventArguments.donationAmount = cost;
            eventArguments.donationCurrency = "Channel Points";
            eventArguments.rewardTitle = title;

            ConfigManager.RULESET_COLLECTION.handleEvent(eventArguments);
        }
        catch (JSONException e)
        {
            TwitchSpawn.LOGGER.error("Error handling channel point redemption", e);
        }
    }


    private void handleChatMessage(CredentialsConfig.Streamer streamer, JSONObject event)
    {
        try
        {
            String username = event.optString("chatter_user_name", "Anonymous");
            String messageText = event.getJSONObject("message").getString("text");

            // Extract badges
            JSONArray badgesArray = event.optJSONArray("badges");
            Map<String, Integer> badges = new HashMap<>();

            if (badgesArray != null)
            {
                for (int i = 0; i < badgesArray.length(); i++)
                {
                    JSONObject badge = badgesArray.getJSONObject(i);
                    badges.put(badge.getString("set_id"), badge.optInt("info", -1));
                }
            }

            // Extract subscription info (if available)
            int subscriptionMonths = 0;

            if (badges.containsKey("subscriber"))
            {
                // EventSub doesn't provide subscription months directly in chat messages
                subscriptionMonths = badges.get("subscriber");
            }

            CooldownBucket cooldownBucket = this.cooldownBuckets.get(streamer.twitchNick);

            EventArguments eventArguments = new EventArguments("chat", "twitch");
            eventArguments.streamerNickname = streamer.minecraftNick;
            eventArguments.actorNickname = username;
            eventArguments.message = messageText;
            eventArguments.subscriptionMonths = subscriptionMonths;
            eventArguments.chatBadges = new HashSet<>(badges.keySet());

            if (cooldownBucket.hasGlobalCooldown())
            {
                TwitchSpawn.LOGGER.info("Still has {} seconds global cooldown.", cooldownBucket.getGlobalCooldown());
            }
            else if (cooldownBucket.canConsume(username))
            {
                ConfigManager.RULESET_COLLECTION.handleEvent(eventArguments, cooldownBucket);
            }
            else if (ConfigManager.RULESET_COLLECTION.getRuleset(streamer.minecraftNick).willPerform(eventArguments))
            {
                if (ConfigManager.PREFERENCES.chatWarnings)
                {
                    // Note: EventSub doesn't allow sending chat messages back
                    // You'd need to implement chat sending via Helix API if needed
                    TwitchSpawn.LOGGER.info(
                        "User {} still has cooldown, but EventSub doesn't support sending chat messages",
                        username);
                }
            }
        }
        catch (JSONException e)
        {
            TwitchSpawn.LOGGER.error("Error handling chat message", e);
        }
    }


    private void handleFollow(CredentialsConfig.Streamer streamer, JSONObject event)
    {
        try
        {
            String followerName = event.getString("user_name");
            String followedAt = event.optString("followed_at", "");

            TwitchSpawn.LOGGER.info("New follower for {}: {}", streamer.twitchNick, followerName);

            EventArguments eventArguments = new EventArguments("follow", "twitch");
            eventArguments.streamerNickname = streamer.minecraftNick;
            eventArguments.actorNickname = followerName;
            eventArguments.message = ""; // Follows don't have messages

            ConfigManager.RULESET_COLLECTION.handleEvent(eventArguments);
        }
        catch (JSONException e)
        {
            TwitchSpawn.LOGGER.error("Error handling follow event", e);
        }
    }


    private void handleSubscription(CredentialsConfig.Streamer streamer, JSONObject event)
    {
        try
        {
            String subscriberName = event.getString("user_name");
            String tier = event.getString("tier"); // "1000", "2000", "3000"
            boolean isGift = event.optBoolean("is_gift", false);

            // Convert tier string to integer (1000 -> 1, 2000 -> 2, 3000 -> 3)
            int subscriptionTier = Integer.parseInt(tier) / 1000;

            TwitchSpawn.LOGGER.info("New subscription for {}: {} (Tier {}, Gift: {})",
                streamer.twitchNick, subscriberName, subscriptionTier, isGift);

            EventArguments eventArguments = new EventArguments("subscription", "twitch");
            eventArguments.streamerNickname = streamer.minecraftNick;
            eventArguments.actorNickname = subscriberName;
            eventArguments.message = ""; // Subscriptions don't have messages
            eventArguments.subscriptionTier = subscriptionTier;
            eventArguments.gifted = isGift;
            eventArguments.subscriptionMonths = 1; // New subscriptions are always 1 month

            ConfigManager.RULESET_COLLECTION.handleEvent(eventArguments);
        }
        catch (JSONException e)
        {
            TwitchSpawn.LOGGER.error("Error handling subscription event", e);
        }
    }


    private void handleResubscription(CredentialsConfig.Streamer streamer, JSONObject event)
    {
        try
        {
            String subscriberName = event.getString("user_name");
            String tier = event.getString("tier"); // "1000", "2000", "3000"
            int cumulativeMonths = event.getInt("cumulative_months");
            int streakMonths = event.optInt("streak_months", 0);
            String resubMessage = event.optString("message", "");

            // Convert tier string to integer (1000 -> 1, 2000 -> 2, 3000 -> 3)
            int subscriptionTier = Integer.parseInt(tier) / 1000;

            TwitchSpawn.LOGGER.info("Resubscription for {}: {} ({} months total, {} streak, Tier {})",
                streamer.twitchNick, subscriberName, cumulativeMonths, streakMonths, subscriptionTier);

            EventArguments eventArguments = new EventArguments("resub", "twitch");
            eventArguments.streamerNickname = streamer.minecraftNick;
            eventArguments.actorNickname = subscriberName;
            eventArguments.message = resubMessage;
            eventArguments.subscriptionTier = subscriptionTier;
            eventArguments.subscriptionMonths = cumulativeMonths;
            eventArguments.gifted = false; // Resub messages are not for gift subs

            ConfigManager.RULESET_COLLECTION.handleEvent(eventArguments);
        }
        catch (JSONException e)
        {
            TwitchSpawn.LOGGER.error("Error handling resubscription event", e);
        }
    }


    private void handleGiftSubscription(CredentialsConfig.Streamer streamer, JSONObject event)
    {
        try
        {
            String gifterName = event.optString("user_name", "Anonymous");
            String recipientName = event.optString("recipient_user_name", "Unknown");
            String tier = event.getString("tier"); // "1000", "2000", "3000"
            int total = event.optInt("total", 1);
            boolean isAnonymous = event.optBoolean("is_anonymous", false);

            // Convert tier string to integer (1000 -> 1, 2000 -> 2, 3000 -> 3)
            int subscriptionTier = Integer.parseInt(tier) / 1000;

            String displayGifterName = isAnonymous ? "Anonymous" : gifterName;

            TwitchSpawn.LOGGER.info("Gift subscription for {}: {} gifted {} sub(s) to {} (Tier {})",
                streamer.twitchNick, displayGifterName, total, recipientName, subscriptionTier);

            EventArguments eventArguments = new EventArguments("subMysteryGift", "twitch");
            eventArguments.streamerNickname = streamer.minecraftNick;
            eventArguments.actorNickname = displayGifterName;
            eventArguments.message = recipientName; // Store recipient name in message field
            eventArguments.subscriptionTier = subscriptionTier;
            eventArguments.subscriptionMonths = total; // Use total field for number of gifts
            eventArguments.gifted = true;

            ConfigManager.RULESET_COLLECTION.handleEvent(eventArguments);
        }
        catch (JSONException e)
        {
            TwitchSpawn.LOGGER.error("Error handling gift subscription event", e);
        }
    }


    private void handleBits(CredentialsConfig.Streamer streamer, JSONObject event)
    {
        try
        {
            String cheererNickname = event.optString("user_name", "Anonymous");
            int bitsAmount = event.getInt("bits");
            String message = event.optString("message", "");
            boolean isAnonymous = event.optBoolean("is_anonymous", false);

            EventArguments eventArguments = new EventArguments("bits", "twitch");
            eventArguments.streamerNickname = streamer.minecraftNick;
            eventArguments.actorNickname = isAnonymous ? "Anonymous" : cheererNickname;
            eventArguments.message = message;
            eventArguments.donationAmount = bitsAmount;
            eventArguments.donationCurrency = "Bits";

            ConfigManager.RULESET_COLLECTION.handleEvent(eventArguments);
        }
        catch (JSONException e)
        {
            TwitchSpawn.LOGGER.error("Error handling bits", e);
        }
    }


    private void handleRaid(CredentialsConfig.Streamer streamer, JSONObject event)
    {
        try
        {
            String raiderNickname = event.getString("from_broadcaster_user_name");
            int raiderCount = event.getInt("viewers");

            EventArguments eventArguments = new EventArguments("raid", "twitch");
            eventArguments.streamerNickname = streamer.minecraftNick;
            eventArguments.actorNickname = raiderNickname;
            eventArguments.raiderCount = raiderCount;
            eventArguments.viewerCount = raiderCount; // Same as raider count for raids
            eventArguments.message = ""; // Raids don't have messages

            ConfigManager.RULESET_COLLECTION.handleEvent(eventArguments);
        }
        catch (JSONException e)
        {
            TwitchSpawn.LOGGER.error("Error handling raid", e);
        }
    }


    private void handleSessionReconnect(CredentialsConfig.Streamer streamer, WebSocket socket, JSONObject message)
    {
        try
        {
            JSONObject payload = message.getJSONObject("payload");
            JSONObject session = payload.getJSONObject("session");
            String reconnectUrl = session.getString("reconnect_url");

            TwitchSpawn.LOGGER.info("EventSub requesting reconnect for {}: {}", streamer.twitchNick, reconnectUrl);

            // Close current connection and reconnect to new URL
            socket.close(1000, "Reconnecting");

            // Create new connection (you'd need to modify startClient to accept custom URL)
            // This is a simplified approach - you might want to implement proper reconnection logic

        }
        catch (JSONException e)
        {
            TwitchSpawn.LOGGER.error("Error handling session reconnect", e);
        }
    }


    private void handleRevocation(CredentialsConfig.Streamer streamer, JSONObject message)
    {
        try
        {
            JSONObject payload = message.getJSONObject("payload");
            JSONObject subscription = payload.getJSONObject("subscription");
            String status = subscription.getString("status");

            TwitchSpawn.LOGGER.error("EventSub subscription revoked for {}: {}", streamer.twitchNick, status);
        }
        catch (JSONException e)
        {
            TwitchSpawn.LOGGER.error("Error handling revocation", e);
        }
    }


    private void subscribeToEvents(CredentialsConfig.Streamer streamer, String sessionId)
    {
        String userId = this.getUserId(streamer);

        if (userId == null)
        {
            TwitchSpawn.LOGGER.error("Could not get user ID for {}", streamer.twitchNick);
            return;
        }

        // Subscribe to channel point redemptions
        this.subscribeToChannelPointRedemptions(streamer, sessionId, userId);

        // Subscribe to chat messages
        this.subscribeToChatMessages(streamer, sessionId, userId);

        // Subscribe to follows
        this.subscribeToFollows(streamer, sessionId, userId);

        // Subscribe to subscriptions
        this.subscribeToSubscriptions(streamer, sessionId, userId);

        // Subscribe to resubscriptions
        this.subscribeToResubscriptions(streamer, sessionId, userId);

        // Subscribe to gift subscriptions
        this.subscribeToGiftSubscriptions(streamer, sessionId, userId);

        // Subscribe to raids
        this.subscribeToRaids(streamer, sessionId, userId);

        // Subscribe to raids
        this.subscribeToBits(streamer, sessionId, userId);
    }


    private void subscribeToChannelPointRedemptions(CredentialsConfig.Streamer streamer,
        String sessionId,
        String userId)
    {
        try
        {
            JSONObject subscription = new JSONObject();
            subscription.put("type", "channel.channel_points_custom_reward_redemption.add");
            subscription.put("version", "1");

            JSONObject condition = new JSONObject();
            condition.put("broadcaster_user_id", userId);
            subscription.put("condition", condition);

            JSONObject transport = new JSONObject();
            transport.put("method", "websocket");
            transport.put("session_id", sessionId);
            subscription.put("transport", transport);

            this.makeHelixApiCall("POST", "/eventsub/subscriptions", streamer, subscription.toString());
        }
        catch (JSONException e)
        {
            TwitchSpawn.LOGGER.error("Error subscribing to channel point redemptions", e);
        }
    }


    private void subscribeToChatMessages(CredentialsConfig.Streamer streamer, String sessionId, String userId)
    {
        try
        {
            JSONObject subscription = new JSONObject();
            subscription.put("type", "channel.chat.message");
            subscription.put("version", "1");

            JSONObject condition = new JSONObject();
            condition.put("broadcaster_user_id", userId);
            condition.put("user_id", userId); // Bot's user ID (same as broadcaster for self-bot)
            subscription.put("condition", condition);

            JSONObject transport = new JSONObject();
            transport.put("method", "websocket");
            transport.put("session_id", sessionId);
            subscription.put("transport", transport);

            this.makeHelixApiCall("POST", "/eventsub/subscriptions", streamer, subscription.toString());
        }
        catch (JSONException e)
        {
            TwitchSpawn.LOGGER.error("Error subscribing to chat messages", e);
        }
    }


    private void subscribeToFollows(CredentialsConfig.Streamer streamer, String sessionId, String userId)
    {
        try
        {
            JSONObject subscription = new JSONObject();
            subscription.put("type", "channel.follow");
            subscription.put("version", "2");

            JSONObject condition = new JSONObject();
            condition.put("broadcaster_user_id", userId);
            condition.put("moderator_user_id", userId); // Required for follow events - using broadcaster as moderator
            subscription.put("condition", condition);

            JSONObject transport = new JSONObject();
            transport.put("method", "websocket");
            transport.put("session_id", sessionId);
            subscription.put("transport", transport);

            this.makeHelixApiCall("POST", "/eventsub/subscriptions", streamer, subscription.toString());
        }
        catch (JSONException e)
        {
            TwitchSpawn.LOGGER.error("Error subscribing to follows", e);
        }
    }


    private void subscribeToSubscriptions(CredentialsConfig.Streamer streamer, String sessionId, String userId)
    {
        try
        {
            JSONObject subscription = new JSONObject();
            subscription.put("type", "channel.subscribe");
            subscription.put("version", "1");

            JSONObject condition = new JSONObject();
            condition.put("broadcaster_user_id", userId);
            subscription.put("condition", condition);

            JSONObject transport = new JSONObject();
            transport.put("method", "websocket");
            transport.put("session_id", sessionId);
            subscription.put("transport", transport);

            this.makeHelixApiCall("POST", "/eventsub/subscriptions", streamer, subscription.toString());
        }
        catch (JSONException e)
        {
            TwitchSpawn.LOGGER.error("Error subscribing to subscriptions", e);
        }
    }


    private void subscribeToResubscriptions(CredentialsConfig.Streamer streamer, String sessionId, String userId)
    {
        try
        {
            JSONObject subscription = new JSONObject();
            subscription.put("type", "channel.subscription.message");
            subscription.put("version", "1");

            JSONObject condition = new JSONObject();
            condition.put("broadcaster_user_id", userId);
            subscription.put("condition", condition);

            JSONObject transport = new JSONObject();
            transport.put("method", "websocket");
            transport.put("session_id", sessionId);
            subscription.put("transport", transport);

            this.makeHelixApiCall("POST", "/eventsub/subscriptions", streamer, subscription.toString());
        }
        catch (JSONException e)
        {
            TwitchSpawn.LOGGER.error("Error subscribing to resubscriptions", e);
        }
    }


    private void subscribeToGiftSubscriptions(CredentialsConfig.Streamer streamer, String sessionId, String userId)
    {
        try
        {
            JSONObject subscription = new JSONObject();
            subscription.put("type", "channel.subscription.gift");
            subscription.put("version", "1");

            JSONObject condition = new JSONObject();
            condition.put("broadcaster_user_id", userId);
            subscription.put("condition", condition);

            JSONObject transport = new JSONObject();
            transport.put("method", "websocket");
            transport.put("session_id", sessionId);
            subscription.put("transport", transport);

            this.makeHelixApiCall("POST", "/eventsub/subscriptions", streamer, subscription.toString());
        }
        catch (JSONException e)
        {
            TwitchSpawn.LOGGER.error("Error subscribing to gift subscriptions", e);
        }
    }


    private void subscribeToBits(CredentialsConfig.Streamer streamer, String sessionId, String userId)
    {
        try
        {
            JSONObject subscription = new JSONObject();
            subscription.put("type", "channel.cheer");
            subscription.put("version", "1");

            JSONObject condition = new JSONObject();
            condition.put("broadcaster_user_id", userId);
            subscription.put("condition", condition);

            JSONObject transport = new JSONObject();
            transport.put("method", "websocket");
            transport.put("session_id", sessionId);
            subscription.put("transport", transport);

            this.makeHelixApiCall("POST", "/eventsub/subscriptions", streamer, subscription.toString());
        }
        catch (JSONException e)
        {
            TwitchSpawn.LOGGER.error("Error subscribing to bits", e);
        }
    }


    private void subscribeToRaids(CredentialsConfig.Streamer streamer, String sessionId, String userId)
    {
        try
        {
            JSONObject subscription = new JSONObject();
            subscription.put("type", "channel.raid");
            subscription.put("version", "1");

            JSONObject condition = new JSONObject();
            condition.put("to_broadcaster_user_id", userId);
            subscription.put("condition", condition);

            JSONObject transport = new JSONObject();
            transport.put("method", "websocket");
            transport.put("session_id", sessionId);
            subscription.put("transport", transport);

            this.makeHelixApiCall("POST", "/eventsub/subscriptions", streamer, subscription.toString());
        }
        catch (JSONException e)
        {
            TwitchSpawn.LOGGER.error("Error subscribing to raids", e);
        }
    }


    private boolean validateToken(CredentialsConfig.Streamer streamer)
    {
        try
        {
            String response = this.makeHelixApiCall("GET", "/users", streamer, null);
            return response != null && !response.isEmpty();
        }
        catch (Exception e)
        {
            TwitchSpawn.LOGGER.error("Token validation failed for {}", streamer.twitchNick, e);
            return false;
        }
    }


    private String getUserId(CredentialsConfig.Streamer streamer)
    {
        try
        {
            String response = this.makeHelixApiCall("GET", "/users", streamer, null);
            if (response != null)
            {
                JSONObject json = new JSONObject(response);
                JSONArray data = json.getJSONArray("data");
                if (data.length() > 0)
                {
                    return data.getJSONObject(0).getString("id");
                }
            }
        }
        catch (Exception e)
        {
            TwitchSpawn.LOGGER.error("Error getting user ID for {}", streamer.twitchNick, e);
        }
        return null;
    }


    private String makeHelixApiCall(String method, String endpoint, CredentialsConfig.Streamer streamer, String body)
    {
        try
        {
            OkHttpClient client = new OkHttpClient();

            Request.Builder requestBuilder = new Request.Builder()
                .url(HELIX_API_BASE + endpoint)
                .addHeader("Authorization", "Bearer " + streamer.twitchAccessToken)
                .addHeader("Client-Id", streamer.twitchClientId)
                .addHeader("Content-Type", "application/json");

            if ("POST".equals(method) && body != null)
            {
                requestBuilder.post(RequestBody.create(MediaType.parse("application/json"), body));
            }
            else
            {
                requestBuilder.get();
            }

            Response response = client.newCall(requestBuilder.build()).execute();

            if (response.isSuccessful())
            {
                return response.body().string();
            }
            else
            {
                TwitchSpawn.LOGGER.error("API call failed: {} {} - {}", method, endpoint, response.code());
                return null;
            }
        }
        catch (IOException e)
        {
            TwitchSpawn.LOGGER.error("Error making API call", e);
            return null;
        }
    }


    private void startKeepAliveTimer(int timeoutSeconds)
    {
        if (this.keepAliveTimer != null)
        {
            this.keepAliveTimer.cancel();
        }

        this.keepAliveTimer = new Timer();
        // Check for keepalive every half of the timeout period
        long period = (timeoutSeconds * 1000L) / 2;

        this.keepAliveTimer.scheduleAtFixedRate(new TimerTask()
        {
            @Override
            public void run()
            {
                // EventSub handles keepalive automatically, this is just for monitoring
                TwitchSpawn.LOGGER.debug("EventSub keepalive check");
            }
        }, period, period);
    }


    @Override
    protected void onClosing(CredentialsConfig.Streamer streamer, WebSocket socket, int code, String reason)
    {
        TwitchSpawn.LOGGER.info("EventSub WebSocket closing for {}: {} - {}", streamer.twitchNick, code, reason);
        socket.close(1000, null);
    }


    @Override
    protected void onFailure(CredentialsConfig.Streamer streamer, WebSocket socket, Throwable t, Response response)
    {
        TwitchSpawn.LOGGER.error("EventSub WebSocket failed for {}", streamer.twitchNick, t);

        this.sockets.remove(socket);

        // Reconnect with a delay 5-second delay
        new Timer().schedule(new TimerTask()
        {
            @Override
            public void run()
            {
                TwitchSpawn.LOGGER.info("Attempting to reconnect EventSub WebSocket for {}", streamer.twitchNick);
                WebSocketListener newSocket = createSocket(streamer);
                WebSocket ws = startClient(newSocket);
                sockets.add(ws);
            }
        }, 5000);
    }
}
