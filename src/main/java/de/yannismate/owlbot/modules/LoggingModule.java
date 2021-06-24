package de.yannismate.owlbot.modules;

import com.google.inject.Singleton;
import discord4j.common.util.Snowflake;

@Singleton
public class LoggingModule implements Module {

  @Override
  public String getName() {
    return "Logging";
  }

  @Override
  public String getDescription() {
    return "Allows logging of joining and leaving users, role changes, kicks, bans and command usage.";
  }

  @Override
  public void enable(Snowflake guildId) {

  }

  @Override
  public void disable(Snowflake guildId) {

  }
}
