package de.yannismate.owlbot.migrations;

import com.github.cloudyrock.mongock.ChangeLog;
import com.github.cloudyrock.mongock.ChangeSet;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;

@ChangeLog(order = "001")
public class InitialSetup {

  private static final IndexOptions UNIQUE_INDEX = new IndexOptions();
  static {
    UNIQUE_INDEX.unique(true);
  }

  @ChangeSet(order = "001", id = "init-guild-settings-collections", author = "Yannis Matezki")
  public void initGuildSettingsCollection(MongoDatabase db) {
    db.createCollection("guild_settings");
    db.getCollection("guild_settings")
        .createIndex(Indexes.ascending("guild_id"), UNIQUE_INDEX);
  }

  @ChangeSet(order = "002", id = "init-module-settings-collections", author = "Yannis Matezki")
  public void initModuleSettingsCollection(MongoDatabase db) {
    db.createCollection("module_settings");
    db.getCollection("module_settings")
        .createIndex(Indexes.compoundIndex(Indexes.ascending("guild_id"), Indexes.ascending("module_name")), UNIQUE_INDEX);
  }

}
