package com.snoozeshare.api.response;

import java.time.LocalDate;
import java.util.List;

/** One Host calendar month with date cells and inclusive availability ranges. */
public record HostCalendarResponse(
        int year,
        int month,
        List<Day> days,
        List<AvailabilityBlockResponse> blocks) {

    public record Day(LocalDate date, boolean booked, boolean blocked) { }
}
