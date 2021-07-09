package de.yannismate.owlbot.model;

import com.google.common.base.Objects;
import discord4j.common.util.Snowflake;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import org.bson.Document;

public class ModuleSettings {

  public static ModuleSettings fromDocument(Document document) {
    Snowflake guildId = Snowflake.of(document.getLong("guild_id"));
    String moduleName = document.getString("module_name");
    Map<String, SettingsObject> settings = ((Map<String, Object>) document.get("settings")).entrySet().stream()
        .map(entry -> new SimpleEntry<>(entry.getKey(), new SettingsObject(entry.getValue())))
        .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
    return new ModuleSettings(guildId, moduleName, settings);
  }

  private final Snowflake guildId;
  private final String moduleName;

  private final Map<String, SettingsObject> settings;

  public ModuleSettings(Snowflake guildId, String moduleName,
      Map<String, SettingsObject> settings) {
    this.guildId = guildId;
    this.moduleName = moduleName;
    this.settings = settings;
  }

  public Snowflake getGuildId() {
    return guildId;
  }

  public String getModuleName() {
    return moduleName;
  }

  public Map<String, SettingsObject> getSettings() {
    return settings;
  }

  public Document toDocument() {
    Document doc = new Document();
    doc.append("guild_id", this.guildId.asLong());
    doc.append("module_name", this.moduleName);
    Document settings = new Document();
    this.settings.entrySet().forEach((entry) -> settings.append(entry.getKey(), entry.getValue().get()));
    doc.append("settings", settings);
    return doc;
  }

  public static class SettingsObject {

    public static SettingsObject of(String value) {
      return new SettingsObject(value);
    }

    public static SettingsObject of(boolean value) {
      return new SettingsObject(value);
    }

    public static SettingsObject of(int value) {
      return new SettingsObject(value);
    }

    public static SettingsObject of(long value) {
      return new SettingsObject(value);
    }

    public static SettingsObject of(Map<String, SettingsObject> value) {
      return new SettingsObject(value);
    }

    private final Object internalObject;

    private SettingsObject(Object obj) {
      if(obj instanceof SettingsObject) this.internalObject = ((SettingsObject) obj).get();
      else this.internalObject = obj;
    }

    public Object get() {
      if(this.internalObject instanceof Map) {
        return ((Map<String, Object>) this.internalObject).entrySet().stream()
            .map(entry -> new SimpleEntry(entry.getKey(), new SettingsObject(entry.getValue()).get()))
            .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
      }
      return internalObject;
    }

    public Optional<String> getString() {
      if(internalObject instanceof String) return Optional.of((String)internalObject);
      return Optional.empty();
    }

    public Optional<Boolean> getBoolean() {
      if(internalObject instanceof Boolean) return Optional.of((Boolean)internalObject);
      return Optional.empty();
    }

    public Optional<Integer> getInt() {
      if(internalObject instanceof Integer) return Optional.of((Integer)internalObject);
      return Optional.empty();
    }

    public Optional<Long> getLong() {
      if(internalObject instanceof Long) return Optional.of((Long)internalObject);
      return Optional.empty();
    }

    public Optional<Map<String, SettingsObject>> getNested() {
      if(internalObject instanceof Map) {
        return Optional.of((Map<String, SettingsObject>)internalObject);
      }
      return Optional.empty();
    }

    public boolean equals(boolean val) {
      if(internalObject instanceof Boolean) {
        return (Boolean) internalObject == val;
      }
      return false;
    }

    public boolean equals(long val) {
      if(internalObject instanceof Long) {
        return internalObject.equals(val);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(internalObject);
    }
  }

}
