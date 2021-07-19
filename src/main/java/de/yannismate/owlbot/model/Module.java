package de.yannismate.owlbot.model;

import discord4j.common.util.Snowflake;

public abstract class Module {

  protected String name = "No name";
  protected String description = "No description";
  protected boolean alwaysActive = false;
  protected boolean beta = false;
  protected Class[] dependencies = {};

  public void postInit() {}

  public void onEnableFor(Snowflake guildId) {}
  public void onDisableFor(Snowflake guildId) {}

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public boolean isAlwaysActive() {
    return alwaysActive;
  }

  public boolean isBeta() {
    return beta;
  }

  public Class[] getDependencies() {
    return dependencies;
  }
}
