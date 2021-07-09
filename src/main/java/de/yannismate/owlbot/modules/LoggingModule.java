package de.yannismate.owlbot.modules;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.yannismate.owlbot.model.Module;
import de.yannismate.owlbot.model.db.ModuleSettings;
import de.yannismate.owlbot.model.db.ModuleSettings.SettingsObject;
import de.yannismate.owlbot.services.DatabaseService;
import de.yannismate.owlbot.services.DiscordService;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.guild.MemberJoinEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class LoggingModule extends Module {

  private final Logger logger = LoggerFactory.getLogger(LoggingModule.class);

  @Inject
  private DatabaseService db;

  @Inject
  private DiscordService discordService;

  public LoggingModule() {
    this.name = "Logging";
    this.description = "Allows logging of joining and leaving users, role changes, kicks, bans and command usage.";
    this.registerEvents();
  }

  @Override
  public void enable(Snowflake guildId) {
    db.getModuleSettings(guildId, LoggingModule.class.getSimpleName()).thenAccept(moduleSettings -> {
      if(moduleSettings.isPresent()) return;
      Map<String, SettingsObject> defaultSettings = new HashMap<>();
      defaultSettings.put("log_member_join", SettingsObject.of(false));
      defaultSettings.put("log_member_join_channel", SettingsObject.of(-1L));
      defaultSettings.put("log_member_leave", SettingsObject.of(false));
      defaultSettings.put("log_member_leave_channel", SettingsObject.of(-1L));
      defaultSettings.put("log_member_role_changed", SettingsObject.of(false));
      defaultSettings.put("log_member_role_changed_channel", SettingsObject.of(-1L));
      defaultSettings.put("log_member_banned", SettingsObject.of(false));
      defaultSettings.put("log_member_banned_channel", SettingsObject.of(-1L));
      defaultSettings.put("log_command_executed", SettingsObject.of(false));
      defaultSettings.put("log_command_executed_channel", SettingsObject.of(-1L));
      ModuleSettings def = new ModuleSettings(guildId, LoggingModule.class.getSimpleName(), defaultSettings);
      db.addModuleSettings(def);
    });
  }

  @Override
  public void disable(Snowflake guildId) {

  }

  private void registerEvents() {
    this.discordService.getGateway().on(MemberJoinEvent.class).subscribe(memberJoinEvent -> {
      db.getGuildSettings(memberJoinEvent.getGuildId())
          .thenApply(guildSettings -> guildSettings.isPresent() && guildSettings.get().getEnabledModules().contains(LoggingModule.class.getSimpleName()))
          .thenCompose(e -> e ? db.getModuleSettings(memberJoinEvent.getGuildId(), LoggingModule.class.getSimpleName()) : CompletableFuture.completedFuture(Optional.empty()))
          .thenAccept(moduleSettings -> {

            if(moduleSettings.isEmpty()) return;
            if(moduleSettings.get().getSettings().get("log_member_join").equals(false)) return;
            Optional<Long> possibleChannel = moduleSettings.get().getSettings().get("log_member_join_channel").getLong();
            if(possibleChannel.isEmpty() || possibleChannel.get() == -1L) return;

            //TODO
            String logMessage = "";

            discordService.getGateway().getChannelById(Snowflake.of(possibleChannel.get()))
                .flatMap(channel -> channel.getRestChannel().createMessage(logMessage))
                .doOnError(error -> logger.atWarn().addArgument(error).log("Error sending log message: {}"))
                .subscribe();
      });

    });
  }


}
