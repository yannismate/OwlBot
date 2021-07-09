package de.yannismate.owlbot.model;

import org.bson.Document;

public interface DatabaseObject {

  DatabaseObject read(Document document);
  Document toDocument();

}
