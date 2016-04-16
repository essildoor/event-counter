package ru.yandex.eventregistry;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Events number stored in the {@link Map}, where:
 *  - keys are time in seconds (e.g. System.currentTimeMillis() / 1000). Type is Long.
 *  - values are events numbers, registered during one second. Type is Long.
 *
 * example:
 *  starting 1460802517351 ms time we have received 3 events:
 *  [first event,   1460802517351] - it will correspond to 1460802517 sec as key:           (1460802517 -> 1)
 *  [second event,  1460802517567] - it will also correspond to 1460802517 sec as key:      (1460802517 -> 2)
 *  [third event,   1460802518899] - this one will correspond to the next second as key:    (1460802518 -> 1)
 *
 * Assuming requirement of the longest query period of 1 day, cleanup task is scheduled with the fix delay
 * in order to remove obsolete data from the storage
 *
 * Created by Andrey Kapitonov on 4/15/16.
 */
public class EventRegistryImpl implements EventRegistry {

    /**
     * Any events data which was registered prior this time is removed by cleaning service task
     */
    private static final long MAX_LIFE_TIME_SEC = TimeRange.LAST_DAY.getSec();
    private static final long CLEANUP_DELAY_SEC = 60L;

    private final Map<Long, Long> eventsCounter;
    private final ScheduledExecutorService cleaningFairy;

    public EventRegistryImpl() {
        eventsCounter = new ConcurrentHashMap<>();
        cleaningFairy = Executors.newSingleThreadScheduledExecutor();
        Runnable cleaningTask = () -> {
            if (eventsCounter.isEmpty()) return;

            final long MIN_BORN_TIME = System.currentTimeMillis() / 1000 - MAX_LIFE_TIME_SEC;

            //get all keys older than current MIN_BORN_TIME and remove them from storage
            eventsCounter.keySet().stream()
                    .sorted()
                    .filter((key) -> key < MIN_BORN_TIME)
                    .collect(Collectors.toList())
                    .forEach(eventsCounter::remove);
        };
        cleaningFairy.scheduleWithFixedDelay(cleaningTask, MAX_LIFE_TIME_SEC + 1L, CLEANUP_DELAY_SEC, TimeUnit.SECONDS);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void registerEvent() {
        final long CURRENT_SEC = System.currentTimeMillis() / 1000;
        eventsCounter.compute(CURRENT_SEC, (k, v) -> v == null ? 1L : v + 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getEventsNumberLastMinute() {
        return getEventsNumber(TimeRange.LAST_MINUTE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getEventsNumberLastHour() {
        return getEventsNumber(TimeRange.LAST_HOUR);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getEventsNumberLastDay() {
        return getEventsNumber(TimeRange.LAST_DAY);
    }

    /*
    for testing purposes
     */
    protected long getEventsNumberLast5sec() {
        return getEventsNumber(TimeRange.LAST_5_SEC);
    }

    private long getEventsNumber(TimeRange timeRange) {
        final long END_TIME_SEC = System.currentTimeMillis() / 1000;
        final long START_TIME_SEC = END_TIME_SEC - timeRange.getSec();

        //get all keys in the specified time interval
        List<Long> keys = eventsCounter.keySet().stream()
                .sorted()
                .filter((key) -> key >= START_TIME_SEC && key <= END_TIME_SEC)
                .collect(Collectors.toList());

        return keys.isEmpty() ? 0 : keys.stream().mapToLong(eventsCounter::get).sum();
    }

    /**
     * Call this method if you don't need event counter anymore
     */
    public void close() {
        cleaningFairy.shutdown();
        try {
            cleaningFairy.awaitTermination(10L, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            if (!cleaningFairy.isShutdown()) cleaningFairy.shutdownNow();
        }
    }

    private enum TimeRange {
        LAST_5_SEC(5L),
        LAST_MINUTE(60L),
        LAST_HOUR(3_600L),
        LAST_DAY(86_400L),
        ;

        private long sec;

        TimeRange(long millis) {
            this.sec = millis;
        }

        public long getSec() {
            return sec;
        }
    }
}
