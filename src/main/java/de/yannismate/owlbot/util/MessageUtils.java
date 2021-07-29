package de.yannismate.owlbot.util;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageUtils {

  public static String escapeForSingleLineCodeBlock(String content) {
    return content.replace("`", "'");
  }

  public static String escapeForMultiLineCodeBlock(String content) {
    return content.replace("`", "\u200E`\u200E");
  }

  public static String replaceTokens(String input, Map<String, String> tokens) {

    StringBuilder stringBuilder = new StringBuilder();

    char c1 = ' ';
    char c2 = ' ';

    boolean isSingleCodeBlock = false;
    boolean isMultiCodeBlock = false;

    boolean tokenOpened = false;
    String currentToken = "";

    for(int i = 0; i < input.length(); i++) {
      char currentChar = input.charAt(i);
      if(tokenOpened) {
        if(currentChar == '}') {
          tokenOpened = false;
          if(tokens.containsKey(currentToken)) {
            if(isMultiCodeBlock) stringBuilder.append(escapeForMultiLineCodeBlock(tokens.get(currentToken)));
            else if(isSingleCodeBlock) stringBuilder.append(escapeForSingleLineCodeBlock(tokens.get(currentToken)));
            else stringBuilder.append(tokens.get(currentToken));
          }
          else stringBuilder.append("${").append(currentToken).append("}");
          currentToken = "";
        } else {
          currentToken += currentChar;
        }
      } else {
        if(currentChar == '{' && c1 == '$') {
          tokenOpened = true;
        } else {
          if(c1 == '$') stringBuilder.append('$');
          if(currentChar != '$') stringBuilder.append(currentChar);
          if(currentChar == '`') {

            if(c1 == '`' && c2 == '`') {
              isMultiCodeBlock = !isMultiCodeBlock;
            } else if(!isMultiCodeBlock) {
              isSingleCodeBlock = !isSingleCodeBlock;
            }

          }
        }
      }

      c2 = c1;
      c1 = currentChar;
    }

    return stringBuilder.toString();

  }


}
