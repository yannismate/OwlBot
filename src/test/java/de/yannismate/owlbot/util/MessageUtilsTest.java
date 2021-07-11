package de.yannismate.owlbot.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.junit.jupiter.api.Test;

public class MessageUtilsTest {

  @Test
  void testTokenizer() {
    String preTokenize = "${token1} static text 1 ${invalid-token} static text 2 ${token1} ${token2}";
    String postTokenize = MessageUtils.replaceTokens(preTokenize, Map.of(
       "token1", "replacement1",
       "token2", "2 replacement"
    ));

    assertEquals("replacement1 static text 1 ${invalid-token} static text 2 replacement1 2 replacement", postTokenize);
  }

}
