package uz.tune.mentourBiz.utils;

import uz.tune.mentourBiz.rest.domain.schoolManagement.schedule.AttendanceRecord;

import java.time.Instant;
import java.util.Comparator;

// Bitta student + bitta dars uchun bazada bir nechta attendance yozuvi bo'lib qolgan hollar bor:
// dars yaratilganda ham, o'sha darsga schedule ochilganda ham NOT_MARKED yozuv qo'shilgan.
// O'qish paytida ular jimgina bittaga keltiriladi, aks holda Collectors.toMap "Duplicate key" bilan yiqiladi.
public final class AttendanceUtils {

    private AttendanceUtils() {
    }

    // Ustunlik tartibi: o'qituvchi belgilagani > eng oxirgi yangilangani > eng katta id.
    public static final Comparator<AttendanceRecord> CANONICAL = Comparator
            .comparing((AttendanceRecord a) -> Boolean.TRUE.equals(a.getIsMarked()))
            .thenComparing(a -> a.getUpdatedAt() != null ? a.getUpdatedAt() : Instant.EPOCH)
            .thenComparing(a -> a.getId() != null ? a.getId() : 0L);

    public static AttendanceRecord canonical(AttendanceRecord a, AttendanceRecord b) {
        return CANONICAL.compare(a, b) >= 0 ? a : b;
    }
}
