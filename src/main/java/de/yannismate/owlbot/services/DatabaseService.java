package de.yannismate.owlbot.services;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.cloudyrock.mongock.driver.mongodb.v3.driver.MongoCore3Driver;
import com.github.cloudyrock.standalone.MongockStandalone;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;
import de.yannismate.owlbot.model.db.CachedMessage;
import de.yannismate.owlbot.model.db.CachedUserJoin;
import de.yannismate.owlbot.model.db.GuildSettings;
import de.yannismate.owlbot.model.db.ModuleSettings;
import de.yannismate.owlbot.model.db.Nuke;
import discord4j.common.util.Snowflake;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class DatabaseService {

  private MongoDatabase db;
  private final Logger logger = LoggerFactory.getLogger(DatabaseService.class);
  private final Executor executor = Executors.newFixedThreadPool(4);

  @Inject
  public DatabaseService(BotSettingsService settingsService) {
    MongoClient client = MongoClients.create();
    this.db = client.getDatabase("owlbot");

    logger.info("Running Mongock migrations.");
    MongockStandalone.builder()
        .setDriver(MongoCore3Driver.withDefaultLock(client, "owlbot"))
        .addChangeLogsScanPackage("de.yannismate.owlbot.migrations")
        .buildRunner()
        .execute();
    logger.info("Migrations completed.");
  }

  public CompletableFuture<Void> addGuild(Snowflake guildId, Snowflake ownerId) {
    return CompletableFuture.runAsync(() ->
      db.getCollection("guild_settings").insertOne(GuildSettings.newDefault(guildId, ownerId).toDocument())
    , executor);
  }

  private final AsyncLoadingCache<Snowflake, GuildSettings> guildSettingsCache = Caffeine.newBuilder()
      .expireAfterAccess(10, TimeUnit.MINUTES)
      .expireAfterWrite(1, TimeUnit.HOURS)
      .executor(this.executor)
      .buildAsync(guildId -> {
        Document filter = new Document("guild_id", guildId.asLong());
        Document result = db.getCollection("guild_settings").find(filter).first();
        if(result == null) return null;
        return new GuildSettings().read(result);
      });
  public CompletableFuture<Optional<GuildSettings>> getGuildSettings(Snowflake guildId) {
    return guildSettingsCache.get(guildId).thenApply(Optional::ofNullable);
  }
  public CompletableFuture<Void> updateGuildSettings(GuildSettings guildSettings) {
    return CompletableFuture.runAsync(() -> {
      guildSettingsCache.put(guildSettings.getGuildId(), CompletableFuture.completedFuture(guildSettings));
      Document filter = new Document("guild_id", guildSettings.getGuildId().asLong());
      db.getCollection("guild_settings").updateOne(filter, new Document("$set", guildSettings.toDocument()), new UpdateOptions().upsert(true));
    }, executor);
  }


  public CompletableFuture<Void> addModuleSettings(ModuleSettings settings) {
    return CompletableFuture.runAsync(() ->
        db.getCollection("module_settings").insertOne(settings.toDocument())
        , executor);
  }
  private final AsyncLoadingCache<String, ModuleSettings> moduleSettingsCache = Caffeine.newBuilder()
      .expireAfterAccess(10, TimeUnit.MINUTES)
      .expireAfterWrite(1, TimeUnit.HOURS)
      .executor(this.executor)
      .buildAsync(mixedKey -> {
        String[] splitKey = mixedKey.split(":");
        Document filter = new Document();
        filter.append("guild_id", Long.valueOf(splitKey[0]));
        filter.append("module_name", splitKey[1]);
        Document result = db.getCollection("module_settings").find(filter).first();
        if(result == null) return null;
        return new ModuleSettings(Snowflake.of(splitKey[0]), splitKey[1]).read(result);
      });
  public CompletableFuture<Optional<ModuleSettings>> getModuleSettings(Snowflake guildId, String moduleName) {
    return moduleSettingsCache.get(guildId.asLong() + ":" + moduleName).thenApply(Optional::ofNullable);
  }
  public CompletableFuture<Void> updateModuleSettings(ModuleSettings moduleSettings) {
    return CompletableFuture.runAsync(() -> {
      moduleSettingsCache
          .put(moduleSettings.getGuildId().asLong() + ":" + moduleSettings.getModuleName(),
              CompletableFuture.completedFuture(moduleSettings));
      Document filter = new Document();
      filter.append("guild_id", moduleSettings.getGuildId().asLong());
      filter.append("module_name", moduleSettings.getModuleName());
      db.getCollection("module_settings")
          .replaceOne(filter, moduleSettings.toDocument());
    }, executor);
  }

  public CompletableFuture<Void> insertCachedMessage(CachedMessage cachedMessage) {
    return CompletableFuture.runAsync(() -> {
      db.getCollection("msg_temp").insertOne(cachedMessage.toDocument());
    }, executor);
  }
  public CompletableFuture<Void> updateCachedMessage(CachedMessage cachedMessage) {
    return CompletableFuture.runAsync(() -> {
      Document filter = new Document("msg_id", cachedMessage.getMessageId().asLong());
      db.getCollection("msg_temp").replaceOne(filter, cachedMessage.toDocument());
    }, executor);
  }
  public CompletableFuture<Optional<CachedMessage>> getCachedMessage(Snowflake messageId) {
    return CompletableFuture.supplyAsync(() -> {
      Document filter = new Document("msg_id", messageId.asLong());
      Document msgDoc = db.getCollection("msg_temp").find(filter).first();
      if(msgDoc == null) return Optional.empty();
      return Optional.of(new CachedMessage().read(msgDoc));
    }, executor);
  }

  public CompletableFuture<Void> insertOrUpdateMemberJoin(CachedUserJoin cachedUserJoin) {
    return CompletableFuture.runAsync(() -> {
      db.getCollection("member_join_temp").updateOne(
          new Document("user_id", cachedUserJoin.getUserId().asLong()),
          new Document("$set", cachedUserJoin.toDocument()),
          new UpdateOptions().upsert(true)
      );
    }, executor);
  }

  public CompletableFuture<List<CachedUserJoin>> getMemberJoinsWithFilter(Document filter) {
    return CompletableFuture.supplyAsync(() -> {
      MongoCursor<Document> docs = db.getCollection("member_join_temp").find(filter).iterator();
      List<CachedUserJoin> list = new ArrayList<>();
      while(docs.hasNext()) {
        list.add(new CachedUserJoin().read(docs.next()));
      }
      return list;
    }, executor);
  }

  public CompletableFuture<String> insertNuke(Nuke nuke) {
    return CompletableFuture.supplyAsync(() -> {
      Document doc = nuke.toDocument();
      db.getCollection("nukes").insertOne(doc);
      return doc.getObjectId("_id").toHexString();
    }, executor);
  }

}
