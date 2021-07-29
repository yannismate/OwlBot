package de.yannismate.owlbot.modules;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.yannismate.owlbot.OwlBot;
import de.yannismate.owlbot.model.Module;
import de.yannismate.owlbot.model.ModuleCommand;
import de.yannismate.owlbot.model.db.CachedMessage;
import de.yannismate.owlbot.model.db.ModuleSettings;
import de.yannismate.owlbot.model.db.ModuleSettings.ModuleSettingsValue;
import de.yannismate.owlbot.model.events.CommandExecutionEvent;
import de.yannismate.owlbot.services.BotEventService;
import de.yannismate.owlbot.services.DatabaseService;
import de.yannismate.owlbot.services.DiscordService;
import de.yannismate.owlbot.util.MessageUtils;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.guild.BanEvent;
import discord4j.core.event.domain.guild.MemberJoinEvent;
import discord4j.core.event.domain.guild.MemberLeaveEvent;
import discord4j.core.event.domain.guild.MemberUpdateEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.event.domain.message.MessageDeleteEvent;
import discord4j.core.event.domain.message.MessageUpdateEvent;
import discord4j.core.object.entity.Role;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import reactor.core.publisher.Mono;

@Singleton
public class LoggingModule extends Module {

  @Inject
  private DatabaseService db;

  @Inject
  private DiscordService discordService;

  @Inject
  private BotEventService botEventService;

  @Inject
  private DatabaseService databaseService;

  private final Set<String> availableLoggingTypes = Set.of("member_join", "member_leave", "member_role_change",
      "member_banned", "command_executed", "message_deleted", "message_edited");

  public LoggingModule() {
    this.name = "Logging";
    this.description = "Allows logging of joining and leaving users, role changes, bans and command usage.";
  }

  @Override
  public void postInit() {
    this.registerEvents();
  }

  @Override
  public void onEnableFor(Snowflake guildId) {
    db.getModuleSettings(guildId, LoggingModule.class.getSimpleName()).thenAccept(moduleSettings -> {
      if(moduleSettings.isPresent()) return;
      ModuleSettings md = new ModuleSettings(guildId, this.getClass().getSimpleName());
      md.getOptions().put("log_member_join", new ModuleSettingsValue(Map.of(
          "enabled", new ModuleSettingsValue(false),
          "channel", new ModuleSettingsValue(-1L),
          "format", new ModuleSettingsValue("\uD83D\uDCE5 ${time} <@${userid}> joined the server. Original name was **${usertag}**. Total members: ${membercount}")
      )));

      md.getOptions().put("log_member_leave", new ModuleSettingsValue(Map.of(
          "enabled", new ModuleSettingsValue(false),
          "channel", new ModuleSettingsValue(-1L),
          "format", new ModuleSettingsValue("\uD83D\uDCE4 ${time} <@${userid}> left the server. Original name was **${usertag}**. Total members: ${membercount}")
      )));

      md.getOptions().put("log_member_role_change", new ModuleSettingsValue(Map.of(
          "enabled", new ModuleSettingsValue(false),
          "channel", new ModuleSettingsValue(-1L),
          "format", new ModuleSettingsValue("\uD83D\uDDD2 ${time} **${usertag}'s** roles changed: `${rolediff}`")
      )));

      md.getOptions().put("log_member_banned", new ModuleSettingsValue(Map.of(
          "enabled", new ModuleSettingsValue(false),
          "channel", new ModuleSettingsValue(-1L),
          "format", new ModuleSettingsValue("⛔️ ${time} <@${userid}> was banned with reason `${reason}`! Original name was **${usertag}**.")
      )));

      md.getOptions().put("log_command_executed", new ModuleSettingsValue(Map.of(
          "enabled", new ModuleSettingsValue(false),
          "channel", new ModuleSettingsValue(-1L),
          "format", new ModuleSettingsValue("\uD83E\uDD16 ${time} **${usertag}'s** executed the command: `${command} ${args}` in <#${channelid}>")
      )));

      md.getOptions().put("log_message_deleted", new ModuleSettingsValue(Map.of(
          "enabled", new ModuleSettingsValue(false),
          "channel", new ModuleSettingsValue(-1L),
          "format", new ModuleSettingsValue("\uD83D\uDDD1 ${time} **Channel:** <#${channelid}> **${usertag}'s** message got deleted. Content: ```${oldcontent}```")
      )));

      md.getOptions().put("log_message_edited", new ModuleSettingsValue(Map.of(
          "enabled", new ModuleSettingsValue(false),
          "channel", new ModuleSettingsValue(-1L),
          "format", new ModuleSettingsValue("✎ ${time} **Channel:** <#${channelid}> **${usertag}** edited their message. Old content: ```${oldcontent}```")
      )));

      db.addModuleSettings(md);
    });
  }

