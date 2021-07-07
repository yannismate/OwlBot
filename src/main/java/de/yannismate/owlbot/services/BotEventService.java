package de.yannismate.owlbot.services;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Singleton;
import de.yannismate.owlbot.model.BotEvent;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

@Singleton
public class BotEventService {

  private final AsyncEventBus eventBus = new AsyncEventBus(Executors.newFixedThreadPool(4));

  public void publishEvent(BotEvent event) {
    this.eventBus.post(event);
  }

  public <T extends BotEvent> void on(Class<T> eventClass, Consumer<T> consumer) {
    this.eventBus.register(new EventSub<T>(consumer));
  }

  private static class EventSub<T> {

    private final Consumer<T> consumer;

    private EventSub(Consumer<T> consumer) {
      this.consumer = consumer;
    }

    @Subscribe
    public void onEvent(T event) {
      consumer.accept(event);
    }

  }

}
