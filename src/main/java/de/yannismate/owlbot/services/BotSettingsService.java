package de.yannismate.owlbot.services;

import com.google.inject.Singleton;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class BotSettingsService {

  private final Logger logger = LoggerFactory.getLogger(BotSettingsService.class);

  private final String discordToken;
  private final int webhooksPort;
  private final String twitchClientId;
  private final String twitchClientSecret;

  public BotSettingsService() throws IOException {
    logger.atInfo().log("Loading bot settings");

    Properties properties = new Properties();
    properties.load(new FileInputStream("settings.properties"));

    this.discordToken = properties.getProperty("DISCORD_TOKEN");
    this.webhooksPort = Integer.parseInt(properties.getProperty("WEBHOOKS_PORT"));
    this.twitchClientId = properties.getProperty("TWITCH_CLIENT_ID");
    this.twitchClientSecret = properties.getProperty("TWITCH_CLIENT_SECRET");
  }

  public String getDiscordToken() {
    return discordToken;
  }

  public int getWebhooksPort() {
    return webhooksPort;
  }

  public String getTwitchClientId() {
    return twitchClientId;
  }

  public String getTwitchClientSecret() {
    return twitchClientSecret;
  }
}