  private void registerEvents() {
    this.discordService.getGateway().on(MemberJoinEvent.class).subscribe(memberJoinEvent -> {
      Snowflake guildId = memberJoinEvent.getGuildId();
      db.getGuildSettings(guildId)
          .thenApply(guildSettings -> guildSettings.isPresent() && guildSettings.get().getEnabledModules().contains(LoggingModule.class.getSimpleName()))
          .thenCompose(e -> e ? db.getModuleSettings(guildId, LoggingModule.class.getSimpleName()) : CompletableFuture.completedFuture(Optional.empty()))
          .thenAccept(moduleSettings -> {

            if(moduleSettings.isEmpty()) return;

            Map<String, ModuleSettingsValue> eventSettings = moduleSettings.get().getOptions().get("log_member_join").getNested().orElseThrow();
            if(!(Boolean)eventSettings.get("enabled").getRaw()) return;
            if(eventSettings.get("channel").getRaw().equals(-1L)) return;

            String format = (String) eventSettings.get("format").getRaw();
            Snowflake channelId = Snowflake.of((Long) eventSettings.get("channel").getRaw());

            if(format.contains("${membercount}")) {
              memberJoinEvent.getGuild().subscribe(guild -> {
                String message = MessageUtils.replaceTokens(format, Map.of(
                    "time", "<t:" + Instant.now().getEpochSecond() + ">",
                    "userid", memberJoinEvent.getMember().getId().asString(),
                    "usertag", memberJoinEvent.getMember().getTag(),
                    "membercount", "" + guild.getMemberCount()
                ));
                discordService.createMessageInChannel(guildId, channelId, message).subscribe();

              });

            } else {

              String message = MessageUtils.replaceTokens(format, Map.of(
                  "time", "<t:" + Instant.now().getEpochSecond() + ">",
                  "userid", memberJoinEvent.getMember().getId().asString(),
                  "usertag", memberJoinEvent.getMember().getTag()
              ));

              discordService.createMessageInChannel(guildId, channelId, message).subscribe();

            }

          });

    });

    this.discordService.getGateway().on(MemberLeaveEvent.class).subscribe(memberLeaveEvent -> {
      if(memberLeaveEvent.getMember().isEmpty()) return;
      Snowflake guildId = memberLeaveEvent.getGuildId();
      db.getGuildSettings(guildId)
          .thenApply(guildSettings -> guildSettings.isPresent() && guildSettings.get().getEnabledModules().contains(LoggingModule.class.getSimpleName()))
          .thenCompose(e -> e ? db.getModuleSettings(guildId, LoggingModule.class.getSimpleName()) : CompletableFuture.completedFuture(Optional.empty()))
          .thenAccept(moduleSettings -> {

            if(moduleSettings.isEmpty()) return;

            Map<String, ModuleSettingsValue> eventSettings = moduleSettings.get().getOptions().get("log_member_leave").getNested().orElseThrow();
            if(!(Boolean)eventSettings.get("enabled").getRaw()) return;
            if(eventSettings.get("channel").getRaw().equals(-1L)) return;

            String format = (String) eventSettings.get("format").getRaw();
            Snowflake channelId = Snowflake.of((Long) eventSettings.get("channel").getRaw());

            if(format.contains("${membercount}")) {
              memberLeaveEvent.getGuild().subscribe(guild -> {
                String message = MessageUtils.replaceTokens(format, Map.of(
                    "time", "<t:" + Instant.now().getEpochSecond() + ">",
                    "userid", memberLeaveEvent.getMember().get().getId().asString(),
                    "usertag", memberLeaveEvent.getMember().get().getTag(),
                    "membercount", "" + guild.getMemberCount()
                ));
                discordService.createMessageInChannel(guildId, channelId, message).subscribe();

              });

            } else {

              String message = MessageUtils.replaceTokens(format, Map.of(
                  "time", "<t:" + Instant.now().getEpochSecond() + ">",
                  "userid", memberLeaveEvent.getMember().get().getId().asString(),
                  "usertag", memberLeaveEvent.getMember().get().getTag()
              ));

              discordService.createMessageInChannel(guildId, channelId, message).subscribe();

            }

          });

    });

    this.discordService.getGateway().on(MemberUpdateEvent.class).subscribe(memberUpdateEvent -> {
      if(memberUpdateEvent.getOld().isEmpty()) return;
      Snowflake guildId = memberUpdateEvent.getGuildId();

      Set<Snowflake> oldRoles = memberUpdateEvent.getOld().get().getRoleIds();
      Set<Snowflake> newRoles = memberUpdateEvent.getCurrentRoles();

      if(oldRoles.equals(newRoles)) return;

      db.getGuildSettings(guildId)
          .thenApply(guildSettings -> guildSettings.isPresent() && guildSettings.get().getEnabledModules().contains(LoggingModule.class.getSimpleName()))
          .thenCompose(e -> e ? db.getModuleSettings(guildId, LoggingModule.class.getSimpleName()) : CompletableFuture.completedFuture(Optional.empty()))
          .thenAccept(moduleSettings -> {

            if(moduleSettings.isEmpty()) return;

            Map<String, ModuleSettingsValue> eventSettings = moduleSettings.get().getOptions().get("log_member_role_change").getNested().orElseThrow();
            if(!(Boolean)eventSettings.get("enabled").getRaw()) return;
            if(eventSettings.get("channel").getRaw().equals(-1L)) return;


            this.discordService.getGateway().getGuildRoles(guildId)
                .filter(role -> oldRoles.contains(role.getId()) || newRoles.contains(role.getId()))
                .collectMap(Role::getId, Role::getName)
                .subscribe(roleNames -> {

                  String format = (String) eventSettings.get("format").getRaw();
                  Snowflake channelId = Snowflake.of((Long) eventSettings.get("channel").getRaw());

                  memberUpdateEvent.getMember().subscribe(member -> {

                    List<String> changes = new ArrayList<>();
                    oldRoles.stream().filter(r -> !newRoles.contains(r)).map(r -> "-" + roleNames.get(r)).forEach(changes::add);
                    newRoles.stream().filter(r -> !oldRoles.contains(r)).map(r -> "+" + roleNames.get(r)).forEach(changes::add);
                    String changesString = String.join(", ", changes);

                    String message = MessageUtils.replaceTokens(format, Map.of(
                        "time", "<t:" + Instant.now().getEpochSecond() + ">",
                        "userid", memberUpdateEvent.getMemberId().asString(),
                        "usertag", member.getTag(),
                        "oldroles", oldRoles.stream().map(roleNames::get).collect(Collectors.joining(", ")),
                        "newroles", newRoles.stream().map(roleNames::get).collect(Collectors.joining(", ")),
                        "rolediff", changesString
                    ));

                    discordService.createMessageInChannel(guildId, channelId, message).subscribe();

                  });

            });


          });
    });

    this.discordService.getGateway().on(BanEvent.class).subscribe(banEvent -> {
      Snowflake guildId = banEvent.getGuildId();

      db.getGuildSettings(guildId)
          .thenApply(guildSettings -> guildSettings.isPresent() && guildSettings.get().getEnabledModules().contains(LoggingModule.class.getSimpleName()))
          .thenCompose(e -> e ? db.getModuleSettings(guildId, LoggingModule.class.getSimpleName()) : CompletableFuture.completedFuture(Optional.empty()))
          .thenAccept(moduleSettings -> {

            if(moduleSettings.isEmpty()) return;

            Map<String, ModuleSettingsValue> eventSettings = moduleSettings.get().getOptions().get("log_member_banned").getNested().orElseThrow();
            if(!(Boolean)eventSettings.get("enabled").getRaw()) return;
            if(eventSettings.get("channel").getRaw().equals(-1L)) return;

            String format = (String) eventSettings.get("format").getRaw();
            Snowflake channelId = Snowflake.of((Long) eventSettings.get("channel").getRaw());

            if(format.contains("${reason}")) {
              this.discordService.getGateway().getGuildById(guildId).flatMap(g -> g.getBan(banEvent.getUser().getId())).subscribe(ban -> {
                String message = MessageUtils.replaceTokens(format, Map.of(
                    "time", "<t:" + Instant.now().getEpochSecond() + ">",
                    "userid", banEvent.getUser().getId().asString(),
                    "usertag", banEvent.getUser().getTag(),
                    "reason", ban.getReason().orElse("-")
                ));

                discordService.createMessageInChannel(guildId, channelId, message).subscribe();
              });
            } else {
              String message = MessageUtils.replaceTokens(format, Map.of(
                  "time", "<t:" + Instant.now().getEpochSecond() + ">",
                  "userid", banEvent.getUser().getId().asString(),
                  "usertag", banEvent.getUser().getTag()
                  ));

              discordService.createMessageInChannel(guildId, channelId, message).subscribe();
            }

          });

    });

    this.botEventService.on(CommandExecutionEvent.class, commandExecutionEvent -> {

      Snowflake guildId = commandExecutionEvent.getGuildId();

      db.getGuildSettings(guildId).thenAccept(guildSettings -> {
        if(guildSettings.isEmpty()) return;
        if(!guildSettings.get().getEnabledModules().contains(LoggingModule.class.getSimpleName())) return;

        db.getModuleSettings(guildId, LoggingModule.class.getSimpleName()).thenAccept(moduleSettings -> {

          if(moduleSettings.isEmpty()) return;

          Map<String, ModuleSettingsValue> eventSettings = moduleSettings.get().getOptions().get("log_command_executed").getNested().orElseThrow();
          if(!(Boolean)eventSettings.get("enabled").getRaw()) return;
          if(eventSettings.get("channel").getRaw().equals(-1L)) return;


          String format = (String) eventSettings.get("format").getRaw();
          Snowflake channelId = Snowflake.of((Long) eventSettings.get("channel").getRaw());

          String message = MessageUtils.replaceTokens(format, Map.of(
              "time", "<t:" + Instant.now().getEpochSecond() + ">",
              "userid", commandExecutionEvent.getMember().getId().asString(),
              "usertag", commandExecutionEvent.getMember().getTag(),
              "command", guildSettings.get().getSettings().getPrefix() + commandExecutionEvent.getCommand(),
              "args", String.join(" ", commandExecutionEvent.getArgs()),
              "channelid", commandExecutionEvent.getChannelId().asString()
          ));

          discordService.createMessageInChannel(guildId, channelId, message).subscribe();


        });

      });



    });

    this.discordService.getGateway().on(MessageCreateEvent.class).subscribe(messageCreateEvent -> {
      if(messageCreateEvent.getMember().isEmpty()) return;

      String content = messageCreateEvent.getMessage().getContent();
      content += messageCreateEvent.getMessage().getEmbeds().stream()
          .filter(embed -> embed.getImage().isPresent())
          .map(embed -> " " + embed.getImage().get().getProxyUrl())
          .collect(Collectors.joining());

      CachedMessage msg = new CachedMessage(messageCreateEvent.getMessage().getId(), content,
          messageCreateEvent.getMember().get().getId(), messageCreateEvent.getMember().get().getTag());
      databaseService.insertCachedMessage(msg);
    });

    this.discordService.getGateway().on(MessageDeleteEvent.class).subscribe(messageDeleteEvent -> {

      if(messageDeleteEvent.getGuildId().isEmpty()) return;
      Snowflake guildId = messageDeleteEvent.getGuildId().get();

      db.getGuildSettings(guildId)
          .thenApply(guildSettings -> guildSettings.isPresent() && guildSettings.get().getEnabledModules().contains(LoggingModule.class.getSimpleName()))
          .thenCompose(e -> e ? db.getModuleSettings(guildId, LoggingModule.class.getSimpleName()) : CompletableFuture.completedFuture(Optional.empty()))
          .thenAccept(moduleSettings -> {

            if(moduleSettings.isEmpty()) return;

            Map<String, ModuleSettingsValue> eventSettings = moduleSettings.get().getOptions().get("log_message_deleted").getNested().orElseThrow();
            if(!(Boolean)eventSettings.get("enabled").getRaw()) return;
            if(eventSettings.get("channel").getRaw().equals(-1L)) return;

            String format = (String) eventSettings.get("format").getRaw();
            Snowflake channelId = Snowflake.of((Long) eventSettings.get("channel").getRaw());

            if(messageDeleteEvent.getMessage().isPresent() && messageDeleteEvent.getMessage().get().getAuthor().isPresent()) {

              String content = messageDeleteEvent.getMessage().get().getContent();
              content += messageDeleteEvent.getMessage().get().getEmbeds().stream()
                  .filter(embed -> embed.getImage().isPresent())
                  .map(embed -> " " + embed.getImage().get().getProxyUrl())
                  .collect(Collectors.joining());

              String message = MessageUtils.replaceTokens(format, Map.of(
                  "time", "<t:" + Instant.now().getEpochSecond() + ">",
                  "channelid", messageDeleteEvent.getChannelId().asString(),
                  "userid", messageDeleteEvent.getMessage().get().getAuthor().get().getId().asString(),
                  "oldcontent", MessageUtils.escapeForMultiLineCodeBlock(content),
                  "usertag", messageDeleteEvent.getMessage().get().getAuthor().get().getTag()
              ));

              discordService.createMessageInChannel(guildId, channelId, message).subscribe();

            } else {
              db.getCachedMessage(messageDeleteEvent.getMessageId()).thenAccept(cachedMsg -> {

                if(cachedMsg.isEmpty()) return;

                String message = MessageUtils.replaceTokens(format, Map.of(
                    "time", "<t:" + Instant.now().getEpochSecond() + ">",
                    "channelid", messageDeleteEvent.getChannelId().asString(),
                    "userid", cachedMsg.get().getSenderId().asString(),
                    "oldcontent", MessageUtils.escapeForMultiLineCodeBlock(cachedMsg.get().getContent()),
                    "usertag", cachedMsg.get().getSenderTag()
                ));

                discordService.createMessageInChannel(guildId, channelId, message).subscribe();

              });
            }

          });


    });

    this.discordService.getGateway().on(MessageUpdateEvent.class).subscribe(messageUpdateEvent -> {

      if(messageUpdateEvent.getGuildId().isEmpty()) return;
      Snowflake guildId = messageUpdateEvent.getGuildId().get();

      db.getGuildSettings(guildId)
          .thenApply(guildSettings -> guildSettings.isPresent() && guildSettings.get().getEnabledModules().contains(LoggingModule.class.getSimpleName()))
          .thenCompose(e -> e ? db.getModuleSettings(guildId, LoggingModule.class.getSimpleName()) : CompletableFuture.completedFuture(Optional.empty()))
          .thenAccept(moduleSettings -> {

            if(moduleSettings.isEmpty()) return;

            Map<String, ModuleSettingsValue> eventSettings = moduleSettings.get().getOptions().get("log_message_edited").getNested().orElseThrow();
            if(!(Boolean)eventSettings.get("enabled").getRaw()) return;
            if(eventSettings.get("channel").getRaw().equals(-1L)) return;

            String format = (String) eventSettings.get("format").getRaw();
            Snowflake channelId = Snowflake.of((Long) eventSettings.get("channel").getRaw());

            if(messageUpdateEvent.getOld().isPresent() && messageUpdateEvent.getOld().get().getAuthor().isPresent()) {

              String content = messageUpdateEvent.getOld().get().getContent();
              content += messageUpdateEvent.getOld().get().getEmbeds().stream()
                  .filter(embed -> embed.getImage().isPresent())
                  .map(embed -> " " + embed.getImage().get().getProxyUrl())
                  .collect(Collectors.joining());

              String message = MessageUtils.replaceTokens(format, Map.of(
                  "time", "<t:" + Instant.now().getEpochSecond() + ">",
                  "channelid", messageUpdateEvent.getChannelId().asString(),
                  "userid", messageUpdateEvent.getOld().get().getAuthor().get().getId().asString(),
                  "oldcontent", MessageUtils.escapeForMultiLineCodeBlock(content),
                  "usertag", messageUpdateEvent.getOld().get().getAuthor().get().getTag()
              ));

              discordService.createMessageInChannel(guildId, channelId, message).subscribe();

            } else {
              db.getCachedMessage(messageUpdateEvent.getMessageId()).thenAccept(cachedMsg -> {

                if(cachedMsg.isEmpty()) return;

                String message = MessageUtils.replaceTokens(format, Map.of(
                    "time", "<t:" + Instant.now().getEpochSecond() + ">",
                    "channelid", messageUpdateEvent.getChannelId().asString(),
                    "userid", cachedMsg.get().getSenderId().asString(),
                    "oldcontent", MessageUtils.escapeForMultiLineCodeBlock(cachedMsg.get().getContent()),
                    "usertag", cachedMsg.get().getSenderTag()
                ));

                discordService.createMessageInChannel(guildId, channelId, message).subscribe();

                String content = messageUpdateEvent.getCurrentContent().orElse("");
                content += messageUpdateEvent.getCurrentEmbeds().stream()
                    .filter(embed -> embed.getImage().isPresent())
                    .map(embed -> " " + embed.getImage().get().getProxyUrl())
                    .collect(Collectors.joining());

                cachedMsg.get().setContent(content);
                db.updateCachedMessage(cachedMsg.get());

              });
            }

          });

    });

  }

