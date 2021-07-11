package de.yannismate.owlbot.model.events;

import de.yannismate.owlbot.model.BotEvent;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Member;

public class CommandExecutionEvent implements BotEvent {

  private final Snowflake guildId;
  private final Snowflake channelId;
  private final Member member;

  private final String command;
  private final String[] args;

  public CommandExecutionEvent(Snowflake guildId, Snowflake channelId,
      Member member, String command, String[] args) {
    this.guildId = guildId;
    this.channelId = channelId;
    this.member = member;
    this.command = command;
    this.args = args;
  }

  public Snowflake getGuildId() {
    return guildId;
  }

  public Snowflake getChannelId() {
    return channelId;
  }

  public Member getMember() {
    return member;
  }

  public String getCommand() {
    return command;
  }

  public String[] getArgs() {
    return args;
  }
}
