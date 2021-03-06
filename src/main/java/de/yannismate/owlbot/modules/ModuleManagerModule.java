package de.yannismate.owlbot.modules;

import com.google.inject.Inject;
import de.yannismate.owlbot.OwlBot;
import de.yannismate.owlbot.model.Module;
import de.yannismate.owlbot.model.ModuleCommand;
import de.yannismate.owlbot.services.DatabaseService;
import de.yannismate.owlbot.services.DiscordService;
import de.yannismate.owlbot.services.ModuleService;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.spec.EmbedCreateSpec;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import reactor.core.publisher.Mono;

public class ModuleManagerModule extends Module {

  @Inject
  private DatabaseService db;

  @Inject
  private ModuleService moduleService;
  
  @Inject
  private DiscordService discordService;

  public ModuleManagerModule() {
    this.name = "ModuleManager";
    this.description = "Allow enabling, disabling and configuration of bot modules.";
    this.alwaysActive = true;
  }

  @ModuleCommand(command = "modules", requiredPermission = "admin.modules.manage")
  public Mono<Void> onModulesCommand(MessageCreateEvent event) {
    Snowflake guildId = event.getGuildId().get();
    Snowflake channelId = event.getMessage().getChannelId();
    return Mono.create(callback -> {
      Snowflake userId = event.getMember().get().getId();
      String[] args = event.getMessage().getContent().split(" ");
      args = Arrays.copyOfRange(args, 1, args.length);

      if(args.length == 0) {
        db.getGuildSettings(guildId).thenAccept(guildSettings -> {
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
                spec.addField(mod.getName(), "???" + (mod.isAlwaysActive() ? "????" : ""), false);
              } else {
                if(mod.isBeta()) return;
                spec.addField(mod.getName(), "???", false);
              }
            });
          });

          event.getMessage().getChannel().flatMap(messageChannel -> messageChannel.createEmbed(specConsumer)).subscribe();
          callback.success();

        });
      } else if(args.length == 2) {
        String moduleName = args[1];
        Optional<Module> findModule = moduleService.getModuleByName(moduleName);

        if(args[0].equalsIgnoreCase("info")) {
          if(findModule.isEmpty()) {
            discordService.createMessageInChannel(guildId, channelId, "<@" + userId.asString() + "> Could not find module " + moduleName).subscribe();
            return;
          }

          db.getGuildSettings(guildId).thenAccept(guildSettings -> {

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
                spec.addField("Enabled", "???" + (findModule.get().isAlwaysActive() ? "????" : ""), false);
              } else {
                spec.addField("Enabled", "???", false);
              }
              spec.addField("Commands", commandsString, false);
              if(findModule.get().getDependencies().length > 0) {
                spec.addField("Dependencies", Arrays.stream(findModule.get().getDependencies())
                        .map(moduleService::getModuleByClass)
                        .map(Module::getName).collect(Collectors.joining(", "))
                    , false);
              }
              spec.setFooter("OwlBot v" + OwlBot.VERSION, null);
              spec.setTimestamp(Instant.now());
            });

            event.getMessage().getChannel().flatMap(messageChannel -> messageChannel.createEmbed(specConsumer)).subscribe();
            callback.success();

          });

        } else if(args[0].equalsIgnoreCase("enable")) {
          //Check if entered module exists
          if(findModule.isEmpty()) {
            discordService.createMessageInChannel(guildId, channelId, "<@" + userId.asString() + "> Could not find module " + moduleName).subscribe();
            return;
          }
          db.getGuildSettings(guildId).thenAccept(guildSettings -> {
            if(guildSettings.isEmpty()) return;
            //Check if module is deactivated
            if(guildSettings.get().getEnabledModules().contains(findModule.get().getClass().getSimpleName())) {
              discordService.createMessageInChannel(guildId, channelId, "<@" + userId.asString() + "> " + findModule.get().getName() + " is already enabled!").subscribe();
              return;
            }

            //Check if module needs dependencies
            String unresolvedDependencies = Arrays.stream(findModule.get().getDependencies())
                .filter(clazz -> !guildSettings.get().getEnabledModules().contains(clazz.getSimpleName()))
                .map(clazz -> moduleService.getModuleByClass(clazz).getName())
                .collect(Collectors.joining(", "));

            if(unresolvedDependencies.length() > 0) {
              discordService.createMessageInChannel(guildId, channelId, "<@" + userId.asString() + "> " + findModule.get().getName() + " depends on one or more disabled Modules: `"
                  + unresolvedDependencies + "`!").subscribe();
              return;
            }

            //Activate
            guildSettings.get().getEnabledModules().add(findModule.get().getClass().getSimpleName());
            findModule.get().onEnableFor(guildId);
            db.updateGuildSettings(guildSettings.get()).thenAccept((v) -> {
              discordService.createMessageInChannel(guildId, channelId, "<@" + userId.asString() + "> " + findModule.get().getName() + " enabled.").subscribe();
            });
            callback.success();
          });
        } else if(args[0].equalsIgnoreCase("disable")) {
          //Check if entered module exists
          if(findModule.isEmpty()) {
            discordService.createMessageInChannel(guildId, channelId, "<@" + userId.asString() + "> Could not find module " + moduleName).subscribe();
            return;
          }

          //Check if module can be deactivated
          if(findModule.get().isAlwaysActive()) {
            discordService.createMessageInChannel(guildId, channelId, "<@" + userId.asString() + "> " + findModule.get().getName() + " cannot be disabled.").subscribe();
            return;
          }

          db.getGuildSettings(guildId).thenAccept(guildSettings -> {
            if(guildSettings.isEmpty()) return;
            //Check if module is active
            if(!guildSettings.get().getEnabledModules().contains(findModule.get().getClass().getSimpleName())) {
              discordService.createMessageInChannel(guildId, channelId, "<@" + userId.asString() + "> " + findModule.get().getName() + " is already disabled!").subscribe();
              return;
            }

            //Check if deactivating the Module would break dependencies
            String unresolvedDependencies = guildSettings.get().getEnabledModules().stream()
                .map(s -> moduleService.getModuleByClassName(s).orElseThrow())
                .filter(module -> Arrays.stream(module.getDependencies()).anyMatch(c -> (c == findModule.get().getClass())))
                .map(Module::getName)
                .collect(Collectors.joining(", "));

            if(unresolvedDependencies.length() > 0) {
              discordService.createMessageInChannel(guildId, channelId, "<@" + userId.asString() + "> " + findModule.get().getName() + " is required by one or more enabled Modules: `"
                  + unresolvedDependencies + "`!").subscribe();
              return;
            }


            //Deactivate
            guildSettings.get().getEnabledModules().remove(findModule.get().getClass().getSimpleName());
            findModule.get().onDisableFor(guildId);
            db.updateGuildSettings(guildSettings.get()).thenAccept((v) -> {
              discordService.createMessageInChannel(guildId, channelId, "<@" + userId.asString() + "> " + findModule.get().getName() + " disabled.").subscribe();
            });
            callback.success();
          });
        } else {
          discordService.createMessageInChannel(guildId, channelId, "<@" + userId.asString() + "> Unknown subcommand!").subscribe();
        }
      } else {
        discordService.createMessageInChannel(guildId, channelId, "<@" + userId.asString() + "> Invalid amount of arguments!").subscribe();
      }
    });

  }


}
