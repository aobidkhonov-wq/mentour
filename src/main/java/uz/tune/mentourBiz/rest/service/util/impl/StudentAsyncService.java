package uz.tune.mentourBiz.rest.service.util.impl;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.School;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.group.Enrollment;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Student;
import uz.tune.mentourBiz.rest.domain.userManagement.user.User;
import uz.tune.mentourBiz.rest.enums.EnrollmentStatus;
import uz.tune.mentourBiz.rest.enums.UserRole;
import uz.tune.mentourBiz.rest.repository.group.GroupRepository;
import uz.tune.mentourBiz.rest.repository.group.enrollment.EnrollmentRepository;
import uz.tune.mentourBiz.rest.repository.student.StudentRepo;
import uz.tune.mentourBiz.rest.service.NotificationService;
import uz.tune.mentourBiz.rest.service.user.UserService;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

@Service
@RequiredArgsConstructor
public class StudentAsyncService {
    private final StudentRepo studentRepo;
    private final UserService userService;
    private final GroupRepository groupRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService; // Sizdagi notification tizimi

//    @Async
//    public void processExcelAsync(byte[] fileBytes, School school, User currentUser) {
//        StringBuilder errorLog = new StringBuilder();
//        int successCount = 0;
//        int failCount = 0;
//
//        try (InputStream is = new ByteArrayInputStream(fileBytes);
//             Workbook workbook = new XSSFWorkbook(is)) {
//
//            Sheet sheet = workbook.getSheetAt(0);
//            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
//                Row row = sheet.getRow(i);
//                if (row == null) continue;
//
//                String firstName = getCellValue(row.getCell(0));
//                String lastName = getCellValue(row.getCell(1));
//
//                try {
//                    if (firstName.isEmpty() || lastName.isEmpty() || getCellValue(row.getCell(2)).isEmpty()) {
//                        throw new Exception("Ma'lumotlar to'liq emas");
//                    }
//
//                    saveStudentLogic(row, school, currentUser);
//                    successCount++;
//                } catch (Exception e) {
//                    failCount++;
//                    errorLog.append(String.format("\n- %s %s: %s", firstName, lastName, e.getMessage()));
//                }
//            }
//
//            String message = String.format("Excel import yakunlandi. \n✅ Muvaffaqiyatli: %d \n❌ Xatolar: %d %s",
//                                            successCount, failCount, errorLog.toString());
//
////            notificationService.send(currentUser.getId(), message);
//
//        } catch (IOException e) {
//            notificationService.send(currentUser.getId(), "Faylni o'qishda texnik xato yuz berdi.");
//        }
//    }
//
//    // Student saqlash logikasi (Siz yozgan kod)
//
//
//    private String getCellValue(Cell cell) {
//        if (cell == null) return "";
//        // ... (Sizdagi getCellValue kodi)
//        return ""; // qisqartirildi
//    }
}