package de.yannismate.owlbot;

import com.google.inject.AbstractModule;
import de.yannismate.owlbot.providers.BotSettingsProvider;
import de.yannismate.owlbot.providers.DatabaseProvider;
import de.yannismate.owlbot.providers.DiscordProvider;

public class OwlBot extends AbstractModule {

  public static void main(String[] args) {

  }

  @Override
  protected void configure() {
    bind(BotSettingsProvider.class).asEagerSingleton();
    bind(DiscordProvider.class).asEagerSingleton();
    bind(DatabaseProvider.class).asEagerSingleton();
  }

}
