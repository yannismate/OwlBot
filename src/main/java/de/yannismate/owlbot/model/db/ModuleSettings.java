package de.yannismate.owlbot.model.db;

import de.yannismate.owlbot.model.DatabaseObject;
import discord4j.common.util.Snowflake;
import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import org.bson.Document;

public class ModuleSettings implements DatabaseObject {

  private Snowflake guildId;
  private String moduleName;
  private final Map<String, ModuleSettingsValue> options = new HashMap<>();

  public ModuleSettings(Snowflake guildId, String moduleName) {
    this.guildId = guildId;
    this.moduleName = moduleName;
  }

  @Override
  public ModuleSettings read(Document document) {
    this.options.clear();
    this.guildId = Snowflake.of(document.getLong("guild_id"));
    this.moduleName = document.getString("module_name");
    Document options = document.get("options", Document.class);
    options.forEach((key, value) -> this.options.put(key, ModuleSettingsValue.fromBDObject(value)));
    return this;
  }

  public Snowflake getGuildId() {
    return guildId;
  }

  public String getModuleName() {
    return moduleName;
  }

  public Map<String, ModuleSettingsValue> getOptions() {
    return options;
  }

  @Override
  public Document toDocument() {
    Document doc = new Document();
    doc.append("guild_id", this.guildId.asLong());
    doc.append("module_name", this.moduleName);
    Document options = new Document();
    this.options.forEach((key, value) -> options.append(key, value.toDBObject()));
    doc.append("options", options);
    return doc;
  }

  public static class ModuleSettingsValue {

    public enum ModuleSettingsValueType {
      LONG,
      STRING,
      BOOLEAN,
      INTEGER,
      MAP
    }

    private ModuleSettingsValueType type;
    private Object internalObject;

    @SuppressWarnings("unchecked")
    public static ModuleSettingsValue fromBDObject(Object object) {
      if(object instanceof Long) return new ModuleSettingsValue((long)object);
      if(object instanceof String) return new ModuleSettingsValue((String)object);
      if(object instanceof Boolean) return new ModuleSettingsValue((boolean)object);
      if(object instanceof Integer) return new ModuleSettingsValue((int)object);
      if(object instanceof Document) {
       Map<?, ?> m = (Map<?, ?>) object;
       Map<String, ModuleSettingsValue> mp = m.entrySet().stream()
           .map(entry -> new SimpleEntry<>((String) entry.getKey(), ModuleSettingsValue.fromBDObject(entry.getValue())))
           .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
       return new ModuleSettingsValue(mp);
      }
      throw new IllegalArgumentException("Given Object is not a valid ModuleSettingsValue " + object.getClass().getName());
    }

    public ModuleSettingsValue(long value) {
      this.type = ModuleSettingsValueType.LONG;
      this.internalObject = value;
    }

    public ModuleSettingsValue(String value) {
      this.type = ModuleSettingsValueType.STRING;
      this.internalObject = value;
    }

    public ModuleSettingsValue(boolean value) {
      this.type = ModuleSettingsValueType.BOOLEAN;
      this.internalObject = value;
    }

    public ModuleSettingsValue(int value) {
      this.type = ModuleSettingsValueType.INTEGER;
      this.internalObject = value;
    }

    public ModuleSettingsValue(Map<String, ModuleSettingsValue> value) {
      this.type = ModuleSettingsValueType.MAP;
      this.internalObject = value;
    }

    @SuppressWarnings("unchecked")
    public Object toDBObject() {
      if(this.type != ModuleSettingsValueType.MAP) return this.internalObject;

      Map<String, ModuleSettingsValue> internalMap = (Map<String, ModuleSettingsValue>) this.internalObject;
      Document doc = new Document();
      internalMap.forEach((key, value) -> doc.append(key, value.toDBObject()));
      return doc;
    }

    public void update(Object newValue) {
      if(!isSameTypeAs(newValue)) throw new IllegalArgumentException("New Value for ModuleSettingsValue is of wrong type.");
      this.internalObject = newValue;
    }

    public ModuleSettingsValueType getType() {
      return type;
    }

    public Object getRaw() {
      return this.internalObject;
    }

    @SuppressWarnings("unchecked")
    public Optional<Map<String, ModuleSettingsValue>> getNested() {
      if(this.type != ModuleSettingsValueType.MAP) return Optional.empty();
      Map<String, ModuleSettingsValue> internalMap = (Map<String, ModuleSettingsValue>) this.internalObject;
      return Optional.of(internalMap);
    }

    public boolean isSameTypeAs(Object value) {
      switch (this.type) {
        case LONG: return Long.class.isAssignableFrom(value.getClass());
        case STRING: return String.class.isAssignableFrom(value.getClass());
        case BOOLEAN: return Boolean.class.isAssignableFrom(value.getClass());
        case INTEGER: return Integer.class.isAssignableFrom(value.getClass());
        case MAP:
          if(!Map.class.isAssignableFrom(value.getClass())) return false;
          Map<?, ?> m = (Map<?, ?>) value;
          return m.entrySet().stream()
              .noneMatch(entry -> !(entry.getKey() instanceof String) || !(entry.getValue() instanceof ModuleSettingsValue));
      }
      return false;
    }

  }

}
