package uz.tune.mentourBiz.rest.service.util.impl;

import com.jcraft.jsch.ChannelSftp;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import uz.tune.mentourBiz.config.logger.Logger;
import uz.tune.mentourBiz.exception.EntityNotFoundException;
import uz.tune.mentourBiz.exception.FileServiceException;
import uz.tune.mentourBiz.exception.ValidationException;
import uz.tune.mentourBiz.rest.domain.Attachment;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.School;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.group.Enrollment;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.group.Group;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Student;
import uz.tune.mentourBiz.rest.domain.userManagement.user.User;
import uz.tune.mentourBiz.rest.enums.EnrollmentStatus;
import uz.tune.mentourBiz.rest.enums.MessageKey;
import uz.tune.mentourBiz.rest.enums.UserRole;
import uz.tune.mentourBiz.rest.enums.UserStatus;
import uz.tune.mentourBiz.rest.model.AttachmentServerDto;
import uz.tune.mentourBiz.rest.model.FailedStudentModel;
import uz.tune.mentourBiz.rest.model.ResAttachmentModel;
import uz.tune.mentourBiz.rest.payload.res.ResUploadResult;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;
import uz.tune.mentourBiz.rest.repository.AttachmentRepo;
import uz.tune.mentourBiz.rest.repository.group.GroupRepository;
import uz.tune.mentourBiz.rest.repository.group.enrollment.EnrollmentRepository;
import uz.tune.mentourBiz.rest.repository.school.SchoolRepository;
import uz.tune.mentourBiz.rest.repository.student.StudentRepo;
import uz.tune.mentourBiz.rest.repository.user.UserRepo;
import uz.tune.mentourBiz.rest.service.user.UserScopeService;
import uz.tune.mentourBiz.rest.service.user.UserService;
import uz.tune.mentourBiz.rest.service.util.FileService;
import uz.tune.mentourBiz.rest.service.util.MessageSingleton;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final MessageSingleton messageSingleton;
    private final AttachmentRepo attachmentRepository;
    private final UserService userService;
    private final UserScopeService userScopeService;
    private final GroupRepository groupRepository;
    private final PasswordEncoder passwordEncoder;
    private final EnrollmentRepository enrollmentRepository;
    private final SchoolRepository schoolRepository;
    private final StudentRepo studentRepo;
    private final StudentAsyncService studentAsyncService;
    private final StudentExcelImportHelper studentExcelImportHelper;
    private final UserRepo userRepo;

    @Value("${app.ssh.username}")
    private String USERNAME;
    @Value("${app.ssh.password}")
    private String PASSWORD;
    @Value("${app.ssh.host}")
    private String HOST;
    @Value("${app.ssh.port}")
    private int PORT;

    private final String DIRECTORY = "/var/www/mentour";

    @Override
    public ResAttachmentModel upload(MultipartFile file) {
        if (Objects.nonNull(file)) {
            User currentUser = userService.getCurrentUser();

            if (file.getSize() > 25 * 1024 * 1024) {
                throw new ValidationException(MessageKey.FILE_SIZE_EXCEEDS.getKey());
            }

            String name;
            if (Objects.nonNull(file.getOriginalFilename()) && file.getOriginalFilename().contains(".")) {
                String[] split = file.getOriginalFilename().split("\\.");
                name = UUID.randomUUID() + "." + split[split.length - 1];
            } else {
                name = UUID.randomUUID().toString();
            }

            try {
                AttachmentServerDto attachment = new AttachmentServerDto(
                        file.getContentType(),
                        name,
                        file.getOriginalFilename(),
                        file.getSize(),
                        DIRECTORY + "/" + name,
                        file.getInputStream(),
                        DIRECTORY
                );
                return sendFileToSftpServer(attachment, currentUser.getId());
            } catch (IOException ignored) {
                throw new FileServiceException(MessageKey.FILE_UPLOAD_ERROR.getKey());
            }
        } else {
            throw new FileServiceException(MessageKey.FILE_IS_EMPTY.getKey());
        }
    }

    private ResAttachmentModel sendFileToSftpServer(AttachmentServerDto attachmentDto, Long userId) {
        try {
            java.nio.file.Path targetPath = java.nio.file.Paths.get(attachmentDto.fullPath());

            java.nio.file.Files.createDirectories(targetPath.getParent());

            java.nio.file.Files.copy(
                    attachmentDto.inputStream(),
                    targetPath,
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING
            );

            Attachment attachment = new Attachment(attachmentDto);
            attachment.setUserId(userId);
            attachment = attachmentRepository.save(attachment);

            return new ResAttachmentModel(attachment);
        } catch (java.io.IOException e) {
            throw new RuntimeException("Local disk write failed: " + e.getMessage(), e);
        }
    }

    @Override
    public ResAttachmentModel uploadStructured(MultipartFile file, String type, String book, String level, String fileName) {
        if (file.isEmpty()) throw new FileServiceException("File is empty");
        User user = userService.getCurrentUser();


        String cleanBook = book.toLowerCase().trim().replaceAll("\\s+", "");
        String cleanLevel = level.toLowerCase().trim().replaceAll("\\s+", "");
        String typeFolder = type.toLowerCase().contains("image") ? "images" : "listenings";

        String relativeFolder;
        if (typeFolder.equals("images")) {
            relativeFolder = String.format("studentApp/%s/%s/%s", typeFolder, cleanBook, cleanLevel);
        } else {
            relativeFolder = String.format("studentApp/%s/%s/%s", cleanBook, typeFolder, cleanLevel);
        }


        String rawFileName = (fileName != null && !fileName.isBlank()) ? fileName : file.getOriginalFilename();
        String absoluteDirectory = DIRECTORY + "/" + relativeFolder;

        try {
            AttachmentServerDto attachmentDto = new AttachmentServerDto(
                    file.getContentType(),
                    relativeFolder + "/" + rawFileName,
                    file.getOriginalFilename(),
                    file.getSize(),
                    absoluteDirectory + "/" + rawFileName,
                    file.getInputStream(),
                    absoluteDirectory
            );
            return sendFileToSftpServer(attachmentDto, user.getId());
        } catch (IOException e) {
            throw new FileServiceException("Upload failed: " + e.getMessage());
        }
    }

    @Override
    public ResAttachmentModel uploadSpeakingAudio(MultipartFile file) {
        User currentUser = userService.getCurrentUser();
        String relPath = "studentApp/speakingAudio";
        String directory = DIRECTORY + "/" + relPath; // /var/www/mentour/studentApp/speakingAudio
        String name = UUID.randomUUID() + ".mp3";

        try {
            AttachmentServerDto attachment = new AttachmentServerDto(
                    file.getContentType(),
                    relPath + "/" + name,
                    file.getOriginalFilename(),
                    file.getSize(),
                    directory + "/" + name,
                    file.getInputStream(),
                    directory
            );
            return sendFileToSftpServer(attachment, currentUser.getId());
        } catch (IOException e) {
            throw new FileServiceException("Audio upload error");
        }
    }

    @Override
    public byte[] getFileBytes(String fullPath) {
        try {
            java.nio.file.Path path = java.nio.file.Paths.get(fullPath);
            return java.nio.file.Files.readAllBytes(path);
        } catch (java.io.IOException e) {
            Logger.exception("Failed to read file directly from disk: " + fullPath, e);
            throw new RuntimeException("Failed to fetch file from local storage", e);
        }
    }

    // Deliberately NOT @Transactional: every row is committed on its own by
    // studentExcelImportHelper, so a failing row can be skipped while the valid rows are kept.
    @Override
    public ResponseMessage excelUpload(UUID groupUuid, String fileName, MultipartFile file) {
        User currentUser = userService.getCurrentUser();
        if (currentUser.getRole() != UserRole.SCHOOL_ADMIN && currentUser.getRole() != UserRole.SCHOOL_DIRECTOR) {
            throw new ValidationException(MessageKey.ACCESS_DENIED.getKey());
        }
        UUID schoolUuid = userScopeService.getCurrentUserSchoolUuid();
        School school = schoolRepository.findByUuid(schoolUuid)
                .orElseThrow(() -> new RuntimeException("School not found: " + schoolUuid));

        if (file.isEmpty()) throw new FileServiceException("File is empty");

        Group group = null;
        if (groupUuid != null) {
            group = groupRepository.findByUuid(groupUuid)
                    .orElseThrow(() -> new ValidationException(
                            messageSingleton.getMessage(MessageKey.GROUP_NOT_FOUND.getKey(), groupUuid.toString())));
            if (!group.getBranch().getSchool().getUuid().equals(school.getUuid())) {
                throw new ValidationException(
                        messageSingleton.getMessage(MessageKey.GROUP_NO_ENROLLMENT.getKey(), groupUuid.toString()));
            }
        }

        List<String> failedRows = new ArrayList<>();
        int successCount = 0;

        // Student limit is counted over the whole organization; resolved once for the whole file.
        List<UUID> scopeSchoolUuids = studentExcelImportHelper.resolveLimitScopeSchoolUuids(school.getUuid());
        UUID targetGroupUuid = (group != null) ? group.getUuid() : null;

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rows = sheet.iterator();

            if (rows.hasNext()) {
                rows.next(); // skip header
            }

            while (rows.hasNext()) {
                Row currentRow = rows.next();
                int rowNumber = currentRow.getRowNum() + 1;

                String firstName = getCellValue(currentRow.getCell(0)).trim();
                String lastName = getCellValue(currentRow.getCell(1)).trim();
                // Columns 3 and 4 are optional: written to the account when filled, ignored when empty
                String phoneNumber = getCellValue(currentRow.getCell(2)).trim();
                String address = getCellValue(currentRow.getCell(3)).trim();

                // Trailing empty rows Excel keeps in the sheet are not reported as errors
                if (firstName.isBlank() && lastName.isBlank() && phoneNumber.isBlank() && address.isBlank()) {
                    continue;
                }

                // 1. Required fields
                if (firstName.isBlank() || lastName.isBlank()) {
                    failedRows.add(rowNumber + "-qator: " + messageSingleton.getMessage(MessageKey.INVALID_ARGUMENT));
                    continue;
                }

                // 2. A row that fails for any reason is skipped, the remaining rows still import
                try {
                    // Enforce the student limit
                    long activeStudents = studentRepo.countActiveStudentsIn(scopeSchoolUuids);
                    userService.validateStudentLimit(school.getUuid(),  1);

                    // Username is generated server-side: firstname + 4 random digits
                    User userAccount = new User();
                    userAccount.setFirstName(firstName);
                    userAccount.setLastName(lastName);
                    userAccount.setUsername(generateUsername(firstName));
                    userAccount.setPassword(passwordEncoder.encode("1111"));
                    userAccount.setRole(UserRole.STUDENT);
                    userAccount.setStatus(UserStatus.ACTIVE);
                    if (!phoneNumber.isBlank()) userAccount.setPhoneNumber(phoneNumber);
                    if (!address.isBlank()) userAccount.setAddress(address);

                    // Persists the account, student profile and (optional) enrollment in its own transaction
                    studentExcelImportHelper.importStudent(userAccount, school.getUuid(), targetGroupUuid);
                    successCount++;
                } catch (Exception e) {
                    Logger.exception("Excel import: " + rowNumber + "-qator o'tkazib yuborildi", e);
                    failedRows.add(rowNumber + "-qator: " + describeRowError(e));
                }
            }

            return new ResponseMessage(buildImportSummary(successCount, failedRows));

        } catch (IOException e) {
            throw new FileServiceException("Excel faylni o'qishda xatolik: " + e.getMessage());
        }
    }

    // Successful rows are already committed, so the summary reports what was imported
    // and lists the skipped rows instead of failing the whole upload.
    private String buildImportSummary(int successCount, List<String> failedRows) {
        String summary = messageSingleton.getMessage(MessageKey.EXCEL_UPLOAD_SUCCESS)
                + " Qo'shildi: " + successCount + " ta.";
        if (!failedRows.isEmpty()) {
            summary += " O'tkazib yuborilgan qatorlar (" + failedRows.size() + " ta): "
                    + String.join("; ", failedRows);
        }
        return summary;
    }

    // Domain exceptions carry a message key (e.g. "student.limit.reached"); anything else
    // (constraint violations and the like) falls back to the raw exception message.
    private String describeRowError(Exception e) {
        String message = e.getMessage();
        if (message == null || message.isBlank()) return e.getClass().getSimpleName();
        return messageSingleton.getMessage(message, message);
    }

    // Uzbek/Russian cyrillic -> latin letters. Only lowercase keys are stored,
    // transliterate() lowercases the input before the lookup.
    private static final Map<Character, String> CYRILLIC_TO_LATIN = Map.ofEntries(
            Map.entry('а', "a"), Map.entry('б', "b"), Map.entry('в', "v"), Map.entry('г', "g"),
            Map.entry('д', "d"), Map.entry('е', "e"), Map.entry('ё', "yo"), Map.entry('ж', "j"),
            Map.entry('з', "z"), Map.entry('и', "i"), Map.entry('й', "y"), Map.entry('к', "k"),
            Map.entry('л', "l"), Map.entry('м', "m"), Map.entry('н', "n"), Map.entry('о', "o"),
            Map.entry('п', "p"), Map.entry('р', "r"), Map.entry('с', "s"), Map.entry('т', "t"),
            Map.entry('у', "u"), Map.entry('ф', "f"), Map.entry('х', "x"), Map.entry('ц', "ts"),
            Map.entry('ч', "ch"), Map.entry('ш', "sh"), Map.entry('щ', "sh"), Map.entry('ъ', ""),
            Map.entry('ы', "i"), Map.entry('ь', ""), Map.entry('э', "e"), Map.entry('ю', "yu"),
            Map.entry('я', "ya"), Map.entry('ў', "o"), Map.entry('қ', "q"), Map.entry('ғ', "g"),
            Map.entry('ҳ', "h")
    );

    // Converts a cyrillic name to latin, e.g. "Ўғилой" -> "ogiloy".
    // Characters outside the table (already latin, digits, spaces) are kept as-is.
    private static String transliterate(String text) {
        if (text == null || text.isBlank()) return "";

        String lower = text.trim().toLowerCase();
        StringBuilder result = new StringBuilder(lower.length());
        for (char ch : lower.toCharArray()) {
            String latin = CYRILLIC_TO_LATIN.get(ch);
            result.append(latin != null ? latin : ch);
        }
        return result.toString();
    }

    // Username format: lowercase latin first name + 4 random digits, e.g. john6875.
    // Cyrillic names are transliterated first, so "Жасур" becomes jasur1234.
    // Retries until unique; falls back to "student" when the name has no latin letters.
    private String generateUsername(String firstName) {
        String base = transliterate(firstName).replaceAll("[^a-z0-9]", "");
        if (base.isBlank()) base = "student";

        String username;
        do {
            int suffix = java.util.concurrent.ThreadLocalRandom.current().nextInt(1000, 10000);
            username = base + suffix;
        } while (userRepo.existsByUsername(username));
        return username;
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                }
                return String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return "";
        }
    }

    private static boolean directoryExists(ChannelSftp channelSftp, String directoryPath) {
        try {
            channelSftp.lstat(directoryPath);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public ResUploadResult excelUploadRes(String fileName, MultipartFile file) {
        User currentUser = userService.getCurrentUser();
        if (currentUser.getRole() != UserRole.SCHOOL_ADMIN && currentUser.getRole() != UserRole.SCHOOL_DIRECTOR) {
            throw new ValidationException(MessageKey.ACCESS_DENIED.getKey());
        }
        UUID schoolUuid = userScopeService.getCurrentUserSchoolUuid();
        School school = schoolRepository.findByUuid(schoolUuid)
                .orElseThrow(() -> new RuntimeException("School not found: " + schoolUuid));

        if (file.isEmpty()) throw new FileServiceException("File is empty");

        List<FailedStudentModel> failedStudents = new ArrayList<>();

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rows = sheet.iterator();

            if (rows.hasNext()) rows.next(); // header o'tkazib yuborish

            while (rows.hasNext()) {
                Row currentRow = rows.next();

                String firstName  = getCellValue(currentRow.getCell(0));
                String lastName   = getCellValue(currentRow.getCell(1));
                String groupIdRaw = getCellValue(currentRow.getCell(2));

                if (firstName.isBlank() || lastName.isBlank() || groupIdRaw.isBlank()) {
                    failedStudents.add(new FailedStudentModel(
                            firstName, lastName, "", groupIdRaw,
                            "Bo'sh katak mavjud"
                    ));
                    continue;
                }

                long groupId = Long.parseLong(groupIdRaw);

                // Username is generated server-side: firstname + 4 random digits
                String username = generateUsername(firstName);

                User userAccount = new User();
                userAccount.setFirstName(firstName);
                userAccount.setLastName(lastName);
                userAccount.setUsername(username);
                userAccount.setPassword(passwordEncoder.encode("1111"));
                userAccount.setRole(UserRole.STUDENT);
                userAccount.setStatus(UserStatus.ACTIVE);

                User savedUser = userRepo.save(userAccount);

                Student s = new Student();
                s.setUser(savedUser);
                s.setSchool(school);
                Student savedStudent = studentRepo.save(s);

                Enrollment enrollment = new Enrollment();
                enrollment.setStatus(EnrollmentStatus.STARTED);
                enrollment.setStudent(savedStudent);

                if (groupId != 0) {
                    Group group = groupRepository.findById(groupId)
                            .orElseThrow(() -> new RuntimeException("Group not found: " + groupId));
                    enrollment.setGroup(group);
                }
                enrollmentRepository.save(enrollment);
            }

        } catch (IOException e) {
            throw new FileServiceException("Excel faylni o'qishda xatolik: " + e.getMessage());
        }

        return new ResUploadResult(failedStudents);
    }

    @Override
    public byte[] generateExcelTemplate() {
        try (Workbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = wb.createSheet("Students");

            CellStyle headerStyle = wb.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);

            CellStyle exampleStyle = wb.createCellStyle();
            exampleStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
            exampleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            exampleStyle.setBorderBottom(BorderStyle.THIN);
            exampleStyle.setBorderTop(BorderStyle.THIN);
            exampleStyle.setBorderLeft(BorderStyle.THIN);
            exampleStyle.setBorderRight(BorderStyle.THIN);

            Font exampleFont = wb.createFont();
            exampleFont.setItalic(true);
            exampleFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
            exampleStyle.setFont(exampleFont);

            String[] headers = {"Name", "SurName", "Phone", "Address"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            sheet.setColumnWidth(0, 5000);
            sheet.setColumnWidth(1, 5000);
            sheet.setColumnWidth(2, 5000);
            sheet.setColumnWidth(3, 8000);

            wb.write(out);
            return out.toByteArray();

        } catch (IOException e) {
            throw new FileServiceException("Shablon yaratishda xatolik: " + e.getMessage());
        }
    }
}