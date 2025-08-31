package org.example.moliyaapp.dto;

import org.example.moliyaapp.enums.Months;

import java.time.Month;
import java.util.HashMap;
import java.util.Map;

public class MonthToMonthsMapper {

    private static final Map<Month, Months> MONTH_MAP = new HashMap<>();

    static {
        MONTH_MAP.put(Month.SEPTEMBER, Months.SENTABR);
        MONTH_MAP.put(Month.OCTOBER, Months.OKTABR);
        MONTH_MAP.put(Month.NOVEMBER, Months.NOYABR);
        MONTH_MAP.put(Month.DECEMBER, Months.DEKABR);
        MONTH_MAP.put(Month.JANUARY, Months.YANVAR);
        MONTH_MAP.put(Month.FEBRUARY, Months.FEVRAL);
        MONTH_MAP.put(Month.MARCH, Months.MART);
        MONTH_MAP.put(Month.APRIL, Months.APREL);
        MONTH_MAP.put(Month.MAY, Months.MAY);
        MONTH_MAP.put(Month.JUNE, Months.IYUN);
    }

    public static Months map(Month month) {
        Months result = MONTH_MAP.get(month);
        if (result == null) {
            throw new IllegalArgumentException("Bu oy o‘quv oylariga kirmaydi: " + month);
        }
        return result;
    }
}
