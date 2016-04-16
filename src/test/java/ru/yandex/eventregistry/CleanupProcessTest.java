package ru.yandex.eventregistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static org.junit.Assert.assertEquals;

/**
 * Created by Andrey Kapitonov on 4/16/16.
 */
public class CleanupProcessTest {

    private EventRegistry eventRegistry;

    @Before
    public void setUp() throws Exception {
        Field maxLifeTime = EventRegistryImpl.class.getDeclaredField("MAX_LIFE_TIME_SEC");
        maxLifeTime.setAccessible(true);
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(maxLifeTime, maxLifeTime.getModifiers() & ~Modifier.FINAL);
        maxLifeTime.setLong(null, 3L);

        eventRegistry = new EventRegistryImpl();
    }

    @After
    public void tearDown() throws Exception {
        ((EventRegistryImpl) eventRegistry).close();
    }

    @Test
    public void testCleanupProcess() throws Exception {

        eventRegistry.registerEvent();
        assertEquals(1L, eventRegistry.getEventsNumberLastMinute());

        Thread.sleep(5000L);

        assertEquals(0L, eventRegistry.getEventsNumberLastMinute());
    }
}
