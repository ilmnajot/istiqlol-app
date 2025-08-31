package org.example.moliyaapp.utils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class DateUtils {

    public static LocalDateTime startOfToday() {
        return LocalDate.now().atStartOfDay();
    }

    public static LocalDateTime endOfToday() {
        return LocalDateTime.of(LocalDate.now(), LocalTime.MAX);
    }

    public static LocalDateTime startOfWeek() {
        return LocalDate.now().with(DayOfWeek.MONDAY).atStartOfDay();
    }

    public static LocalDateTime startOfMonth() {
        return LocalDate.now().withDayOfMonth(1).atStartOfDay();
    }

    public static LocalDateTime startOfYear() {
        return LocalDate.now().withDayOfYear(1).atStartOfDay();
    }
}
