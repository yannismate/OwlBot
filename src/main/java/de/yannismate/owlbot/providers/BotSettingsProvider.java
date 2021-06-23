package de.yannismate.owlbot.providers;

import com.google.inject.Singleton;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class BotSettingsProvider {

  private final Logger logger = LoggerFactory.getLogger(BotSettingsProvider.class);

  private final String discordToken;

  public BotSettingsProvider() throws IOException {
    logger.atInfo().log("Loading bot settings");

    Properties properties = new Properties();
    properties.load(new FileInputStream("settings.properties"));

    this.discordToken = properties.getProperty("DISCORD_TOKEN");
  }

  public String getDiscordToken() {
    return discordToken;
  }

}
