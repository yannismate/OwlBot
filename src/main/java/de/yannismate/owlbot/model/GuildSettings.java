package de.yannismate.owlbot.model;

import com.google.re2j.Pattern;
import discord4j.common.util.Snowflake;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import org.bson.BsonArray;
import org.bson.Document;

public class GuildSettings {

  public static GuildSettings newDefault(Snowflake guildId, Snowflake ownerId) {
    Settings s = new Settings();
    s.setPrefix("!");
    Permissions p = new Permissions();
    p.getUserPermissions().put(ownerId, new HashSet<>(Set.of("*")));
    return new GuildSettings(guildId, new HashSet<>(), s, p);
  }

  public static GuildSettings fromDocument(Document doc) {
    Snowflake id = Snowflake.of(doc.getLong("guild_id"));
    Set<String> em = new HashSet<>(doc.getList("enabled_modules", String.class));
    Settings settings = Settings.fromDocument((Document) doc.get("settings"));
    Permissions permissions = Permissions.fromDocument((Document) doc.get("permissions"));
    return new GuildSettings(id, em, settings, permissions);
  }

  private final Snowflake guildId;
  private final Set<String> enabledModules;
  private final Settings settings;
  private final Permissions permissions;

  private GuildSettings(Snowflake guildId, Set<String> enabledModules,
      Settings settings, Permissions permissions) {
    this.guildId = guildId;
    this.enabledModules = enabledModules;
    this.settings = settings;
    this.permissions = permissions;
  }

  public Snowflake getGuildId() {
    return guildId;
  }

  public Set<String> getEnabledModules() {
    return enabledModules;
  }

  public Settings getSettings() {
    return settings;
  }

  public Permissions getPermissions() {
    return permissions;
  }

  public Document toDocument() {
    Document doc = new Document();
    doc.append("guild_id", this.guildId.asLong());
    doc.append("settings", this.settings.toDocument());
    doc.append("permissions", this.permissions.toDocument());
    doc.append("enabled_modules", Arrays.asList(this.enabledModules.toArray()));
    return doc;
  }

  public static class Settings {

    public static Settings fromDocument(Document document) {
      Settings s = new Settings();
      s.setPrefix(document.getString("prefix"));
      return s;
    }

    private String prefix;

    public String getPrefix() {
      return prefix;
    }

    public void setPrefix(String prefix) {
      this.prefix = prefix;
    }

    public Document toDocument() {
      Document doc = new Document();
      doc.append("prefix", this.prefix);
      return doc;
    }
  }

  public static class Permissions {

    public static Permissions fromDocument(Document document) {
      Map<Snowflake, Set<String>> rolePerms = new HashMap<>();
      Map<Snowflake, Set<String>> userPerms = new HashMap<>();

      if(document.containsKey("role_permissions")) {
        for(Entry<String, List<String>> e : ((Map<String, List<String>>)document.get("role_permissions")).entrySet()) {
          rolePerms.put(Snowflake.of(e.getKey()), new HashSet<>(e.getValue()));
        }
      }

      if(document.containsKey("user_permissions")) {
        for(Entry<String, List<String>> e : ((Map<String, List<String>>)document.get("user_permissions")).entrySet()) {
          userPerms.put(Snowflake.of(e.getKey()), new HashSet<>(e.getValue()));
        }
      }

      Permissions perm = new Permissions();
      perm.rolePermissions = rolePerms;
      perm.userPermissions = userPerms;
      return perm;
    }

    private Map<Snowflake, Set<String>> rolePermissions = new HashMap<>();
    private Map<Snowflake, Set<String>> userPermissions = new HashMap<>();

    public Map<Snowflake, Set<String>> getRolePermissions() {
      return rolePermissions;
    }

    public Map<Snowflake, Set<String>> getUserPermissions() {
      return userPermissions;
    }

    public boolean hasPermission(Snowflake user, Set<Snowflake> roles, String permission) {
      if(userPermissions.containsKey(user)) {
        for(String p : userPermissions.get(user)) {
          if(globMatch(p, permission)) return true;
        }
      }
      for(String p : roles.stream().map(r -> rolePermissions.getOrDefault(r, new HashSet<>())).flatMap(Collection::stream).collect(Collectors.toSet())) {
        if(globMatch(p, permission)) return true;
      }
      return false;
    }

    private boolean globMatch(String glob, String s) {
      String pat = glob.replace("*", "(.)*");
      return Pattern.compile(pat).matches(s);
    }

    public Document toDocument() {
      Document doc = new Document();
      Document rolePerms = new Document();
      Document userPerms = new Document();
      this.rolePermissions.forEach((s, p) -> rolePerms.append(s.asString(), Arrays.asList(p.toArray())));
      this.userPermissions.forEach((s, p) -> userPerms.append(s.asString(), Arrays.asList(p.toArray())));
      doc.append("role_permissions", rolePerms);
      doc.append("user_permissions", userPerms);
      return doc;
    }
  }


}