  @ModuleCommand(command = "logging", requiredPermission = "admin.logging.manage")
  public Mono<Void> onModulesCommand(MessageCreateEvent event) {
    Snowflake guildId = event.getGuildId().get();
    Snowflake channelId = event.getMessage().getChannelId();
    Snowflake userId = event.getMember().get().getId();

    return Mono.create(callback -> {

      String[] argsTemp = event.getMessage().getContent().split(" ");
      final String[] args = Arrays.copyOfRange(argsTemp, 1, argsTemp.length);

      db.getGuildSettings(guildId).thenAccept(guildSettings -> {
        if(guildSettings.isEmpty()) return;

        db.getModuleSettings(guildId, LoggingModule.class.getSimpleName()).thenAccept(moduleSettings -> {
          if(moduleSettings.isEmpty()) return;


          Function<String, Boolean> isEnabled = name -> (Boolean) moduleSettings.get().getOptions().get("log_" + name).getNested().orElse(Map.of("enabled", new ModuleSettingsValue(false))).get("enabled").getRaw();
          Function<String, String> format = name -> (String) moduleSettings.get().getOptions().get("log_" + name).getNested().orElse(Map.of("format", new ModuleSettingsValue("?"))).get("format").getRaw();
          Function<String, Long> channel = name -> (Long) moduleSettings.get().getOptions().get("log_" + name).getNested().orElse(Map.of("channel", new ModuleSettingsValue(-1L))).get("channel").getRaw();
          Function<String, String> tokens = name -> {
            switch (name.toLowerCase()) {
              case "member_join":
              case "member_leave":
                return "${time}, ${userid}, ${usertag}, ${membercount}";
              case "member_role_change":
                return "${time}, ${userid}, ${usertag}, ${oldroles}, ${newroles}, ${rolediff}";
              case "member_banned":
                return "${time}, ${userid}, ${usertag}, ${reason}";
              case "command_executed":
                return "${time}, ${userid}, ${usertag}, ${command}, ${args}, ${channelid}";
              case "message_deleted":
              case "message_edited":
                return "${time}, ${channelid}, ${userid}, ${usertag}, ${oldcontent}";
            }
            return "";
          };

          if(args.length == 0) {
            String prefix = guildSettings.get().getSettings().getPrefix();
            event.getMessage().getChannel().flatMap(messageChannel -> messageChannel.createEmbed(spec -> {
              spec.setTitle("Logging");
              spec.setColor(OwlBot.COLOR_NEUTRAL);
              spec.setDescription("`" + prefix + "logging info [type]` - Shows current settings of a logging type\n"
                  + "`" + prefix + "logging enable [type]` - Enable logging type\n"
                  + "`" + prefix + "logging disable [type]` - Enable logging type\n"
                  + "`" + prefix + "logging set channel [type] [channel]` - Set channel for messages of the given logging type\n"
                  + "`" + prefix + "logging set format [type] [format]` - Set format for given logging type");
              spec.setFooter("OwlBot v" + OwlBot.VERSION, null);
              spec.setTimestamp(Instant.now());
              spec.addField("member_join", (isEnabled.apply("member_join") ? "✅" : "❌"), false);
              spec.addField("member_leave", (isEnabled.apply("member_leave") ? "✅" : "❌"), false);
              spec.addField("member_role_change", (isEnabled.apply("member_role_change") ? "✅" : "❌"), false);
              spec.addField("member_banned", (isEnabled.apply("member_banned") ? "✅" : "❌"), false);
              spec.addField("command_executed", (isEnabled.apply("command_executed") ? "✅" : "❌"), false);
              spec.addField("message_deleted", (isEnabled.apply("message_deleted") ? "✅" : "❌"), false);
              spec.addField("message_edited", (isEnabled.apply("message_edited") ? "✅" : "❌"), false);
            })).subscribe();
            callback.success();
          } else if(args.length == 1) {
            discordService.createMessageInChannel(guildId, channelId, "<@" + userId.asString() + "> Invalid amount of arguments!").subscribe();
          } else if(args.length == 2) {
            if(args[0].equalsIgnoreCase("info")) {

              if(!availableLoggingTypes.contains(args[1].toLowerCase())) {
                discordService.createMessageInChannel(guildId, channelId, "<@" + userId.asString() + "> Invalid logging type!").subscribe();
                return;
              }

              event.getMessage().getChannel().flatMap(messageChannel -> messageChannel.createEmbed(spec -> {
                spec.setTitle("Logging " + args[1].toLowerCase());
                spec.setColor(isEnabled.apply(args[1]) ? OwlBot.COLOR_POSITIVE : OwlBot.COLOR_NEGATIVE);
                spec.setFooter("OwlBot v" + OwlBot.VERSION, null);
                spec.setTimestamp(Instant.now());
                spec.addField("Enabled", (isEnabled.apply(args[1]) ? "✅" : "❌"), false);
                spec.addField("Channel", "<#" + channel.apply(args[1]) + ">", false);
                spec.addField("Format", "`" + MessageUtils.escapeForSingleLineCodeBlock(format.apply(args[1])) + "`", false);
                spec.addField("Tokens", tokens.apply(args[1]), false);
              })).subscribe();
              callback.success();
            } else if(args[0].equalsIgnoreCase("enable")) {
              if(!availableLoggingTypes.contains(args[1].toLowerCase())) {
                discordService.createMessageInChannel(guildId, channelId, "<@" + userId.asString() + "> Invalid logging type!").subscribe();
                return;
              }
              if(isEnabled.apply(args[1])) {
                discordService.createMessageInChannel(guildId, channelId, "<@" + userId.asString() + "> Logging type is already enabled!").subscribe();
                return;
              }

              moduleSettings.get().getOptions().get("log_" + args[1].toLowerCase()).getNested().orElse(new HashMap<>()).put("enabled", new ModuleSettingsValue(true));
              db.updateModuleSettings(moduleSettings.get()).thenAccept(v -> {
                discordService.createMessageInChannel(guildId, channelId, "<@" + userId.asString() + "> Logging type " + args[1].toLowerCase() +  " successfully enabled!").subscribe();
                callback.success();
              });

            } else if(args[0].equalsIgnoreCase("disable")) {
              if(!availableLoggingTypes.contains(args[1].toLowerCase())) {
                discordService.createMessageInChannel(guildId, channelId, "<@" + userId.asString() + "> Invalid logging type!").subscribe();
                return;
              }
              if(!isEnabled.apply(args[1])) {
                discordService.createMessageInChannel(guildId, channelId, "<@" + userId.asString() + "> Logging type is already disabled!").subscribe();
                return;
              }

              moduleSettings.get().getOptions().get("log_" + args[1].toLowerCase()).getNested().orElse(new HashMap<>()).put("enabled", new ModuleSettingsValue(false));
              db.updateModuleSettings(moduleSettings.get()).thenAccept(v -> {
                discordService.createMessageInChannel(guildId, channelId, "<@" + userId.asString() + "> Logging type " + args[1].toLowerCase() +  " successfully disabled!").subscribe();
                callback.success();
              });

            } else {
              discordService.createMessageInChannel(guildId, channelId, "<@" + userId.asString() + "> Unknown subcommand!").subscribe();
            }
          } else if(args[0].equalsIgnoreCase("set")) {
            if(args[1].equalsIgnoreCase("channel")) {

              if(!availableLoggingTypes.contains(args[2].toLowerCase())) {
                discordService.createMessageInChannel(guildId, channelId, "<@" + userId.asString() + "> Invalid logging type!").subscribe();
                return;
              }

              if(!args[3].startsWith("<#") || !args[3].endsWith(">")) {
                discordService.createMessageInChannel(guildId, channelId, "<@" + userId.asString() + "> Please provide a channel as the 4th argument!").subscribe();
                return;
              }

              long newChannelId;
              try {
                newChannelId = Long.parseLong(args[3].substring(2, args[3].length() - 1));
              } catch (NumberFormatException e){
                discordService.createMessageInChannel(guildId, channelId, "<@" + userId.asString() + "> Please provide a channel as the 4th argument!").subscribe();
                return;
              }

              moduleSettings.get().getOptions().get("log_" + args[2].toLowerCase()).getNested().orElse(new HashMap<>()).put("channel", new ModuleSettingsValue(newChannelId));

              db.updateModuleSettings(moduleSettings.get()).thenAccept(v -> {
                discordService.createMessageInChannel(guildId, channelId, "<@" + userId.asString() + "> Channel for " + args[2].toLowerCase() + " successfully updated!").subscribe();
                callback.success();
              });

            } else if(args[1].equalsIgnoreCase("format")) {

              if(!availableLoggingTypes.contains(args[2].toLowerCase())) {
                discordService.createMessageInChannel(guildId, channelId, "<@" + userId.asString() + "> Invalid logging type!").subscribe();
                return;
              }

              String newFormat = String.join(" ", Arrays.copyOfRange(args, 3, args.length));

              moduleSettings.get().getOptions().get("log_" + args[2].toLowerCase()).getNested().orElse(new HashMap<>()).put("format", new ModuleSettingsValue(newFormat));

              db.updateModuleSettings(moduleSettings.get()).thenAccept(v -> {
                discordService.createMessageInChannel(guildId, channelId, "<@" + userId.asString() + "> Format for " + args[2].toLowerCase() + " successfully updated!").subscribe();
                callback.success();
              });

            } else {
              discordService.createMessageInChannel(guildId, channelId, "<@" + userId.asString() + "> Unknown subcommand!").subscribe();
            }
          } else {
            discordService.createMessageInChannel(guildId, channelId, "<@" + userId.asString() + "> Unknown subcommand!").subscribe();
          }

        });

      });


    });
  }


}
