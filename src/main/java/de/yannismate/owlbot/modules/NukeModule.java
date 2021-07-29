package de.yannismate.owlbot.modules;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.yannismate.owlbot.OwlBot;
import de.yannismate.owlbot.model.Module;
import de.yannismate.owlbot.model.ModuleCommand;
import de.yannismate.owlbot.model.db.CachedUserJoin;
import de.yannismate.owlbot.services.DatabaseService;
import de.yannismate.owlbot.services.DiscordService;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.guild.MemberJoinEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.spec.EmbedCreateSpec;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.function.Consumer;
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

      CachedUserJoin userJoin = new CachedUserJoin(memberJoinEvent.getMember().getId(),
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
                "[from] - Time frame beginning (Message ID or ISO 8601 timestamp)\n"
                    + "[to] - Time frame ending (Message ID, or ISO 8601 timestamp)\n"
                    + "[excluded] - (optional) Comma seperated list of ignored user IDs",
                false);
            spec.addField(prefix + "nuke name_regex",
                "[regex] - RE2 Regex to check names for\n"
                    + "[lookback] - Time to look back in minutes",
                false);
            spec.addField(prefix + "nuke account_creation_time",
                "[time] - ACT of example account (User ID or ISO 8601 timestamp)\n"
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

      } else if(args[0].equalsIgnoreCase("name_regex")) {

      } else if(args[0].equalsIgnoreCase("account_creation_time")) {

      } else if(args[0].equalsIgnoreCase("profile_picture")) {

      }


    });
  }


}
