package de.yannismate.owlbot.migrations;

import com.github.cloudyrock.mongock.ChangeLog;
import com.github.cloudyrock.mongock.ChangeSet;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import java.util.concurrent.TimeUnit;

@ChangeLog(order = "001")
public class InitialSetup {

  private static final IndexOptions UNIQUE_INDEX = new IndexOptions();
  static {
    UNIQUE_INDEX.unique(true);
  }

  @ChangeSet(order = "001", id = "init-guild-settings-collection", author = "Yannis Matezki")
  public void initGuildSettingsCollection(MongoDatabase db) {
    db.createCollection("guild_settings");
    db.getCollection("guild_settings")
        .createIndex(Indexes.ascending("guild_id"), UNIQUE_INDEX);
  }

  @ChangeSet(order = "002", id = "init-module-settings-collection", author = "Yannis Matezki")
  public void initModuleSettingsCollection(MongoDatabase db) {
    db.createCollection("module_settings");
    db.getCollection("module_settings")
        .createIndex(Indexes.compoundIndex(Indexes.ascending("guild_id"), Indexes.ascending("module_name")), UNIQUE_INDEX);
  }

  @ChangeSet(order = "003", id = "init-temp-messages-collection", author = "Yannis Matezki")
  public void initTempMessagesCollection(MongoDatabase db) {
    db.createCollection("msg_temp");
    db.getCollection("msg_temp").createIndex(Indexes.ascending("msg_id"), UNIQUE_INDEX);
    IndexOptions options = new IndexOptions();
    options.unique(true);
    options.expireAfter(7L, TimeUnit.DAYS);
    db.getCollection("msg_temp").createIndex(Indexes.ascending("created_at"), options);
  }

  @ChangeSet(order = "004", id = "init-temp-member-joins-collection", author = "Yannis Matezki")
  public void initTempMemberJoinsCollection(MongoDatabase db) {
    db.createCollection("member_join_temp");
    db.getCollection("member_join_temp").createIndex(Indexes.compoundIndex(Indexes.ascending("guild_id"), Indexes.ascending("user_id")), UNIQUE_INDEX);
    IndexOptions options = new IndexOptions();
    options.unique(true);
    options.expireAfter(1L, TimeUnit.DAYS);
    db.getCollection("member_join_temp").createIndex(Indexes.ascending("joined_at"), options);
  }

  @ChangeSet(order = "005", id = "init-nuke-collection", author = "Yannis Matezki")
  public void initNukeCollection(MongoDatabase db) {
    db.createCollection("nukes");
  }

}
