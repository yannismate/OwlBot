package de.yannismate.owlbot.model.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.yannismate.owlbot.model.db.GuildSettings;
import de.yannismate.owlbot.model.db.GuildSettings.Permissions;
import discord4j.common.util.Snowflake;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.bson.Document;
import org.junit.jupiter.api.Test;

public class GuildSettingsTest {


  @Test
  void testBsonConversion() {

    Snowflake ownerId = Snowflake.of(ThreadLocalRandom.current().nextLong());
    Snowflake randomRole = Snowflake.of(ThreadLocalRandom.current().nextLong());

    Document testDoc = new Document();
    testDoc.append("guild_id", Snowflake.of(ThreadLocalRandom.current().nextLong()).asLong());
    Document perm = new Document();
    Document userPerm = new Document();
    Document rolePerm = new Document();

    userPerm.append(ownerId.asString(), List.of("*"));
    rolePerm.append(randomRole.asString(), List.of("test.permission"));
    perm.append("user_permissions", userPerm);
    perm.append("role_permissions", rolePerm);
    testDoc.append("permissions", perm);
    Document settings = new Document("prefix", "!");
    testDoc.append("settings", settings);
    testDoc.append("enabled_modules", List.of("TestModule"));

    testDoc = Document.parse(testDoc.toJson());

    assertEquals(testDoc, new GuildSettings().read(testDoc).toDocument());

  }

  @Test
  void testPermissionMatching() {

    Snowflake ownerId = Snowflake.of(ThreadLocalRandom.current().nextLong());
    Snowflake notOwnerId = Snowflake.of(ownerId.asLong() + 1);
    Snowflake randomRole = Snowflake.of(ThreadLocalRandom.current().nextLong());

    Permissions permissions = new Permissions();
    permissions.getUserPermissions().put(ownerId, Set.of("*"));
    permissions.getRolePermissions().put(randomRole, Set.of("test.*"));

    assertTrue(permissions.hasPermission(ownerId, Collections.emptySet(), "any_permission"));
    assertTrue(permissions.hasPermission(ownerId, Collections.emptySet(), "test.any_permission"));
    assertTrue(permissions.hasPermission(ownerId, Collections.singleton(randomRole), "any_permission"));

    assertTrue(permissions.hasPermission(notOwnerId, Collections.singleton(randomRole), "test.any_permission.foo"));
    assertFalse(permissions.hasPermission(notOwnerId, Collections.singleton(randomRole), "any_permission"));

  }


}
