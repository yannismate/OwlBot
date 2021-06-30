package de.yannismate.owlbot.services;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import de.yannismate.owlbot.model.GuildSettings;
import discord4j.common.util.Snowflake;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.bson.Document;

@Singleton
public class DatabaseService {

  private MongoDatabase db;
  private final Executor executor = Executors.newFixedThreadPool(4);

  @Inject
  public DatabaseService(BotSettingsService settingsService) {
    MongoClient client = MongoClients.create();
    this.db = client.getDatabase("owlbot");
  }

  public Future<Void> addGuild(Snowflake guildId) {
    return CompletableFuture.runAsync(() -> {
      db.getCollection("guild_settings").insertOne(GuildSettings.newDefault(guildId).toDocument());
    }, executor);
  }

  private final AsyncLoadingCache<Snowflake, GuildSettings> guildSettingsCache = Caffeine.newBuilder()
      .expireAfterAccess(10, TimeUnit.MINUTES)
      .expireAfterWrite(1, TimeUnit.HOURS)
      .executor(this.executor)
      .buildAsync(guildId -> {
        Document filter = new Document("guild_id", guildId.asLong());
        Document result = db.getCollection("guild_settings").find(filter).first();
        if(result == null) return null;
        return GuildSettings.fromDocument(result);
      });
  public Future<Optional<GuildSettings>> getGuildSettings(Snowflake guildId) {
    return guildSettingsCache.get(guildId).thenApply(Optional::ofNullable);
  }


}
