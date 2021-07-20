package de.yannismate.owlbot.model.db;

import de.yannismate.owlbot.model.DatabaseObject;
import discord4j.common.util.Snowflake;
import java.util.Date;
import org.bson.Document;

public class CachedMessage implements DatabaseObject {

  private Snowflake messageId;
  private String content;

  private Snowflake senderId;
  private String senderTag;

  public CachedMessage(){};

  public CachedMessage(Snowflake messageId, String content, Snowflake senderId,
      String senderTag) {
    this.messageId = messageId;
    this.content = content;
    this.senderId = senderId;
    this.senderTag = senderTag;
  }

  @Override
  public CachedMessage read(Document document) {
    this.messageId = Snowflake.of(document.getLong("msg_id"));
    this.content = document.getString("content");
    this.senderId = Snowflake.of(document.getLong("sender_id"));
    this.senderTag = document.getString("sender_tag");
    return this;
  }

  public Snowflake getMessageId() {
    return messageId;
  }

  public String getContent() {
    return content;
  }

  public Snowflake getSenderId() {
    return senderId;
  }

  public String getSenderTag() {
    return senderTag;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public void setSenderTag(String senderTag) {
    this.senderTag = senderTag;
  }

  @Override
  public Document toDocument() {
    Document doc = new Document();
    doc.append("msg_id", messageId.asLong())
        .append("content", content)
        .append("sender_id", senderId.asLong())
        .append("sender_tag", senderTag)
        .append("created_at", new Date());
    return doc;
  }

}
