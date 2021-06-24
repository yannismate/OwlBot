package de.yannismate.owlbot.services;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;

@Singleton
public class DiscordService {

  private final DiscordClient client;
  private final GatewayDiscordClient gateway;

  @Inject
  public DiscordService(BotSettingsService botSettingsService) {
    this.client = DiscordClient.create(botSettingsService.getDiscordToken());
    this.gateway = client.login().block();
  }

  public DiscordClient getClient() {
    return client;
  }

  public GatewayDiscordClient getGateway() {
    return gateway;
  }

}
