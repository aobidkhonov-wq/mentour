package uz.tune.mentourBiz.utils;

import uz.tune.mentourBiz.rest.model.ResDateModel;

import java.time.*;
import java.time.format.DateTimeFormatter;

public class DateUtils {

    public static DateTimeFormatter fLocalDateDotted = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    public static DateTimeFormatter fLocalDateDashed = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static DateTimeFormatter fLocalDateForContact= DateTimeFormatter.ofPattern("ddMM-yy");
    public static DateTimeFormatter fLocalTimeWithSecond = DateTimeFormatter.ofPattern("HH:mm:ss");
    public static DateTimeFormatter fLocalTime = DateTimeFormatter.ofPattern("HH:mm");

    public static final ZoneId DEFAULT_SCHOOL_ZONE = ZoneId.of("Asia/Tashkent");

    /** A school's wall clock, falling back to Tashkent (+5) for schools without an offset. */
    public static ZoneId schoolZone(Integer utcOffset) {
        return utcOffset != null ? ZoneOffset.ofHours(utcOffset) : DEFAULT_SCHOOL_ZONE;
    }

    /**
     * The date an instant falls on in the school's own timezone ("yyyy-MM-dd"). Clients get this
     * ready-made because response timestamps carry the school's wall clock under a UTC label:
     * converting them again on the client moves evening lessons onto the next day.
     */
    public static String schoolDate(Instant instant, Integer utcOffset) {
        return instant == null ? null : fLocalDateDotted.withZone(schoolZone(utcOffset)).format(instant);
    }

    /** The clock time an instant falls on in the school's own timezone ("HH:mm"). */
    public static String schoolTime(Instant instant, Integer utcOffset) {
        return instant == null ? null : fLocalTime.withZone(schoolZone(utcOffset)).format(instant);
    }

    public static ResDateModel getDateModel(LocalDateTime dateTime) {
        String localDate = dateTime.toLocalDate().format(fLocalDateDashed);
        String localTime = dateTime.toLocalTime().format(fLocalTimeWithSecond);
        return new ResDateModel(localDate, localTime);
    }

    public static LocalDate parseLocalDate(String dateString) {
        return LocalDate.parse(dateString, fLocalDateDashed);
    }
}
