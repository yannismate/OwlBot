package de.yannismate.owlbot.modules;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.yannismate.owlbot.model.Module;
import de.yannismate.owlbot.model.db.ModuleSettings;
import de.yannismate.owlbot.model.db.ModuleSettings.ModuleSettingsValue;
import de.yannismate.owlbot.services.DatabaseService;
import de.yannismate.owlbot.services.DiscordService;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.guild.MemberJoinEvent;
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
    this.description = "Allows logging of joining and leaving users, role changes, bans and command usage.";
    this.registerEvents();
  }

  @Override
  public void enable(Snowflake guildId) {
    db.getModuleSettings(guildId, LoggingModule.class.getSimpleName()).thenAccept(moduleSettings -> {
      if(moduleSettings.isPresent()) return;
      ModuleSettings md = new ModuleSettings(guildId, this.getClass().getSimpleName());
      md.getOptions().put("log_member_join", new ModuleSettingsValue(Map.of(
          "enabled", new ModuleSettingsValue(false),
          "channel", new ModuleSettingsValue(-1L),
          "format", new ModuleSettingsValue("\uD83D\uDCE5 {time} <@{userid}> joined the server. Original name was **{usertag}**. Total members: {membercount}")
      )));

      md.getOptions().put("log_member_leave", new ModuleSettingsValue(Map.of(
          "enabled", new ModuleSettingsValue(false),
          "channel", new ModuleSettingsValue(-1L),
          "format", new ModuleSettingsValue("\uD83D\uDCE4 {time} <@{userid}> left the server. Original name was **{usertag}**. Total members: {membercount}")
      )));

      md.getOptions().put("log_member_role_change", new ModuleSettingsValue(Map.of(
          "enabled", new ModuleSettingsValue(false),
          "channel", new ModuleSettingsValue(-1L),
          "format", new ModuleSettingsValue("\uD83D\uDDD2 {time} **{usertag}'s** roles changed: `{rolediff}`")
      )));

      md.getOptions().put("log_member_banned", new ModuleSettingsValue(Map.of(
          "enabled", new ModuleSettingsValue(false),
          "channel", new ModuleSettingsValue(-1L),
          "format", new ModuleSettingsValue("⛔️ {time} <@{userid}> was banned! Original name was **{usertag}**.")
      )));

      md.getOptions().put("log_command_used", new ModuleSettingsValue(Map.of(
          "enabled", new ModuleSettingsValue(false),
          "channel", new ModuleSettingsValue(-1L),
          "format", new ModuleSettingsValue("\uD83D\uDCE5 {time} **{usertag}'s** executed the command: `{command} {args}` in <#{channelid}>")
      )));

      db.addModuleSettings(md);
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
            ///TODO
      });

    });
  }


}
