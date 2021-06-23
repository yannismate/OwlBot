package de.yannismate.owlbot.modules;

import discord4j.common.util.Snowflake;

public interface Module {

  String getName();
  String getDescription();

  void enable(Snowflake guildId);
  void disable(Snowflake guildId);

}
