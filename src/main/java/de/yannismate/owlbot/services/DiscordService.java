package de.yannismate.owlbot.services;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.channel.Channel.Type;
import discord4j.discordjson.json.MessageData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

@Singleton
public class DiscordService {

  private final Logger logger = LoggerFactory.getLogger(DiscordService.class);
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

  public Mono<MessageData> createMessageInChannel(Snowflake guildId, Snowflake channelId, String message) {
    return this.gateway.getGuildChannels(guildId)
        .filter(gc -> gc.getType() == Type.GUILD_TEXT)
        .filter(gc -> gc.getId().asLong() == channelId.asLong())
        .flatMap(gc -> gc.getRestChannel().createMessage(message))
        .doOnError(error -> logger.atInfo().addArgument(guildId).addArgument(channelId).addArgument(error).log("Failed to send log in G{}:C{}. Error: {}"))
        .take(1).next();
  }

}
