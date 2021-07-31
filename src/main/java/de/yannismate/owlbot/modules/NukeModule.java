package de.yannismate.owlbot.modules;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.re2j.Pattern;
import com.google.re2j.PatternSyntaxException;
import de.yannismate.owlbot.OwlBot;
import de.yannismate.owlbot.model.Module;
import de.yannismate.owlbot.model.ModuleCommand;
import de.yannismate.owlbot.model.db.CachedUserJoin;
import de.yannismate.owlbot.model.db.Nuke;
import de.yannismate.owlbot.model.db.Nuke.NukeMode;
import de.yannismate.owlbot.model.db.Nuke.NukeOptions;
import de.yannismate.owlbot.model.db.Nuke.NukeOptionsAccountCreationTime;
import de.yannismate.owlbot.model.db.Nuke.NukeOptionsJoinTime;
import de.yannismate.owlbot.model.db.Nuke.NukeOptionsNameRegex;
import de.yannismate.owlbot.services.DatabaseService;
import de.yannismate.owlbot.services.DiscordService;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.guild.MemberJoinEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.spec.EmbedCreateSpec;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.bson.Document;
import reactor.core.publisher.Mono;

@Singleton
public class NukeModule extends Module {

  @Inject
  private DatabaseService db;

  @Inject
  private DiscordService discordService;

  public NukeModule() {
    this.name = "Nuke";
    this.description = "Allows mass-banning of accounts based on different identifying parameters.";
    this.dependencies = new Class[]{LoggingModule.class};
  }

  @Override
  public void postInit() {
    discordService.getGateway().on(MemberJoinEvent.class).subscribe(memberJoinEvent -> {
      String avatarUrl = memberJoinEvent.getMember().getAvatarUrl();
      avatarUrl = avatarUrl.substring(avatarUrl.lastIndexOf("/") + 1);
      avatarUrl = avatarUrl.split("\\.")[0];

      CachedUserJoin userJoin = new CachedUserJoin(memberJoinEvent.getGuildId(),
          memberJoinEvent.getMember().getId(),
          new Date(memberJoinEvent.getMember().getJoinTime().toEpochMilli()),
          memberJoinEvent.getMember().getUsername(),
          memberJoinEvent.getMember().getId().getTimestamp().toEpochMilli(),
          (avatarUrl.length() > 5 ? avatarUrl : null)
          );
      db.insertOrUpdateMemberJoin(userJoin).thenAccept(v->{});
    });

  }

