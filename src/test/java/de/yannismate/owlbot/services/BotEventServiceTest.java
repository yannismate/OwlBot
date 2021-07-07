package de.yannismate.owlbot.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.yannismate.owlbot.model.BotEvent;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

public class BotEventServiceTest {

  private class TestEvent implements BotEvent {

    public TestEvent(int testId) {
      this.testId = testId;
    }

    public int testId;
  }

  private class TestEvent2 implements BotEvent {

    public TestEvent2(int testId) {
      this.testId = testId;
    }

    public int testId;
  }

  @Test
  void testEventHandling() throws InterruptedException {
    BotEventService botEventService = new BotEventService();
    CountDownLatch latch = new CountDownLatch(1);
    final int random = ThreadLocalRandom.current().nextInt();

    botEventService.on(TestEvent.class, testEvent -> {
      assertEquals(random, testEvent.testId);
      latch.countDown();
    });

    botEventService.publishEvent(new TestEvent(random));
    botEventService.publishEvent(new TestEvent2(random+1));

    assertTrue(latch.await(100, TimeUnit.MILLISECONDS));

  }


}
