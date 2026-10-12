package com.events.goup.service;

import com.events.goup.entity.Event;
import com.events.goup.entity.enums.EventStatus;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZonedDateTime;

public final class EventSchedule {
    private EventSchedule() {
    }

    public static boolean hasEnded(Event event, Clock clock) {
        if (event.getStatus() == EventStatus.FINISHED) {
            return true;
        }
        ZonedDateTime now = ZonedDateTime.now(clock);
        LocalDate today = now.toLocalDate();
        if (event.getEventDate().isBefore(today)) {
            return true;
        }
        return event.getEventDate().isEqual(today)
                && event.getEndTime() != null
                && !event.getEndTime().isAfter(now.toLocalTime());
    }
}
