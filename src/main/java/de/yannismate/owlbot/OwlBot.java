package de.yannismate.owlbot;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import de.yannismate.owlbot.providers.BotSettingsProvider;
import de.yannismate.owlbot.providers.DatabaseProvider;
import de.yannismate.owlbot.providers.DiscordProvider;
import de.yannismate.owlbot.providers.ModuleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OwlBot {

  public static void main(String[] args) {
    Injector injector = Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        bind(BotSettingsProvider.class).asEagerSingleton();
        bind(DiscordProvider.class).asEagerSingleton();
        bind(DatabaseProvider.class).asEagerSingleton();
      }
    });
    OwlBot owlBot = new OwlBot(injector);
    owlBot.loadModules();
  }

  private final Logger logger = LoggerFactory.getLogger(OwlBot.class);
  private final Injector injector;
  private final ModuleProvider moduleProvider;

  public OwlBot(Injector injector) {
    logger.atInfo().log("Starting OwlBot");
    this.injector = injector;
    this.moduleProvider = injector.getInstance(ModuleProvider.class);
  }

  private void loadModules() {
    moduleProvider.getAvailableModules().stream()
        .peek(c -> logger.atInfo().addArgument(c.getCanonicalName()).log("Loading Module in {}..."))
        .map(injector::getInstance)
        .peek(m -> logger.atInfo().addArgument(m.getName()).log("Successfully loaded {}"))
        .forEach(moduleProvider::addModule);
  }

}
