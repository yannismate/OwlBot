package de.yannismate.owlbot.providers;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;

@Singleton
public class DiscordProvider {

  private final DiscordClient client;
  private final GatewayDiscordClient gateway;

  @Inject
  public DiscordProvider(BotSettingsProvider botSettingsProvider) {
    this.client = DiscordClient.create(botSettingsProvider.getDiscordToken());
    this.gateway = client.login().block();
  }

  public DiscordClient getClient() {
    return client;
  }

  public GatewayDiscordClient getGateway() {
    return gateway;
  }

}
