package de.yannismate.owlbot.model.db;

import de.yannismate.owlbot.model.DatabaseObject;
import discord4j.common.util.Snowflake;
import java.util.Date;
import org.bson.Document;

public class CachedUserJoin implements DatabaseObject {

  private Snowflake guildId;
  private Snowflake userId;
  private Date joinedAt;
  private String name;
  private long accountCreationTime;
  private String profilePictureHash;

  public CachedUserJoin(){}

  public CachedUserJoin(Snowflake guildId, Snowflake userId, Date joinedAt, String name,
      long accountCreationTime, String profilePictureHash) {
    this.guildId = guildId;
    this.userId = userId;
    this.joinedAt = joinedAt;
    this.name = name;
    this.accountCreationTime = accountCreationTime;
    this.profilePictureHash = profilePictureHash;
  }

  @Override
  public CachedUserJoin read(Document document) {
    this.guildId = Snowflake.of(document.getLong("guild_id"));
    this.userId = Snowflake.of(document.getLong("user_id"));
    this.joinedAt = document.getDate("joined_at");
    this.name = document.getString("name");
    this.accountCreationTime = document.getLong("account_creation_time");
    this.profilePictureHash = document.getString("profile_picture_hash");
    return this;
  }

  public Snowflake getGuildId() {
    return guildId;
  }

  public Snowflake getUserId() {
    return userId;
  }

  public Date getJoinedAt() {
    return joinedAt;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public long getAccountCreationTime() {
    return accountCreationTime;
  }

  public void setAccountCreationTime(long accountCreationTime) {
    this.accountCreationTime = accountCreationTime;
  }

  public String getProfilePictureHash() {
    return profilePictureHash;
  }

  public void setProfilePictureHash(String profilePictureHash) {
    this.profilePictureHash = profilePictureHash;
  }

  @Override
  public Document toDocument() {
    Document doc = new Document();
    doc.append("guild_id", guildId.asLong())
        .append("user_id", userId.asLong())
        .append("joined_at", joinedAt)
        .append("name", name)
        .append("account_creation_time", accountCreationTime)
        .append("profile_picture_hash", profilePictureHash);
    return doc;
  }

}
