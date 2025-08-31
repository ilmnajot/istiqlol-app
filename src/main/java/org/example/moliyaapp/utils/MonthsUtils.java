package org.example.moliyaapp.utils;

import org.example.moliyaapp.enums.Months;
import java.util.Map;
import static java.util.Map.entry;

public final class MonthsUtils {
    public static final Map<Months, Integer> ACADEMIC_MONTH_ORDER = Map.ofEntries(
            entry(Months.SENTABR, 1),
            entry(Months.OKTABR, 2),
            entry(Months.NOYABR, 3),
            entry(Months.DEKABR, 4),
            entry(Months.YANVAR, 5),
            entry(Months.FEVRAL, 6),
            entry(Months.MART, 7),
            entry(Months.APREL, 8),
            entry(Months.MAY, 9),
            entry(Months.IYUN, 10),
            entry(Months.IYUL, -1),
            entry(Months.AVGUST, -1)
    );

    private MonthsUtils() {
        // Util klass - instance yaratishni oldini olish
    }

    public static int getAcademicYearForMonth(Months month, String academicYear) {
        if (!ACADEMIC_MONTH_ORDER.containsKey(month)) {
            throw new IllegalArgumentException("Berilgan oy o'quv yiliga tegishli emas: " + month);
        }

        int startYear = Integer.parseInt(academicYear.split("-")[0]);
        return ACADEMIC_MONTH_ORDER.get(month) <= 4 ? startYear : startYear + 1;
    }
}