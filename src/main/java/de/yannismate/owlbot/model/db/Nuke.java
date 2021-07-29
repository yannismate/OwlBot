package de.yannismate.owlbot.model.db;

import de.yannismate.owlbot.model.DatabaseObject;
import discord4j.common.util.Snowflake;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import org.bson.Document;

public class Nuke implements DatabaseObject {

  public enum NukeMode {
    JOIN_TIME,
    NAME_REGEX,
    ACCOUNT_CREATION_TIME,
    PROFILE_PICTURE;

    @Override
    public String toString() {
      return this.name().toLowerCase();
    }

    static NukeMode fromString(String s) {
      return Arrays.stream(NukeMode.values()).filter(nm -> nm.name().equalsIgnoreCase(s)).findFirst().orElseThrow();
    }

  }

  private Snowflake guildId;
  private Snowflake executedBy;
  private NukeMode nukeMode;
  private NukeOptions nukeOptions;
  private Date executionDate;
  private Date radiationEndDate;
  private List<Snowflake> affectedUsers;


  @Override
  public Nuke read(Document document) {
    this.guildId = Snowflake.of(document.getLong("guild_id"));
    this.executedBy = Snowflake.of(document.getLong("executed_by"));
    this.nukeMode = NukeMode.fromString(document.getString("nuke_mode"));
    switch (nukeMode) {
      case JOIN_TIME:
        this.nukeOptions = new NukeOptionsJoinTime().read((Document) document.get("nuke_options"));
        break;
      case NAME_REGEX:
        this.nukeOptions = new NukeOptionsNameRegex().read((Document) document.get("nuke_options"));
        break;
      case ACCOUNT_CREATION_TIME:
        this.nukeOptions = new NukeOptionsAccountCreationTime().read((Document) document.get("nuke_options"));
        break;
      case PROFILE_PICTURE:
        this.nukeOptions = new NukeOptionsProfilePicture().read((Document) document.get("nuke_options"));
        break;
    }
    this.executionDate = document.getDate("execution_date");
    this.radiationEndDate = document.getDate("radiation_end_date");
    this.affectedUsers = document.getList("affected_users", Long.class).stream().map(Snowflake::of).collect(Collectors.toList());
    return this;
  }

  @Override
  public Document toDocument() {
    return new Document()
        .append("guild_id", guildId.asLong())
        .append("executed_by", executedBy.asLong())
        .append("nuke_mode", nukeMode.toString())
        .append("nuke_options", nukeOptions.toDocument())
        .append("execution_date", executionDate)
        .append("radiation_end_date", radiationEndDate)
        .append("affected_users", affectedUsers.stream().map(Snowflake::asLong).collect(Collectors.toList()));
  }

  public interface NukeOptions extends DatabaseObject{}

  public static class NukeOptionsJoinTime implements NukeOptions{
    private Date from;
    private Date to;
    private List<Snowflake> ignoredUsers;

    public NukeOptionsJoinTime() {}

    public NukeOptionsJoinTime(Date from, Date to,
        List<Snowflake> ignoredUsers) {
      this.from = from;
      this.to = to;
      this.ignoredUsers = ignoredUsers;
    }

    public Date getFrom() {
      return from;
    }

    public Date getTo() {
      return to;
    }

    public List<Snowflake> getIgnoredUsers() {
      return ignoredUsers;
    }

    @Override
    public NukeOptionsJoinTime read(Document document) {
      this.from = document.getDate("from");
      this.to = document.getDate("to");
      this.ignoredUsers = document.getList("ignored_users", Long.class).stream()
          .map(Snowflake::of)
          .collect(Collectors.toList());
      return this;
    }

    @Override
    public Document toDocument() {
      return new Document()
          .append("from", this.from)
          .append("to", this.to)
          .append("ignored_users", this.ignoredUsers.stream()
              .map(Snowflake::asLong)
              .collect(Collectors.toList()));
    }
  }
  public static class NukeOptionsNameRegex implements NukeOptions {
    private String regex;
    private Date joinedAfter;

    public NukeOptionsNameRegex(){}

    public NukeOptionsNameRegex(String regex, Date joinedAfter) {
      this.regex = regex;
      this.joinedAfter = joinedAfter;
    }

    public String getRegex() {
      return regex;
    }

    public Date getJoinedAfter() {
      return joinedAfter;
    }

    @Override
    public NukeOptionsNameRegex read(Document document) {
      this.regex = document.getString("regex");
      this.joinedAfter = document.getDate("joined_after");
      return this;
    }

    @Override
    public Document toDocument() {
      return new Document()
          .append("regex", this.regex)
          .append("joined_after", this.joinedAfter);
    }
  }
  public static class NukeOptionsAccountCreationTime implements NukeOptions {
    private Date time;
    private int radius;
    private Date joinedAfter;

    public NukeOptionsAccountCreationTime() { }

    public NukeOptionsAccountCreationTime(Date time, int radius, Date joinedAfter) {
      this.time = time;
      this.radius = radius;
      this.joinedAfter = joinedAfter;
    }

    public Date getTime() {
      return time;
    }

    public int getRadius() {
      return radius;
    }

    public Date getJoinedAfter() {
      return joinedAfter;
    }

    @Override
    public NukeOptionsAccountCreationTime read(Document document) {
      this.time = document.getDate("time");
      this.radius = document.getInteger("radius");
      this.joinedAfter = document.getDate("joined_after");
      return this;
    }

    @Override
    public Document toDocument() {
      return new Document()
          .append("time", this.time)
          .append("radius", this.radius)
          .append("joined_after", this.joinedAfter);
    }
  }
  public static class NukeOptionsProfilePicture implements NukeOptions {
    private String hash;
    private Date joinedAfter;

    public NukeOptionsProfilePicture() { }

    public NukeOptionsProfilePicture(String hash, Date joinedAfter) {
      this.hash = hash;
      this.joinedAfter = joinedAfter;
    }

    public String getHash() {
      return hash;
    }

    public Date getJoinedAfter() {
      return joinedAfter;
    }

    @Override
    public NukeOptionsProfilePicture read(Document document) {
      this.hash = document.getString("hash");
      this.joinedAfter = document.getDate("joined_after");
      return this;
    }

    @Override
    public Document toDocument() {
      return new Document()
          .append("hash", this.hash)
          .append("joined_after", this.joinedAfter);
    }
  }

}
