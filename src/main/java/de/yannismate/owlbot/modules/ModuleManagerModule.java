package de.yannismate.owlbot.modules;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;

public class ModuleManagerModule extends Module {

  public ModuleManagerModule() {
    this.name = "Module Manager";
    this.description = "Allow enabling, disabling and configuration of bot modules.";
  }

  @Override
  public void enable(Snowflake guildId) {

  }

  @Override
  public void disable(Snowflake guildId) {

  }

  @ModuleCommand(command = "test")
  public void testCommand(MessageCreateEvent event) {
    System.out.println("Command executed!");
  }

}
