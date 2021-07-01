package de.yannismate.owlbot.modules;

import com.google.inject.Inject;
import de.yannismate.owlbot.OwlBot;
import de.yannismate.owlbot.services.DatabaseService;
import de.yannismate.owlbot.services.ModuleService;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.spec.EmbedCreateSpec;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Consumer;

public class ModuleManagerModule extends Module {

  @Inject
  private DatabaseService db;

  @Inject
  private ModuleService moduleService;

  public ModuleManagerModule() {
    this.name = "ModuleManager";
    this.description = "Allow enabling, disabling and configuration of bot modules.";
    this.alwaysActive = true;
  }

  @Override
  public void enable(Snowflake guildId) {}

  @Override
  public void disable(Snowflake guildId) {}

  @ModuleCommand(command = "modules", requiredPermission = "admin.managemodules")
  public void onModulesCommand(MessageCreateEvent event) {
    String[] args = event.getMessage().getContent().split(" ");
    args = Arrays.copyOfRange(args, 1, args.length);

    if(args.length == 0) {
      db.getGuildSettings(event.getGuildId().get()).thenAccept(guildSettings -> {
        if(guildSettings.isEmpty()) return;
        String prefix = guildSettings.get().getSettings().getPrefix();

        Consumer<EmbedCreateSpec> specConsumer = (spec -> {
          spec.setTitle("Modules");
          spec.setDescription(prefix + "`modules info [module]` - Shows info about a module\n"
              + prefix + "`modules enable [module]` - Enable module\n"
              + prefix + "`modules disable [module]` - Disable module");
          spec.setFooter("OwlBot v" + OwlBot.VERSION, null);
          spec.setTimestamp(Instant.now());
          moduleService.getAvailableModules().forEach(clazz -> {
            Module mod = moduleService.getModuleByClass(clazz);
            if(guildSettings.get().getEnabledModules().contains(clazz.getSimpleName()) || mod.isAlwaysActive()) {
              spec.addField(mod.getName(), "✅" + (mod.isAlwaysActive() ? "🔒" : ""), false);
            } else {
              if(mod.isBeta()) return;
              spec.addField(mod.getName(), "❌", false);
            }
          });
        });

        event.getMessage().getChannel().flatMap(messageChannel -> messageChannel.createEmbed(specConsumer)).subscribe();

      });
    } else if(args.length == 2) {
      String moduleName = args[1];
      Optional<Module> findModule = moduleService.getModuleByName(moduleName);

      if(args[0].toLowerCase().equals("info")) {
        if(findModule.isEmpty()) {
          event.getMessage().getChannel().flatMap(c -> c.createMessage("<@" + event.getMember().get().getId().asString() + "> Could not find module " + findModule)).subscribe();
          return;
        }
      } else if(args[0].toLowerCase().equals("enable")) {
        if(findModule.isEmpty()) {
          event.getMessage().getChannel().flatMap(c -> c.createMessage("<@" + event.getMember().get().getId().asString() + "> Could not find module " + findModule)).subscribe();
          return;
        }
        db.getGuildSettings(event.getGuildId().get()).thenAccept(guildSettings -> {
          if(guildSettings.isEmpty()) return;
          if(guildSettings.get().getEnabledModules().contains(findModule.get().getClass().getSimpleName())) {
            event.getMessage().getChannel().flatMap(c -> c.createMessage("<@" + event.getMember().get().getId().asString() + "> " + findModule.get().getName() + " is already enabled!")).subscribe();
            return;
          }
          guildSettings.get().getEnabledModules().add(findModule.get().getClass().getSimpleName());
          findModule.get().enable(event.getGuildId().get());
          db.updateGuildSettings(guildSettings.get()).thenAccept((v) -> {
            event.getMessage().getChannel().flatMap(c -> c.createMessage("<@" + event.getMember().get().getId().asString() + "> " + findModule.get().getName() + " enabled.")).subscribe();
          });
        });
      } else if(args[0].toLowerCase().equals("disable")) {
        if(findModule.isEmpty()) {
          event.getMessage().getChannel().flatMap(c -> c.createMessage("<@" + event.getMember().get().getId().asString() + "> Could not find module " + findModule)).subscribe();
          return;
        }
        if(findModule.get().isAlwaysActive()) {
          event.getMessage().getChannel().flatMap(c -> c.createMessage("<@" + event.getMember().get().getId().asString() + "> " + findModule.get().getName() + " cannot be deactivated.")).subscribe();
          return;
        }
        db.getGuildSettings(event.getGuildId().get()).thenAccept(guildSettings -> {
          if(guildSettings.isEmpty()) return;
          if(!guildSettings.get().getEnabledModules().contains(findModule.get().getClass().getSimpleName())) {
            event.getMessage().getChannel().flatMap(c -> c.createMessage("<@" + event.getMember().get().getId().asString() + "> " + findModule.get().getName() + " is already disabled!")).subscribe();
            return;
          }
          guildSettings.get().getEnabledModules().remove(findModule.get().getClass().getSimpleName());
          findModule.get().disable(event.getGuildId().get());
          db.updateGuildSettings(guildSettings.get()).thenAccept((v) -> {
            event.getMessage().getChannel().flatMap(c -> c.createMessage("<@" + event.getMember().get().getId().asString() + "> " + findModule.get().getName() + " disabled.")).subscribe();
          });
        });
      } else {
        event.getMessage().getChannel().flatMap(messageChannel -> messageChannel.createMessage("<@" + event.getMember().get().getId().asString() + "> Unknown subcommand!")).subscribe();
      }
    } else {
      event.getMessage().getChannel().flatMap(messageChannel -> messageChannel.createMessage("<@" + event.getMember().get().getId().asString() + "> Invalid amount of arguments!")).subscribe();
    }

  }


}
