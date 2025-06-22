package net.programmer.igoodie.twitchspawn.twitchauth;


import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;

import net.programmer.igoodie.twitchspawn.TwitchSpawn;
import net.programmer.igoodie.twitchspawn.configuration.CredentialsConfig;
import okhttp3.*;


/**
 * Centralized Twitch API client for handling all Twitch API interactions
 */
public class TwitchApiClient
{
    public TwitchApiClient()
    {
        this.httpClient = new OkHttpClient();
        this.gson = new Gson();
    }


    /**
     * Request the device code for client.
     * @param scopes The scopes for events.
     * @return The DeviceAuthResponse code.
     * @throws IOException if request failed.
     */
    public DeviceAuthResponse requestDeviceCode(String scopes) throws IOException
    {
        RequestBody requestBody = new FormBody.Builder()
            .add("client_id", TwitchSpawn.APP_ID)
            .add("scopes", scopes)
            .build();

        Request request = new Request.Builder()
            .url(DEVICE_AUTH_URL)
            .post(requestBody)
            .build();

        try (Response response = this.httpClient.newCall(request).execute())
        {
            if (!response.isSuccessful())
            {
                throw new IOException("Device auth request failed: " + response.code());
            }

            String responseBody = response.body().string();
            JsonObject json = this.gson.fromJson(responseBody, JsonObject.class);

            return new DeviceAuthResponse(
                json.get("device_code").getAsString(),
                json.get("user_code").getAsString(),
                json.get("verification_uri").getAsString(),
                json.get("expires_in").getAsInt(),
                json.has("interval") ? json.get("interval").getAsInt() : 5
            );
        }
    }


    /**
     * Wait for access tokens on verification.
     * @param deviceCode The device code for connection.
     * @return The response with client access token.
     * @throws IOException if request failed.
     */
    public TokenResponse pollForToken(String deviceCode) throws IOException
    {
        RequestBody requestBody = new FormBody.Builder()
            .add("client_id", TwitchSpawn.APP_ID)
            .add("device_code", deviceCode)
            .add("grant_type", "urn:ietf:params:oauth:grant-type:device_code")
            .build();

        Request request = new Request.Builder()
            .url(TOKEN_URL)
            .post(requestBody)
            .build();

        try (Response response = this.httpClient.newCall(request).execute())
        {
            String responseBody = response.body().string();
            JsonObject json = this.gson.fromJson(responseBody, JsonObject.class);

            if (response.isSuccessful())
            {
                String accessToken = json.get("access_token").getAsString();
                String refreshToken = json.has("refresh_token") ?
                    json.get("refresh_token").getAsString() : null;

                return TokenResponse.success(accessToken, refreshToken);
            }
            else
            {
                String error = json.has("message") ? json.get("message").getAsString() : "unknown_error";
                return TokenResponse.error(error);
            }
        }
    }


    public void revokeToken(String accessToken) throws IOException
    {
        RequestBody requestBody = new FormBody.Builder().
            add("client_id", TwitchSpawn.APP_ID).
            add("token", accessToken).
            build();

        Request request = new Request.Builder().
            url("https://id.twitch.tv/oauth2/revoke").
            post(requestBody).
            build();

        try (Response response = this.httpClient.newCall(request).execute())
        {
            if (!response.isSuccessful())
            {
                throw new IOException("Failed to revoke token: " + response.code());
            }
        }
    }


    /* token validation */
    public boolean isValidToken(CredentialsConfig.Streamer streamer)
    {
        Request request = new Request.Builder().
            url("https://id.twitch.tv/oauth2/validate").
            addHeader("Authorization", "OAuth " + streamer.twitchAccessToken).
            build();

        try (Response response = this.httpClient.newCall(request).execute())
        {
            return response.isSuccessful();
        }
        catch (IOException e)
        {
            return false;
        }
    }


    /**
     * Close all active connections as everything is received.
     */
    public void close()
    {
        this.httpClient.dispatcher().executorService().shutdown();
        this.httpClient.connectionPool().evictAll();
    }


    public record DeviceAuthResponse(String deviceCode,
                                     String userCode,
                                     String verificationUri,
                                     int expiresIn,
                                     int interval)
    {
    }


    public record TokenResponse(boolean success,
                                String accessToken,
                                String refreshToken,
                                String error)
    {
        public static TokenResponse success(String accessToken, String refreshToken)
        {
            return new TokenResponse(true, accessToken, refreshToken, null);
        }


        public static TokenResponse error(String error)
        {
            return new TokenResponse(false, null, null, error);
        }
    }

    private final OkHttpClient httpClient;

    private final Gson gson;

    public static final String DEVICE_AUTH_URL = "https://id.twitch.tv/oauth2/device";

    public static final String TOKEN_URL = "https://id.twitch.tv/oauth2/token";
}
