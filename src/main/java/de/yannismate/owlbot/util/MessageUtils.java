package de.yannismate.owlbot.util;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageUtils {

  public static String escapeForSingleLineCodeBlock(String content) {
    return content.replace("`", "'");
  }

  public static String escapeForMultiLineCodeBlock(String content) {
    return content.replace("`", "\u200E`");
  }

  public static String replaceTokens(String input, Map<String, String> tokens) {
    Pattern patter = Pattern.compile("\\$\\{(.+?)}");
    Matcher matcher = patter.matcher(input);
    StringBuilder buffer = new StringBuilder();

    while(matcher.find()) {
      if(!tokens.containsKey(matcher.group(1))) continue;

      String replacement = tokens.get(matcher.group(1));
      matcher.appendReplacement(buffer, "");
      buffer.append(replacement);
    }
    matcher.appendTail(buffer);

    return buffer.toString();
  }


}
