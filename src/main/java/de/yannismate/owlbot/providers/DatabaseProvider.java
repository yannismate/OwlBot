package de.yannismate.owlbot.providers;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class DatabaseProvider {

  @Inject
  private BotSettingsProvider botSettingsProvider;

}