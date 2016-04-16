package ru.yandex.eventregistry;

/**
 * Counts events, stores event stats such as:
 *
 * 1) events number for the last minute
 *
 * 2) events number for the last hour
 *
 * 3) events number for the last day
 *
 * Created by Andrei Kapitonov on 4/15/16.
 */
public interface EventRegistry {

    /**
     * Registers event
     */
    void registerEvent();

    /**
     *
     * @return number of registered events for the last minute
     */
    long getEventsNumberLastMinute();

    /**
     *
     * @return number of registered events for the last hour
     */
    long getEventsNumberLastHour();

    /**
     *
     * @return number of registered events for the last day
     */
    long getEventsNumberLastDay();
}
