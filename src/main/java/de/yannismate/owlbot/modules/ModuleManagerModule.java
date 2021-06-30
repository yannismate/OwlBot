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
    }

    if(args.length == 2) {
      if(args[0].toLowerCase().equals("info")) {

      } else if(args[0].toLowerCase().equals("enable")) {

      } else if(args[0].toLowerCase().equals("disable")) {

      } else {
        event.getMessage().getChannel().flatMap(messageChannel -> messageChannel.createMessage("<@" + event.getMember().get().getId() + "> Unknown subcommand!")).subscribe();
      }
    } else {
      event.getMessage().getChannel().flatMap(messageChannel -> messageChannel.createMessage("<@" + event.getMember().get().getId() + "> Invalid amount of arguments!")).subscribe();
    }

  }


}
