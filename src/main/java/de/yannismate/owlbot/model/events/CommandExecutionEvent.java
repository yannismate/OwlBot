package de.yannismate.owlbot.model.events;

import discord4j.common.util.Snowflake;

public class CommandExecutionEvent {

  private final Snowflake guildId;
  private final Snowflake channelId;
  private final Snowflake userId;

  private final String command;
  private final String[] args;

  public CommandExecutionEvent(Snowflake guildId, Snowflake channelId,
      Snowflake userId, String command, String[] args) {
    this.guildId = guildId;
    this.channelId = channelId;
    this.userId = userId;
    this.command = command;
    this.args = args;
  }

  public Snowflake getGuildId() {
    return guildId;
  }

  public Snowflake getChannelId() {
    return channelId;
  }

  public Snowflake getUserId() {
    return userId;
  }

  public String getCommand() {
    return command;
  }

  public String[] getArgs() {
    return args;
  }
}
