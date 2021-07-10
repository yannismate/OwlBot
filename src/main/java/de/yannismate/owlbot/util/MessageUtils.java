package de.yannismate.owlbot.util;

import discord4j.core.object.entity.channel.MessageChannel;
import reactor.core.publisher.Mono;

public class MessageUtils {

  public static void createMessageInChannel(Mono<MessageChannel> channel, String message) {
    channel.flatMap(c -> c.createMessage(message)).subscribe();
  }

  public static String escapeForSingleLineCodeBlock(String content) {
    return content.replace("`", "'");
  }

  public static String escapeForMultiLineCodeBlock(String content) {
    return content.replace("`", "\u200E`");
  }


}
