package ru.yandex.eventregistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import static org.junit.Assert.assertEquals;

/**
 * Created by Andrey Kapitonov on 4/15/16.
 */
public class EventRegistryTest {

    private EventRegistry eventRegistry;

    @Before
    public void setUp() throws Exception {
        eventRegistry = new EventRegistryImpl();
    }

    @After
    public void tearDown() throws Exception {
        ((EventRegistryImpl) eventRegistry).close();
    }

    @Test
    public void testSimpleRegistering() throws Exception {
        eventRegistry.registerEvent();

        assertEquals(1L, eventRegistry.getEventsNumberLastMinute());
        assertEquals(1L, eventRegistry.getEventsNumberLastHour());
        assertEquals(1L, eventRegistry.getEventsNumberLastDay());
    }

    @Test
    public void testGetNumberOfEventsBeforeAndAfterTimeRange() throws Exception {
        final Method method = eventRegistry.getClass().getDeclaredMethod("getEventsNumberLast5sec");
        eventRegistry.registerEvent();
        assertEquals(1L, method.invoke(eventRegistry));

        Thread.sleep(6000L);

        assertEquals(0L, method.invoke(eventRegistry));
    }

    @Test
    public void testMassiveConcurrentRegistering() throws Exception {
        final int NUM_OF_THREADS = 8;
        final int EVENTS_NUM_TOTAL = 1000_000;

        //create pooled executor service and submit specified number of registerEvent() calls
        ExecutorService executorService = Executors.newFixedThreadPool(NUM_OF_THREADS);
        executorService.invokeAll(IntStream.range(0, EVENTS_NUM_TOTAL).mapToObj(i -> (Callable<Object>) () -> {
            eventRegistry.registerEvent();
            return null;
        }).collect(Collectors.toList()));
        executorService.shutdown();

        //wait until all tasks will be finished
        try {
            executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        } finally {
            if (!executorService.isShutdown()) executorService.shutdownNow();
        }

        assertEquals((long) EVENTS_NUM_TOTAL, eventRegistry.getEventsNumberLastMinute());
        assertEquals((long) EVENTS_NUM_TOTAL, eventRegistry.getEventsNumberLastHour());
        assertEquals((long) EVENTS_NUM_TOTAL, eventRegistry.getEventsNumberLastDay());
    }
}
