package de.yannismate.owlbot.model.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.yannismate.owlbot.model.db.ModuleSettings;
import de.yannismate.owlbot.model.db.ModuleSettings.ModuleSettingsValue;
import de.yannismate.owlbot.model.db.ModuleSettings.ModuleSettingsValue.ModuleSettingsValueType;
import discord4j.common.util.Snowflake;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.bson.Document;
import org.junit.jupiter.api.Test;

public class ModuleSettingsTest {

  @Test
  void testBsonConversion() {

    Snowflake randomGuildId = Snowflake.of(ThreadLocalRandom.current().nextLong());

    ModuleSettings ms = new ModuleSettings(randomGuildId, "TestModule");
    ms.getOptions().put("test_key_1", new ModuleSettingsValue(true));
    ms.getOptions().put("test_key_2", new ModuleSettingsValue(123));
    ms.getOptions().put("test_key_3", new ModuleSettingsValue(431L));
    ms.getOptions().put("test_key_4", new ModuleSettingsValue("test"));
    ms.getOptions().put("test_key_5", new ModuleSettingsValue(Map.of(
        "test_nested_1", new ModuleSettingsValue(false),
        "test_nested_2", new ModuleSettingsValue("true")
    )));

    ModuleSettings ms2 = new ModuleSettings(randomGuildId, "TestModule").read(Document.parse(ms.toDocument().toJson()));

    assertEquals(ModuleSettingsValueType.BOOLEAN, ms2.getOptions().get("test_key_1").getType());
    assertEquals(true, ms2.getOptions().get("test_key_1").getRaw());

    assertEquals(ModuleSettingsValueType.INTEGER, ms2.getOptions().get("test_key_2").getType());
    assertEquals(123, ms2.getOptions().get("test_key_2").getRaw());

    assertEquals(ModuleSettingsValueType.LONG, ms2.getOptions().get("test_key_3").getType());
    assertEquals(431L, ms2.getOptions().get("test_key_3").getRaw());

    assertEquals(ModuleSettingsValueType.STRING, ms2.getOptions().get("test_key_4").getType());
    assertEquals("test", ms2.getOptions().get("test_key_4").getRaw());

    assertEquals(ModuleSettingsValueType.MAP, ms2.getOptions().get("test_key_5").getType());
    assertTrue(ms2.getOptions().get("test_key_5").getNested().isPresent());
    assertEquals(2, ms2.getOptions().get("test_key_5").getNested().get().size());

    Map<String, ModuleSettingsValue> nested = ms2.getOptions().get("test_key_5").getNested().get();
    assertEquals(false, nested.get("test_nested_1").getRaw());
    assertEquals("true", nested.get("test_nested_2").getRaw());

  }

}
