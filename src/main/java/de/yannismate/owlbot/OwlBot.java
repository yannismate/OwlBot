package de.yannismate.owlbot;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import de.yannismate.owlbot.model.Module;
import de.yannismate.owlbot.services.BotEventService;
import de.yannismate.owlbot.services.BotSettingsService;
import de.yannismate.owlbot.services.DatabaseService;
import de.yannismate.owlbot.services.DiscordService;
import de.yannismate.owlbot.services.ModuleService;
import de.yannismate.owlbot.services.TwitchAuthenticationService;
import de.yannismate.owlbot.services.WebhooksService;
import discord4j.rest.util.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OwlBot {

  public static final String VERSION = "1.0-DEV";
  public static final Color COLOR_NEUTRAL = Color.of(0, 133, 235);
  public static final Color COLOR_POSITIVE = Color.of(0, 179, 30);
  public static final Color COLOR_NEGATIVE = Color.of(219, 0, 0);

  public static void main(String[] args) {
    Injector injector = Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        bind(BotSettingsService.class).asEagerSingleton();
        bind(DiscordService.class).asEagerSingleton();
        bind(DatabaseService.class).asEagerSingleton();
        bind(BotEventService.class).asEagerSingleton();
        bind(TwitchAuthenticationService.class).asEagerSingleton();
        bind(WebhooksService.class).asEagerSingleton();
      }
    });
    OwlBot owlBot = new OwlBot(injector);
    owlBot.loadModules();
  }

  private final Logger logger = LoggerFactory.getLogger(OwlBot.class);
  private final Injector injector;
  private final ModuleService moduleService;

  public OwlBot(Injector injector) {
    logger.atInfo().log("Starting OwlBot");
    this.injector = injector;
    this.moduleService = injector.getInstance(ModuleService.class);
  }

  private void loadModules() {
    moduleService.getAvailableModules().stream()
        .peek(c -> logger.atInfo().addArgument(c.getCanonicalName()).log("Loading Module in {}..."))
        .map(injector::getInstance)
        .peek(Module::postInit)
        .peek(m -> logger.atInfo().addArgument(m.getName()).log("Successfully loaded {}"))
        .forEach(moduleService::addModule);
  }

}
