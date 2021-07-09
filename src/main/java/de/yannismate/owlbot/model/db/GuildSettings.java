package de.yannismate.owlbot.model.db;

import com.google.re2j.Pattern;
import de.yannismate.owlbot.model.DatabaseObject;
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
import org.bson.Document;

public class GuildSettings implements DatabaseObject {

  public static GuildSettings newDefault(Snowflake guildId, Snowflake ownerId) {
    Settings s = new Settings();
    s.setPrefix("!");
    Permissions p = new Permissions();
    p.getUserPermissions().put(ownerId, new HashSet<>(Set.of("*")));
    return new GuildSettings(guildId, new HashSet<>(), s, p);
  }

  private Snowflake guildId;
  private Set<String> enabledModules;
  private Settings settings;
  private Permissions permissions;

  public GuildSettings() {}

  private GuildSettings(Snowflake guildId, Set<String> enabledModules,
      Settings settings, Permissions permissions) {
    this.guildId = guildId;
    this.enabledModules = enabledModules;
    this.settings = settings;
    this.permissions = permissions;
  }

  @Override
  public GuildSettings read(Document doc) {
    this.guildId = Snowflake.of(doc.getLong("guild_id"));
    this.enabledModules = new HashSet<>(doc.getList("enabled_modules", String.class));
    this.settings = new Settings().read((Document) doc.get("settings"));
    this.permissions = new Permissions().read((Document) doc.get("permissions"));
    return this;
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

  public static class Settings implements DatabaseObject {


    @Override
    public Settings read(Document document) {
      this.prefix = document.getString("prefix");
      return this;
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

  public static class Permissions implements DatabaseObject {


    private Map<Snowflake, Set<String>> rolePermissions = new HashMap<>();
    private Map<Snowflake, Set<String>> userPermissions = new HashMap<>();

    public Map<Snowflake, Set<String>> getRolePermissions() {
      return rolePermissions;
    }

    public Map<Snowflake, Set<String>> getUserPermissions() {
      return userPermissions;
    }

    @Override
    public Permissions read(Document document) {
      this.rolePermissions = new HashMap<>();
      this.userPermissions = new HashMap<>();

      if(document.containsKey("role_permissions")) {
        for(Entry<String, List<String>> e : ((Map<String, List<String>>)document.get("role_permissions")).entrySet()) {
          this.rolePermissions.put(Snowflake.of(e.getKey()), new HashSet<>(e.getValue()));
        }
      }

      if(document.containsKey("user_permissions")) {
        for(Entry<String, List<String>> e : ((Map<String, List<String>>)document.get("user_permissions")).entrySet()) {
          this.userPermissions.put(Snowflake.of(e.getKey()), new HashSet<>(e.getValue()));
        }
      }

      return this;
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
