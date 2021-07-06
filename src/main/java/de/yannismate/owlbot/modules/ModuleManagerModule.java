package de.yannismate.owlbot.modules;

import com.google.inject.Inject;
import de.yannismate.owlbot.OwlBot;
import de.yannismate.owlbot.model.Module;
import de.yannismate.owlbot.model.ModuleCommand;
import de.yannismate.owlbot.services.DatabaseService;
import de.yannismate.owlbot.services.ModuleService;
import de.yannismate.owlbot.util.MessageUtils;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.spec.EmbedCreateSpec;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

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
    Snowflake userId = event.getMember().get().getId();
    String[] args = event.getMessage().getContent().split(" ");
    args = Arrays.copyOfRange(args, 1, args.length);

    if(args.length == 0) {
      db.getGuildSettings(event.getGuildId().get()).thenAccept(guildSettings -> {
        if(guildSettings.isEmpty()) return;
        String prefix = guildSettings.get().getSettings().getPrefix();

        Consumer<EmbedCreateSpec> specConsumer = (spec -> {
          spec.setTitle("Modules");
          spec.setColor(OwlBot.COLOR_NEUTRAL);
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

      if(args[0].equalsIgnoreCase("info")) {
        if(findModule.isEmpty()) {
          MessageUtils.createMessageInChannel(event.getMessage().getChannel(), "<@" + userId.asString() + "> Could not find module " + moduleName);
          return;
        }

        db.getGuildSettings(event.getGuildId().get()).thenAccept(guildSettings -> {

          if(guildSettings.isEmpty()) return;

          boolean isModuleEnabled = guildSettings.get().getEnabledModules().contains(findModule.get().getClass().getSimpleName()) || findModule.get().isAlwaysActive();

          //Get module commands
          var commands = moduleService.getCommandsByModuleClass(findModule.get().getClass());
          List<String> sortedCommands = commands.keySet().stream()
              .map(e -> guildSettings.get().getSettings().getPrefix() + e)
              .sorted(String::compareToIgnoreCase).collect(Collectors.toList());

          String commandsString = sortedCommands.size() > 0 ? String.join("\n", sortedCommands) : "-";

          Consumer<EmbedCreateSpec> specConsumer = (spec -> {
            spec.setTitle(findModule.get().getName());
            spec.setColor(isModuleEnabled ? OwlBot.COLOR_POSITIVE : OwlBot.COLOR_NEGATIVE);
            spec.setDescription(findModule.get().getDescription());
            if(isModuleEnabled) {
              spec.addField("Enabled", "✅" + (findModule.get().isAlwaysActive() ? "🔒" : ""), false);
            } else {
              spec.addField("Enabled", "❌", false);
            }
            spec.addField("Commands", commandsString, false);
            spec.setFooter("OwlBot v" + OwlBot.VERSION, null);
            spec.setTimestamp(Instant.now());
          });

          event.getMessage().getChannel().flatMap(messageChannel -> messageChannel.createEmbed(specConsumer)).subscribe();

        });

      } else if(args[0].equalsIgnoreCase("enable")) {
        //Check if entered module exists
        if(findModule.isEmpty()) {
          MessageUtils.createMessageInChannel(event.getMessage().getChannel(), "<@" + userId.asString() + "> Could not find module " + moduleName);
          return;
        }
        db.getGuildSettings(event.getGuildId().get()).thenAccept(guildSettings -> {
          if(guildSettings.isEmpty()) return;
          //Check if module is deactivated
          if(guildSettings.get().getEnabledModules().contains(findModule.get().getClass().getSimpleName())) {
            MessageUtils.createMessageInChannel(event.getMessage().getChannel(), "<@" + userId.asString() + "> " + findModule.get().getName() + " is already enabled!");
            return;
          }

          //Activate
          guildSettings.get().getEnabledModules().add(findModule.get().getClass().getSimpleName());
          findModule.get().enable(event.getGuildId().get());
          db.updateGuildSettings(guildSettings.get()).thenAccept((v) -> {
            MessageUtils.createMessageInChannel(event.getMessage().getChannel(), "<@" + userId.asString() + "> " + findModule.get().getName() + " enabled.");
          });
        });
      } else if(args[0].equalsIgnoreCase("disable")) {
        //Check if entered module exists
        if(findModule.isEmpty()) {
          MessageUtils.createMessageInChannel(event.getMessage().getChannel(), "<@" + userId.asString() + "> Could not find module " + moduleName);
          return;
        }

        //Check if module can be deactivated
        if(findModule.get().isAlwaysActive()) {
          MessageUtils.createMessageInChannel(event.getMessage().getChannel(), "<@" + userId.asString() + "> " + findModule.get().getName() + " cannot be disabled.");
          return;
        }

        db.getGuildSettings(event.getGuildId().get()).thenAccept(guildSettings -> {
          if(guildSettings.isEmpty()) return;
          //Check if module is active
          if(!guildSettings.get().getEnabledModules().contains(findModule.get().getClass().getSimpleName())) {
            MessageUtils.createMessageInChannel(event.getMessage().getChannel(), "<@" + userId.asString() + "> " + findModule.get().getName() + " is already disabled!");
            return;
          }
          //Deactivate
          guildSettings.get().getEnabledModules().remove(findModule.get().getClass().getSimpleName());
          findModule.get().disable(event.getGuildId().get());
          db.updateGuildSettings(guildSettings.get()).thenAccept((v) -> {
            MessageUtils.createMessageInChannel(event.getMessage().getChannel(), "<@" + userId.asString() + "> " + findModule.get().getName() + " disabled.");
          });
        });
      } else {
        MessageUtils.createMessageInChannel(event.getMessage().getChannel(), "<@" + userId.asString() + "> Unknown subcommand!");
      }
    } else {
      MessageUtils.createMessageInChannel(event.getMessage().getChannel(), "<@" + userId.asString() + "> Invalid amount of arguments!");
    }

  }


}
