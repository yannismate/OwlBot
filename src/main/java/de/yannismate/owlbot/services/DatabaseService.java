package de.yannismate.owlbot.services;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import discord4j.common.util.Snowflake;
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
      Document doc = new Document();
      doc.append("guild_id", guildId.asLong());

      Document settings = new Document();
      settings.append("prefix", "!");
      doc.append("settings", settings);

      db.getCollection("guild_settings").insertOne(doc);
    }, executor);
  }

  private final AsyncLoadingCache<Snowflake, Document> guildSettingsCache = Caffeine.newBuilder()
      .expireAfterAccess(10, TimeUnit.MINUTES)
      .expireAfterWrite(1, TimeUnit.HOURS)
      .executor(this.executor)
      .buildAsync(guildId -> {
        Document filter = new Document("guild_id", guildId.asLong());
        return db.getCollection("guild_settings").find(filter).first();
      });
  public Future<Document> getGuildSettings(Snowflake guildId) {
    return guildSettingsCache.get(guildId);
  }


}
