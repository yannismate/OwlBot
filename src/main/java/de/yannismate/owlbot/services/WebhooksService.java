package de.yannismate.owlbot.services;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class WebhooksService {

  private final Logger logger = LoggerFactory.getLogger(WebhooksService.class);

  private final HttpClient httpClient;
  private final Executor executor;
  private final String webhookUrl;
  private final String hmacSecret;
  private final String clientId;

  @Inject
  private BotEventService botEventService;

  @Inject
  private TwitchAuthenticationService twitchAuthenticationService;

  @Inject
  public WebhooksService(BotSettingsService botSettingsService) throws IOException {
    this.executor = Executors.newFixedThreadPool(4);
    this.httpClient = HttpClient.newBuilder().executor(executor).build();
    this.webhookUrl = botSettingsService.getWebhooksUrl();
    this.hmacSecret = botSettingsService.getTwitchWebhookSecret();
    this.clientId = botSettingsService.getTwitchClientId();
    HttpServer server = HttpServer.create(new InetSocketAddress(botSettingsService.getWebhooksPort()), 16);
    server.setExecutor(null);
    server.createContext("/twitch", new TwitchWebhooksHandler());
    server.start();
  }


  public CompletableFuture<Void> subscribeTwitchLiveWebhook(String broadcasterId) {
    return CompletableFuture.runAsync(() -> {
      twitchAuthenticationService.getAppToken().thenAccept(token -> {

        JSONObject rq = new JSONObject();
        rq.put("type", "stream.online");
        rq.put("version", "1");
        rq.put("condition", new JSONObject(Map.of("broadcaster_user_id", broadcasterId)));
        JSONObject transport = new JSONObject();
        transport.put("method", "webhook");
        transport.put("callback", webhookUrl + "/twitch");
        transport.put("secret", hmacSecret);
        rq.put("transport", transport);

        HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.twitch.tv/helix/eventsub/subscriptions"))
            .header("Client-ID", clientId)
            .header("Authorization", "Bearer " + token)
            .header("Content-Type", "application/json")
            .POST(BodyPublishers.ofString(rq.toJSONString()))
            .build();

        httpClient.sendAsync(request, BodyHandlers.ofString()).thenAccept(httpResponse -> {
          if(httpResponse.statusCode() >= 300) {
            logger.atError().addArgument(httpResponse.statusCode()).addArgument(httpResponse.body()).log("Error creating webhook subscription. Code {}, Reponse {}");
          }
        });
      });
    }, executor);
  }


  class TwitchWebhooksHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {

      //TODO
      String body = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))
          .lines()
          .collect(Collectors.joining("\n"));

      logger.atInfo().addArgument(exchange.getRequestURI()).log("Webhook URI: {}");
      logger.atInfo().addArgument(body).log("Body: {}");

      exchange.sendResponseHeaders(200, 0);

    }

  }


}