  @ModuleCommand(command = "nuke", requiredPermission = "moderator.nuke")
  public Mono<Void> onModulesCommand(MessageCreateEvent event) {
    Snowflake guildId = event.getGuildId().get();
    Snowflake channelId = event.getMessage().getChannelId();
    Snowflake userId = event.getMember().get().getId();
    return Mono.create(callback -> {

      String[] args = event.getMessage().getContent().split(" ");
      args = Arrays.copyOfRange(args, 1, args.length);

      if(args.length == 0) {

        db.getGuildSettings(guildId).thenAccept(guildSettings -> {
          if(guildSettings.isEmpty()) return;
          String prefix = guildSettings.get().getSettings().getPrefix();

          Consumer<EmbedCreateSpec> specConsumer = (spec -> {
            spec.setTitle("Nuke");
            spec.setColor(OwlBot.COLOR_NEUTRAL);
            spec.setDescription("`" + prefix + "nuke info [id]` - Shows info about a previously executed nuke\n"
                + "`" + prefix + "nuke [type] [arguments...] - Collect accounts for nuke`");
            spec.addField(prefix + "nuke join_time",
                "[from] - Time frame beginning (Message ID or ISO 8601 UTC timestamp)\n"
                    + "[to] - Time frame ending (Message ID, or ISO 8601 UTC timestamp)\n"
                    + "[excluded] - (optional) Comma seperated list of ignored user IDs",
                false);
            spec.addField(prefix + "nuke name_regex",
                "[regex] - RE2 Regex to check names for\n"
                    + "[lookback] - Time to look back in minutes",
                false);
            spec.addField(prefix + "nuke account_creation_time",
                "[time] - ACT of example account (User ID or ISO 8601 UTC timestamp)\n"
                    + "[radius] - Time radius to match other accounts in minutes\n"
                    + "[lookback] - Time to look back in minutes",
                false);
            spec.addField(prefix + "nuke profile_picture",
                "[hash] - Profile picture hash of example user (User ID or image hash)\n"
                    + "[lookback] - Time to look back in minutes",
                false);
            spec.setFooter("OwlBot v" + OwlBot.VERSION, null);
            spec.setTimestamp(Instant.now());
          });

          event.getMessage().getChannel().flatMap(messageChannel -> messageChannel.createEmbed(specConsumer)).subscribe();
          callback.success();

        });

      } else if(args[0].equalsIgnoreCase("join_time")) {
        if(args.length != 3 && args.length != 4) {
          discordService.createMessageInChannel(guildId, channelId,
              "<@" + userId.asString() + "> Invalid amount of arguments!").subscribe();
          return;
        }

        Date from = null;
        Date to = null;
        List<Snowflake> excluded = new ArrayList<>();

        try {
          long longFrom = Long.parseLong(args[1]);
          from = new Date(Snowflake.of(longFrom).getTimestamp().toEpochMilli());
        } catch (NumberFormatException e) {
          try {
            Instant instantFrom = Instant.parse(args[1]);
            from = Date.from(instantFrom);
          } catch (DateTimeParseException e2) {
            discordService.createMessageInChannel(guildId, channelId,
                "<@" + userId.asString() + "> Please provide a valid message ID or ISO 8601 timestamp as the 2nd argument!").subscribe();
            return;
          }
        }

        try {
          long longTo = Long.parseLong(args[2]);
          to = new Date(Snowflake.of(longTo).getTimestamp().toEpochMilli());
        } catch (NumberFormatException e) {
          try {
            Instant instantFrom = Instant.parse(args[2]);
            to = Date.from(instantFrom);
          } catch (DateTimeParseException e2) {
            discordService.createMessageInChannel(guildId, channelId,
                "<@" + userId.asString() + "> Please provide a valid message ID or ISO 8601 timestamp as the 3rd argument!").subscribe();
            return;
          }
        }

        if(args.length == 4) {
          for(String usr : args[3].split(",")) {
            try {
              long usrId = Long.parseLong(usr);
              excluded.add(Snowflake.of(usrId));
            } catch (NumberFormatException e) {
              discordService.createMessageInChannel(guildId, channelId,
                  "<@" + userId.asString() + "> Please provide a list of comma seperated user ids as the 4th argument!").subscribe();
              return;
            }
          }
        }

        Document filter = new Document();

        filter.append("guild_id", guildId.asLong());

        filter.append("joined_at", new Document().append("$gt", from).append("$lt", to));

        final Date finalFrom = from;
        final Date finalTo = to;
        db.getMemberJoinsWithFilter(filter).thenAccept(affectedUsers -> {
          affectedUsers = affectedUsers.stream().filter(u -> !excluded.contains(u.getUserId())).collect(Collectors.toList());

          List<CachedUserJoin> finalAffectedUsers = affectedUsers;

          NukeOptionsJoinTime nukeOptions = new NukeOptionsJoinTime(finalFrom, finalTo, excluded);
          Nuke nuke = new Nuke();
          nuke.setGuildId(guildId);
          nuke.setExecutionDate(new Date());
          nuke.setNukeMode(NukeMode.JOIN_TIME);
          nuke.setNukeOptions(nukeOptions);
          nuke.setExecutedBy(userId);
          nuke.setAffectedUsers(affectedUsers.stream().map(CachedUserJoin::getUserId).collect(Collectors.toList()));

          db.insertNuke(nuke).thenAccept(nukeId -> {
            event.getGuild().subscribe(guild -> {

              Mono<Void> doBans = finalAffectedUsers.stream()
                  .map(u -> guild.ban(u.getUserId(), ban -> {
                    ban.setDeleteMessageDays(7);
                    ban.setReason("Nuke " + nukeId);
                  }))
                  .reduce(Mono::when)
                  .orElse(Mono.empty());

              doBans.doOnSuccess(v ->
                  event.getMessage().getChannel().flatMap(channel ->
                      channel.createEmbed(embed -> {
                        embed.setTitle("Completed Nuke");
                        embed.setFooter("OwlBot v" + OwlBot.VERSION, null);
                        embed.setTimestamp(Instant.now());
                        embed.addField("ID", nukeId, false);
                        embed.addField("Affected Users", finalAffectedUsers.size() + "", false);
                        embed.addField("Executed by", "<@" + event.getMember().get().getId().asString() + ">", false);
                        embed.setColor(finalAffectedUsers.size() > 0 ? OwlBot.COLOR_POSITIVE : OwlBot.COLOR_NEGATIVE);
                      })
                  ).subscribe()
              ).subscribe();


            });
          });

        });


      } else if(args[0].equalsIgnoreCase("name_regex")) {
        if(args.length < 3) {
          discordService.createMessageInChannel(guildId, channelId,
              "<@" + userId.asString() + "> Invalid amount of arguments!").subscribe();
          return;
        }

        String regexStr = String.join(" ", Arrays.copyOfRange(args, 1, args.length - 1));

        Pattern regex = null;
        try {
          regex = Pattern.compile(regexStr);
        } catch (PatternSyntaxException e) {
          discordService.createMessageInChannel(guildId, channelId,
              "<@" + userId.asString() + "> Please provide a valid RE2 regex as the 2nd argument!").subscribe();
          return;
        }
        Date from = null;
        try {
          int mins = Integer.parseInt(args[args.length - 1]);
          from = new Date(System.currentTimeMillis() - ((long) mins * 60*1000));
        } catch (NumberFormatException e) {
          discordService.createMessageInChannel(guildId, channelId,
              "<@" + userId.asString() + "> Please provide an integer as the 3rd argument!").subscribe();
          return;
        }

        Document filter = new Document();

        filter.append("guild_id", guildId.asLong());
        filter.append("joined_at", new Document("$gt", from));

        NukeOptionsNameRegex nukeOptions = new NukeOptionsNameRegex(regex.pattern(), from);

        Pattern finalRegex = regex;
        db.getMemberJoinsWithFilter(filter).thenAccept(users -> {
          List<Snowflake> ids = users.stream()
              .filter(j -> finalRegex.matches(j.getName()))
              .map(CachedUserJoin::getUserId)
              .collect(Collectors.toList());

          Nuke nuke = new Nuke();
          nuke.setGuildId(guildId);
          nuke.setExecutionDate(new Date());
          nuke.setNukeMode(NukeMode.NAME_REGEX);
          nuke.setNukeOptions(nukeOptions);
          nuke.setExecutedBy(userId);
          nuke.setAffectedUsers(ids);

          db.insertNuke(nuke).thenAccept(nukeId ->
            event.getGuild().subscribe(guild -> {

              Mono<Void> doBans = ids.stream()
                  .map(u -> guild.ban(u, ban -> {
                    ban.setDeleteMessageDays(7);
                    ban.setReason("Nuke " + nukeId);
                  }))
                  .reduce(Mono::when)
                  .orElse(Mono.empty());

              doBans.doOnSuccess(v ->
                  event.getMessage().getChannel().flatMap(channel ->
                      channel.createEmbed(embed -> {
                        embed.setTitle("Completed Nuke");
                        embed.setFooter("OwlBot v" + OwlBot.VERSION, null);
                        embed.setTimestamp(Instant.now());
                        embed.addField("ID", nukeId, false);
                        embed.addField("Affected Users", ids.size() + "", false);
                        embed.addField("Executed by", "<@" + event.getMember().get().getId().asString() + ">", false);
                        embed.setColor(ids.size() > 0 ? OwlBot.COLOR_POSITIVE : OwlBot.COLOR_NEGATIVE);
                      })
                  ).subscribe()
              ).subscribe();


            })
          );

        });

      } else if(args[0].equalsIgnoreCase("account_creation_time")) {

        if(args.length != 4) {
          discordService.createMessageInChannel(guildId, channelId,
              "<@" + userId.asString() + "> Invalid amount of arguments!").subscribe();
          return;
        }

        Date act = null;
        int radiusMinutes = 0;
        Date from = null;

        try {
          long longAct = Long.parseLong(args[1]);
          act = new Date(Snowflake.of(longAct).getTimestamp().toEpochMilli());
        } catch (NumberFormatException e) {
          try {
            Instant instantFrom = Instant.parse(args[1]);
            act = Date.from(instantFrom);
          } catch (DateTimeParseException e2) {
            discordService.createMessageInChannel(guildId, channelId,
                "<@" + userId.asString() + "> Please provide a valid user ID or ISO 8601 timestamp as the 2nd argument!").subscribe();
            return;
          }
        }

        try {
          radiusMinutes = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
          discordService.createMessageInChannel(guildId, channelId,
              "<@" + userId.asString() + "> Please provide an integer as the 2nd argument!").subscribe();
          return;
        }

        try {
          from = new Date(System.currentTimeMillis() - ((long) Integer.parseInt(args[3]) * 60*1000));
        } catch (NumberFormatException e) {
          discordService.createMessageInChannel(guildId, channelId,
              "<@" + userId.asString() + "> Please provide an integer as the 3rd argument!").subscribe();
          return;
        }

        Document filter = new Document();

        filter.append("guild_id", guildId.asLong());
        filter.append("joined_at", new Document("$gt", from));
        Date minRad = new Date(act.getTime() - ((long) radiusMinutes * 60*1000));
        Date maxRad = new Date(act.getTime() + ((long) radiusMinutes * 60*1000));
        filter.append("account_creation_time", new Document().append("$gt", minRad).append("$lt", maxRad));

        NukeOptionsAccountCreationTime nukeOptions = new NukeOptionsAccountCreationTime(act, radiusMinutes, from);

        db.getMemberJoinsWithFilter(filter).thenAccept(users -> {

          Nuke nuke = new Nuke();
          nuke.setGuildId(guildId);
          nuke.setExecutionDate(new Date());
          nuke.setNukeMode(NukeMode.ACCOUNT_CREATION_TIME);
          nuke.setNukeOptions(nukeOptions);
          nuke.setExecutedBy(userId);
          nuke.setAffectedUsers(users.stream().map(CachedUserJoin::getUserId).collect(Collectors.toList()));

          db.insertNuke(nuke).thenAccept(nukeId ->
              event.getGuild().subscribe(guild -> {

                Mono<Void> doBans = users.stream()
                    .map(u -> guild.ban(u.getUserId(), ban -> {
                      ban.setDeleteMessageDays(7);
                      ban.setReason("Nuke " + nukeId);
                    }))
                    .reduce(Mono::when)
                    .orElse(Mono.empty());

                doBans.doOnSuccess(v ->
                    event.getMessage().getChannel().flatMap(channel ->
                        channel.createEmbed(embed -> {
                          embed.setTitle("Completed Nuke");
                          embed.setFooter("OwlBot v" + OwlBot.VERSION, null);
                          embed.setTimestamp(Instant.now());
                          embed.addField("ID", nukeId, false);
                          embed.addField("Affected Users", users.size() + "", false);
                          embed.addField("Executed by", "<@" + event.getMember().get().getId().asString() + ">", false);
                          embed.setColor(users.size() > 0 ? OwlBot.COLOR_POSITIVE : OwlBot.COLOR_NEGATIVE);
                        })
                    ).subscribe()
                ).subscribe();
              })
          );

        });

      } else if(args[0].equalsIgnoreCase("profile_picture")) {

      }


    });
  }


}
