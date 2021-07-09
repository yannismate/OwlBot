package de.yannismate.owlbot.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.yannismate.owlbot.model.db.ModuleSettings;
import de.yannismate.owlbot.model.db.ModuleSettings.SettingsObject;
import discord4j.common.util.Snowflake;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.bson.Document;
import org.junit.jupiter.api.Test;

public class ModuleSettingsTest {

  @Test
  void testBsonConversion() {
    Document testDoc = new Document();
    testDoc.append("guild_id", Snowflake.of(ThreadLocalRandom.current().nextLong()).asLong());
    testDoc.append("module_name", "TestModule");

    Document settings = new Document();
    settings.append("key1", SettingsObject.of("value1").get());
    settings.append("key2", SettingsObject.of(ThreadLocalRandom.current().nextInt()).get());
    settings.append("key3", SettingsObject.of(ThreadLocalRandom.current().nextLong()).get());
    settings.append("key4", SettingsObject.of(Map.of("key5", SettingsObject.of("key6"))).get());
    testDoc.append("settings", settings);

    //Simulate database insert and get
    testDoc = Document.parse(testDoc.toJson());

    ModuleSettings ms = ModuleSettings.fromDocument(testDoc);

    assertEquals(testDoc, Document.parse(ms.toDocument().toJson()));

    ModuleSettings ms2 = new ModuleSettings(
        Snowflake.of(ThreadLocalRandom.current().nextLong()),
        "TestModule2",
        Map.of("test_key", ModuleSettings.SettingsObject.of(1234),
            "test_key2", ModuleSettings.SettingsObject.of(Map.of("test_key3", ModuleSettings.SettingsObject.of("1234a")))));

    ms2.toDocument();

  }

}
