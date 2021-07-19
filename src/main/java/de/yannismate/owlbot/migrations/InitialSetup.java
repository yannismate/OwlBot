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

}
