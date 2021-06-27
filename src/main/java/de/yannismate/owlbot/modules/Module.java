package de.yannismate.owlbot.modules;

import discord4j.common.util.Snowflake;

public abstract class Module {

  protected String name = "No name";
  protected String description = "No description";
  protected boolean alwaysActive = false;
  protected boolean beta = false;

  abstract public void enable(Snowflake guildId);
  abstract public void disable(Snowflake guildId);

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }
}
