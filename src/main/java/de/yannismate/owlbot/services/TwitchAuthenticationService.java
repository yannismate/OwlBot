package de.yannismate.owlbot.services;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.util.concurrent.CompletableFuture;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class TwitchAuthenticationService {

  private final static String AUTH_FILE_NAME = "twitch_auth.json";

  private final Logger logger = LoggerFactory.getLogger(TwitchAuthenticationService.class);

  private final String clientId;
  private final String clientSecret;

  private String token;
  private long tokenExpiresAt = 0;

  private final HttpClient client;


  @Inject
  public TwitchAuthenticationService(BotSettingsService botSettingsService)
      throws TwitchRequestException {
    this.clientId = botSettingsService.getTwitchClientId();
    this.clientSecret = botSettingsService.getTwitchClientSecret();
    this.client = HttpClient.newHttpClient();
    File authFile = new File(AUTH_FILE_NAME);
    if(authFile.exists()) {
      try {
        String json = Files.readString(authFile.toPath());
        JSONParser parser = new JSONParser();
        JSONObject authData = (JSONObject) parser.parse(json);
        this.token = (String) authData.get("token");
        this.tokenExpiresAt = (long) authData.get("token_expires_at");
      } catch (IOException | ParseException e) {
        logger.atError().addArgument(e).log("Could not read existing twitch auth file. {}");
      }
    }

    if(token == null || tokenExpiresAt < System.currentTimeMillis()) requestNewToken();

  }

  private void requestNewToken() throws TwitchRequestException {
    String parameters = "?client_id=" + clientId
        + "&client_secret=" + clientSecret
        + "&grant_type=client_credentials";
    HttpRequest request = HttpRequest.newBuilder()
        .POST(BodyPublishers.noBody())
        .uri(URI.create("https://id.twitch.tv/oauth2/token" + parameters))
        .build();
    HttpResponse<String> response = null;
    try {
      response = client.send(request, BodyHandlers.ofString());
    } catch (IOException | InterruptedException e) {
      throw new TwitchRequestException("Post request to id.twitch.tv failed", e);
    }
    JSONParser parser = new JSONParser();

    JSONObject responseJson = null;
    try {
      responseJson = (JSONObject) parser.parse(response.body());
    } catch (ParseException | ClassCastException e) {
      throw new TwitchRequestException("Could not parse response JSON", e);
    }

    this.tokenExpiresAt = System.currentTimeMillis() + (1000L * (long)responseJson.get("expires_in"));
    this.token = (String) responseJson.get("access_token");
    logger.atInfo().log("Successfully requested new Twitch Auth token.");

    JSONObject tokenInfo = new JSONObject();
    tokenInfo.put("token", this.token);
    tokenInfo.put("token_expires_at", tokenExpiresAt);
    try {
      Files.writeString(new File(AUTH_FILE_NAME).toPath(), tokenInfo.toJSONString());
    } catch (IOException e) {
      logger.atError().addArgument(e).log("Could not save twitch auth data in file. {}");
    }
    logger.atInfo().log("Successfully saved new token info to file.");
  }


  public CompletableFuture<String> getAppToken() {
    return CompletableFuture.supplyAsync(() -> {
      if(tokenExpiresAt < System.currentTimeMillis()) {
        try {
          requestNewToken();
        } catch (TwitchRequestException e) {
          logger.atError().addArgument(e).log("Could not refresh Twitch App Token! {}");
          return null;
        }
      }
      return this.token;
    });
  }

  static class TwitchRequestException extends Exception {
    public TwitchRequestException(String message, Throwable cause) {
      super(message, cause);
    }
  }


}
