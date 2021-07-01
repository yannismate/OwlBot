package de.yannismate.owlbot.util;

import discord4j.core.object.entity.channel.MessageChannel;
import reactor.core.publisher.Mono;

public class MessageUtils {

  public static void createMessageInChannel(Mono<MessageChannel> channel, String message) {
    channel.flatMap(c -> c.createMessage(message)).subscribe();
  }


}
