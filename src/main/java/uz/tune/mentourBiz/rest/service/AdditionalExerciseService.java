package uz.tune.mentourBiz.rest.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import uz.tune.mentourBiz.config.logger.Logger;
import uz.tune.mentourBiz.exception.EntityNotFoundException;
import uz.tune.mentourBiz.exception.PermissionForbidden;
import uz.tune.mentourBiz.exception.ValidationException;
import uz.tune.mentourBiz.rest.admin.req.ReqUpdateWordTranslation;
import uz.tune.mentourBiz.rest.admin.req.create.adminCreate.ReqCreateTask;
import uz.tune.mentourBiz.rest.admin.req.create.adminCreate.ReqCreateUnit;
import uz.tune.mentourBiz.rest.admin.req.create.adminCreate.ReqCreateVocabSet;
import uz.tune.mentourBiz.rest.admin.req.upd.*;
import uz.tune.mentourBiz.rest.admin.res.ResContentHeader;
import uz.tune.mentourBiz.rest.domain.Organization;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.SchoolBook;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.Unit;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.exercise.ExerciseQuestion;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.exercise.ExerciseTask;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.vocabulary.VocabularyQuestion;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.vocabulary.VocabularySet;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.vocabulary.VocabularyWord;
import uz.tune.mentourBiz.rest.domain.schoolManagement.course.Course;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.School;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.group.Level;
import uz.tune.mentourBiz.rest.domain.userManagement.user.User;
import uz.tune.mentourBiz.rest.enums.*;
import uz.tune.mentourBiz.rest.model.ResAttachmentModel;
import uz.tune.mentourBiz.rest.payload.ReqUpsertVocabWord;
import uz.tune.mentourBiz.rest.payload.questionFormats.types.answers.typesOfAnswes.*;
import uz.tune.mentourBiz.rest.payload.questionFormats.types.nodes.*;
import uz.tune.mentourBiz.rest.payload.req.ReqDetailedQuestionCreate;
import uz.tune.mentourBiz.rest.payload.req.combinedBody.ReqCombinedUnitExerciseTask;
import uz.tune.mentourBiz.rest.payload.res.ResBooks;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;
import uz.tune.mentourBiz.rest.payload.res.exercise.ResVocabSet;
import uz.tune.mentourBiz.rest.payload.res.lesson.ResUnit;
import uz.tune.mentourBiz.rest.payload.studentRes.res.exercise.ResAdminQuestions;
import uz.tune.mentourBiz.rest.payload.studentRes.res.exercise.ResVocabLearnWord;
import uz.tune.mentourBiz.rest.repository.SchoolDirectorRepo;
import uz.tune.mentourBiz.rest.repository.course.CourseRepo;
import uz.tune.mentourBiz.rest.repository.group.LevelRepository;
import uz.tune.mentourBiz.rest.repository.school.SchoolBookRepository;
import uz.tune.mentourBiz.rest.repository.school.SchoolRepo;
import uz.tune.mentourBiz.rest.repository.unit.UnitRepository;
import uz.tune.mentourBiz.rest.repository.unit.exercise.ExerciseQuestionRepository;
import uz.tune.mentourBiz.rest.repository.unit.exercise.ExerciseTaskRepository;
import uz.tune.mentourBiz.rest.repository.unit.exercise.VocabularyWordRepository;
import uz.tune.mentourBiz.rest.repository.unit.exercise.vocabulary.VocabularyQuestionRepository;
import uz.tune.mentourBiz.rest.repository.unit.exercise.vocabulary.VocabularySetRepository;
import uz.tune.mentourBiz.rest.service.user.UserScopeService;
import uz.tune.mentourBiz.rest.service.user.UserService;
import uz.tune.mentourBiz.rest.service.util.FileService;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdditionalExerciseService {

    private final SchoolBookRepository bookRepo;
    private final UnitRepository unitRepo;
    private final ExerciseTaskRepository taskRepo;
    private final ExerciseQuestionRepository questionRepo;
    private final UserService userService;
    private final UserScopeService userScopeService;
    private final SchoolRepo schoolRepo;
    private final LevelRepository levelRepo;
    private final SchoolBookRepository schoolBookRepository;
    private final VocabularyWordRepository vocabularyWordRepository;
    private final VocabularyQuestionRepository vocabularyQuestionRepository;
    private final VocabularySetRepository vocabularySetRepository;
    private final ExerciseTaskRepository exerciseTaskRepository;
    private final FileService fileService;
    private static final Integer SCHOOL_AUDIO_LIMIT = 50;
    private final CourseRepo courseRepo;
    private final SchoolDirectorRepo schoolDirectorRepo;

    @Transactional
    public ResAttachmentModel uploadListeningFile(MultipartFile file, UUID bookUuid) {
        User user = userService.getCurrentUser();

        List<UUID> authorizedUuids = userScopeService.getAuthorizedSchoolUuids();
        if (authorizedUuids.isEmpty() && !user.getRole().equals(UserRole.SYS_ADMIN)) {
            throw new PermissionForbidden(MessageKey.SCHOOL_CONTEXT_REQUIRED.getKey());
        }

        SchoolBook book = schoolBookRepository.findByUuid(bookUuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.SCHOOL_BOOK_NOT_FOUND.getKey()));

        if (!user.getRole().equals(UserRole.SYS_ADMIN)) {
            boolean canAccess = (book.getSchool() != null && authorizedUuids.contains(book.getSchool().getUuid())) ||
                    (book.getOrganization() != null && authorizedUuids.stream().anyMatch(sid -> schoolRepo.findByUuid(sid).get().getOrganization().equals(book.getOrganization())));

            if (!canAccess) throw new PermissionForbidden(MessageKey.ACCESS_DENIED.getKey());
        }

        Random rand = new Random();
        return fileService.uploadStructured(
                file,
                "listenings",
                book.getName(),
                book.getLevel().getName(),
                "listening-" + (rand.nextInt(9000) + 1000)
        );
    }

    @Transactional
    public void deleteVocabWord(UUID wordUuid, UUID setUuid) {
        VocabularyQuestion vq = vocabularyQuestionRepository.findByVocabularyWordAndVocabularySetUuid(
                        vocabularyWordRepository.findByUuid(wordUuid).orElseThrow(() -> new EntityNotFoundException(MessageKey.WORD_NOT_FOUND.getKey())),
                        setUuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.WORD_NOT_LINKED.getKey()));

        authorize(vq.getVocabularySet().getUnit().getSchoolBook());

        vocabularyQuestionRepository.delete(vq);
    }

    @Transactional(readOnly = true)
    public List<ResVocabLearnWord> getWordsBySetUuid(UUID setUuid) {
        VocabularySet vocabSet = vocabularySetRepository.findByUuid(setUuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.VOCAB_SET_NOT_FOUND.getKey()));

        authorize(vocabSet.getUnit().getSchoolBook());

        return vocabularyQuestionRepository.findAllByVocabularySet_Uuid(setUuid).stream()
                .map(q -> new ResVocabLearnWord(q.getVocabularyWord(), q))
                .toList();
    }

    @Transactional
    public ResponseMessage bulkUpdateWordTranslations(List<ReqUpdateWordTranslation> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new ValidationException(MessageKey.LIST_EMPTY.getKey());
        }

        for (ReqUpdateWordTranslation req : requests) {
            VocabularyWord word = vocabularyWordRepository.findByUuid(req.getWordUuid())
                    .orElseThrow(() -> new EntityNotFoundException(MessageKey.WORD_NOT_FOUND.getKey()));

            if (req.getTranslationUz() != null) word.setTranslationUz(req.getTranslationUz());
            if (req.getTranslationRu() != null) word.setTranslationRu(req.getTranslationRu());
            if (req.getTranslationTjk() != null) word.setTranslationTjk(req.getTranslationTjk());
            if (req.getTranslationKaa() != null) word.setTranslationKaa(req.getTranslationKaa());
            if (req.getTranslationKrg() != null) word.setTranslationKrg(req.getTranslationKrg());

            vocabularyWordRepository.save(word);
        }

        return new ResponseMessage("Successfully updated " + requests.size() + " words.");
    }


    private void authorize(SchoolBook book) {
        User user = userService.getCurrentUser();

        if (user.getRole() == UserRole.SYS_ADMIN) return;

        List<UUID> authorizedUuids = userScopeService.getAuthorizedSchoolUuids();

        boolean isDirectlyOwned = book.getSchool() != null && authorizedUuids.contains(book.getSchool().getUuid());
        boolean isOrgOwned = book.getOrganization() != null &&
                authorizedUuids.stream().anyMatch(sid -> {
                    var schoolOpt = schoolRepo.findByUuid(sid);
                    return schoolOpt.isPresent()
                            && schoolOpt.get().getOrganization() != null
                            && schoolOpt.get().getOrganization().equals(book.getOrganization());
                });
        boolean isGlobalAllowed = book.isGlobal();

        if (!isDirectlyOwned && !isOrgOwned && !isGlobalAllowed) {
            throw new PermissionForbidden(MessageKey.ORG_MISMATCH.getKey());
        }
    }

    private boolean checkEditPermission(SchoolBook book) {
        User user = userService.getCurrentUser();
        if (user.getRole() == UserRole.SYS_ADMIN) return true;
        if (book.isGlobal()) return false; // Non-SysAdmins cannot edit global books

        List<UUID> authorizedUuids = userScopeService.getAuthorizedSchoolUuids();

        if (book.getSchool() != null && authorizedUuids.contains(book.getSchool().getUuid())) return true;

        if (user.getRole() == UserRole.SCHOOL_DIRECTOR && book.getOrganization() != null) {
            return schoolDirectorRepo.findByUser(user)
                    .map(d -> d.getOrganization().equals(book.getOrganization()))
                    .orElse(false);
        }

        return false;
    }

    @Transactional(readOnly = true)
    public ResAdminQuestions getQuestion(UUID uuid) {
        ExerciseQuestion question = questionRepo.findByUuid(uuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.EXERCISE_QUESTION_NOT_FOUND.getKey()));

        authorizeQuestionOwnershipReadOnly(question);

        return new ResAdminQuestions(question);
    }

    private void authorizeQuestionOwnershipReadOnly(ExerciseQuestion question) {
        User currentUser = userService.getCurrentUser();
        if (currentUser.getRole() == UserRole.SYS_ADMIN) return;
        UUID userSchoolId = userScopeService.getCurrentUserSchoolUuid();
        List<SchoolBook> allowedBooks = schoolBookRepository.findAvailableForSchool(userSchoolId);
        boolean hasAccess = question.getExerciseTask().stream()
                .map(task -> task.getUnit().getSchoolBook().getUuid())
                .anyMatch(bookUuid -> allowedBooks.stream()
                        .anyMatch(allowed -> allowed.getUuid().equals(bookUuid)));

        if (!hasAccess) {
            throw new PermissionForbidden(MessageKey.ACCESS_DENIED.getKey());
        }
    }


    @Transactional(readOnly = true)
    public ResVocabLearnWord getVocabWord(UUID uuid) {
        User user = userService.getCurrentUser();
        VocabularyWord word = vocabularyWordRepository.findByUuid(uuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.WORD_NOT_FOUND.getKey()));

        if (user.getRole() == UserRole.SYS_ADMIN) return new ResVocabLearnWord(word);

        UUID userSchoolUuid = userScopeService.getCurrentUserSchoolUuid();

        boolean canSee = vocabularyQuestionRepository.existsByWordUuidAndSchoolUuid(word.getUuid(), userSchoolUuid);

        if (!canSee) {
            throw new PermissionForbidden(MessageKey.ACCESS_DENIED.getKey());
        }

        return new ResVocabLearnWord(word);
    }



    @Transactional(readOnly = true)
    public List<ResContentHeader> getTasksForManagement(UUID unitUuid) {
        Unit unit = unitRepo.findByUuid(unitUuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.UNIT_NOT_FOUND.getKey()));

        authorize(unit.getSchoolBook());

        return taskRepo.findAllByUnit_UuidAndStatus(unitUuid, ExerciseTaskStatus.ACTIVE).stream()
                .map(t -> new ResContentHeader(
                        t.getUuid(), t.getTitle(), t.getTopic(),
                        t.getSortOrder(), t.getStatus().name(), t.getSectionType()
                )).toList();
    }

    @Transactional(readOnly = true)
    public List<ResContentHeader> getVocabTasksForManagement(UUID unitUuid) {
        Unit unit = unitRepo.findByUuid(unitUuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.UNIT_NOT_FOUND.getKey()));

        authorize(unit.getSchoolBook());

        return vocabularySetRepository.findAllByUnit_Uuid(unitUuid).stream()
                .map(t -> new ResContentHeader(
                        t.getUuid(), t.getTitle(), null,
                        t.getSortOrder(), t.getStatus().name(), LessonSectionType.VOCABULARY
                )).toList();
    }

    @Transactional(readOnly = true)
    public List<ResVocabLearnWord> getVocabQuestionsForManagement(UUID taskUuid) {
        VocabularySet task = vocabularySetRepository.findByUuid(taskUuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.EXERCISE_TASK_NOT_FOUND.getKey()));

        authorize(task.getUnit().getSchoolBook());

        return vocabularyQuestionRepository.findAllByVocabularySet_Uuid(taskUuid).stream()
                .map(q -> new ResVocabLearnWord(q.getVocabularyWord(), q))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ResAdminQuestions> getQuestionsForManagement(UUID taskUuid) {
        ExerciseTask task = taskRepo.findByUuid(taskUuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.EXERCISE_TASK_NOT_FOUND.getKey()));

        authorize(task.getUnit().getSchoolBook());

        return questionRepo.findAllByExerciseTask_Uuid(taskUuid).stream()
                .map(ResAdminQuestions::new)
                .toList();
    }


    @Transactional(readOnly = true)
    public List<ResBooks> getAvailableBooks(Boolean isGlobal, UUID schoolUuid) {
        User user = userService.getCurrentUser();

        UUID resolvedId = userScopeService.resolveSchoolUuid(schoolUuid);
        Collection<UUID> schoolUuids;
        UUID orgUuid = null;

        if (user.getRole() == UserRole.SYS_ADMIN) {
            if (resolvedId != null) {
                schoolUuids = List.of(resolvedId);
                orgUuid = schoolRepo.findByUuid(resolvedId)
                        .map(s -> s.getOrganization() != null ? s.getOrganization().getUuid() : null)
                        .orElse(null);
            } else {
                return bookRepo.findAll().stream().map(ResBooks::new).toList();
            }
        }
        else if (user.getRole() == UserRole.SCHOOL_DIRECTOR) {
            orgUuid = schoolDirectorRepo.findByUser(user)
                    .map(sd -> sd.getOrganization().getUuid())
                    .orElseThrow(() -> new EntityNotFoundException(MessageKey.DIRECTOR_NOT_FOUND.getKey()));
            schoolUuids = (resolvedId != null) ? List.of(resolvedId) : userScopeService.getAuthorizedSchoolUuids();
        }
        else {
            if (resolvedId == null) {
                throw new ValidationException(MessageKey.SCHOOL_CONTEXT_REQUIRED.getKey());
            }

            School targetSchool = schoolRepo.findByUuid(resolvedId)
                    .orElseThrow(() -> new EntityNotFoundException(MessageKey.SCHOOL_NOT_FOUND.getKey()));

            orgUuid = (targetSchool.getOrganization() != null) ? targetSchool.getOrganization().getUuid() : null;
            schoolUuids = List.of(resolvedId);
        }

        return schoolBookRepository.findBooksFiltered(schoolUuids, orgUuid, isGlobal, Pageable.unpaged())
                .getContent().stream()
                .map(ResBooks::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ResUnit> getUnits(UUID bookUuid) {
        SchoolBook book = bookRepo.findByUuid(bookUuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.SCHOOL_BOOK_NOT_FOUND.getKey()));

        authorize(book);

        UnitStatus filterStatus = (userService.getCurrentUser().getRole() == UserRole.SYS_ADMIN) ? null : UnitStatus.ACTIVE;

        return unitRepo.findAllBySchoolBookUuidAndStatus(bookUuid, filterStatus).stream()
                .sorted(Comparator.comparing(Unit::getSortOrder))
                .map(ResUnit::new)
                .toList();
    }
    // book

    @Transactional
    public UUID createBook(String name, UUID levelUuid, UUID targetSchoolId) {
        User user = userService.getCurrentUser();
        Level level = levelRepo.findByUuid(levelUuid).orElseThrow();

        SchoolBook book = new SchoolBook();
        book.setName(name);
        book.setCreatedBy(user);
        book.setLevel(level);
        book.setStatus(SchoolBookStatus.ACTIVE);

        if (user.getRole() == UserRole.SYS_ADMIN) {
            if (targetSchoolId != null) {
                School target = schoolRepo.findByUuid(targetSchoolId).orElseThrow();
                book.setSchool(target);
                book.setOrganization(target.getOrganization());
                book.setGlobal(false);
            } else {
                book.setGlobal(true);
            }
        }
        else if (user.getRole() == UserRole.SCHOOL_DIRECTOR) {
            Organization org = schoolDirectorRepo.findByUser(user)
                    .orElseThrow(() -> new EntityNotFoundException(MessageKey.DIRECTOR_NOT_FOUND.getKey())).getOrganization();

            book.setOrganization(org);
            book.setGlobal(false);

            if (targetSchoolId != null) {
                // Scoped to 1 branch
                School target = schoolRepo.findByUuid(targetSchoolId).orElseThrow();
                if (!org.getUuid().equals(target.getOrganization().getUuid())) throw new PermissionForbidden(MessageKey.UNAUTHORIZED.getKey());
                book.setSchool(target);
            } else {
                // Scoped to whole Organization (NULL school)
                book.setSchool(null);
            }
        }
        else {
            School school = schoolRepo.findByUuid(userScopeService.getCurrentUserSchoolUuid()).get();
            book.setSchool(school);
            book.setOrganization(school.getOrganization());
            book.setGlobal(false);
        }

        SchoolBook savedBook = bookRepo.save(book);

        autoLinkBookToExistingCourses(savedBook);

        return savedBook.getUuid();
    }


    private void autoLinkBookToExistingCourses(SchoolBook book) {
        List<Course> targetCourses = new ArrayList<>();

        if (book.getOrganization() != null) {
            targetCourses = courseRepo.findAllByOrganizationAndLevel(
                    book.getOrganization().getUuid(),
                    book.getLevel().getUuid());
        }
        else if (book.getSchool() != null) {
            targetCourses = courseRepo.findAllBySchoolAndLevel(
                    book.getSchool().getUuid(),
                    book.getLevel().getUuid());
        }

        if (!targetCourses.isEmpty()) {
            for (Course course : targetCourses) {
                if (!course.getLinkedBooks().contains(book)) {
                    course.getLinkedBooks().add(book);
                }
            }
            courseRepo.saveAll(targetCourses);
            Logger.logInfo("Auto-linked book '" + book.getName() + "' to " + targetCourses.size() + " existing courses.");
        }
    }

    @Transactional
    public ResponseMessage updateBook(UUID uuid, ReqUpdateBook req) {
        SchoolBook book = bookRepo.findByUuid(uuid).orElseThrow(() -> new EntityNotFoundException(MessageKey.SCHOOL_BOOK_NOT_FOUND.getKey()));
        Level level  = levelRepo.findByUuid(req.getLevelUuid())
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.LEVEL_NOT_FOUND.getKey()));

        boolean isAuth = checkEditPermission(book);
        if(!isAuth){
            throw new PermissionForbidden(MessageKey.ACCESS_DENIED.getKey());
        }

        if (req.getName() != null) book.setName(req.getName());
        if (req.getStatus() != null) book.setStatus(req.getStatus());
        if (req.getLevelUuid() != null) book.setLevel(level);
        bookRepo.save(book);
        return new ResponseMessage("Book updated");
    }


    // org book
//    @Transactional
//    public ResponseMessage createBook(List<ReqCreateBook> reqSb) {
//        User currentUser = userService.getCurrentUser();
//        List<SchoolBook> schoolBooks = new ArrayList<>();
//
//        reqSb.forEach(req -> {
//            Level level = levelRepo.findByUuid(req.getLevelUuid()).orElseThrow();
//            SchoolBook book = new SchoolBook();
//            book.setUuid(UUID.randomUUID());
//            book.setName(req.getName());
//            book.setLevel(level);
//
//            if (currentUser.getRole().equals(UserRole.SCHOOL_DIRECTOR)) {
//                Organization org = schoolDirectorRepo.findByUser(currentUser).get().getOrganization();
//                book.setOrganization(org);
//            }
//
//            schoolBooks.add(book);
//        });
//        schoolBookRepository.saveAll(schoolBooks);
//        return new ResponseMessage("Content created at Organization level.");
//    }


    //unis

    @Transactional
    public UUID createUnit(ReqCreateUnit req) {
        SchoolBook book = bookRepo.findByUuid(req.getBookUuid())
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.SCHOOL_BOOK_NOT_FOUND.getKey()));

        // Security logic change: checkEditPermission now knows about Org-level books
        if (!checkEditPermission(book)) {
            throw new PermissionForbidden(MessageKey.ACCESS_DENIED.getKey());
        }
        unitRepo.existsByTitleAndSchoolBookId(req.getTitle(), book.getId());

        Unit unit = new Unit();
        unit.setSchoolBook(book);
        unit.setTitle(req.getTitle());
        unit.setTopic(req.getTopic());
        unit.setSortOrder(req.getSortOrder());
        unit.setStatus(UnitStatus.ACTIVE);
        unit.setUnitType(req.getType());
        return unitRepo.save(unit).getUuid();
    }

    @Transactional
    public ResponseMessage createCombinedUnit(ReqCombinedUnitExerciseTask req) {
        ReqCombinedUnitExerciseTask.ReqCombinedUnits reqUnit = req.getReqCombinedUnits();
        SchoolBook book = bookRepo.findByUuid(reqUnit.getBookUuid())
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.SCHOOL_BOOK_NOT_FOUND.getKey()));

        if (!checkEditPermission(book)) {
            throw new PermissionForbidden(MessageKey.ACCESS_DENIED.getKey());
        }

        Unit unit = new Unit();
        unit.setSchoolBook(book);
        unit.setTitle(reqUnit.getTitle());
        unit.setTopic(reqUnit.getTopic());
        unit.setSortOrder(reqUnit.getSortOrder());
        unit.setStatus(UnitStatus.ACTIVE);
        unit.setUnitType(UnitType.REGULAR);
        Unit finalUnit = unitRepo.save(unit);

        if (req.getReqCreateTasks() != null) {
            List<ExerciseTask> exerciseTasks = req.getReqCreateTasks().stream().map(taskReq -> {
                ExerciseTask newTask = new ExerciseTask();
                newTask.setUnit(finalUnit);
                newTask.setTitle(taskReq.getTitle());
                newTask.setTopic(taskReq.getTopic());
                newTask.setSortOrder(taskReq.getSortOrder());
                newTask.setSectionType(taskReq.getSectionType());
                newTask.setStatus(ExerciseTaskStatus.ACTIVE);
                return newTask;
            }).collect(Collectors.toList());
            exerciseTaskRepository.saveAll(exerciseTasks);
        }

        return new ResponseMessage("Unit and tasks created successfully at HQ/Branch level.");
    }

    @Transactional
    public void updateUnit(UUID uuid, ReqUpdateUnit req) {
        Unit unit = unitRepo.findByUuid(uuid).orElseThrow(() -> new EntityNotFoundException(MessageKey.UNIT_NOT_FOUND.getKey()));

        if (!checkEditPermission(unit.getSchoolBook())) {
            throw new PermissionForbidden(MessageKey.ACCESS_DENIED.getKey());
        }

        if (req.getTitle() != null) unit.setTitle(req.getTitle());
        if (req.getTopic() != null) unit.setTopic(req.getTopic());
        if (req.getSortOrder() != null) unit.setSortOrder(req.getSortOrder());
        if (req.getStatus() != null) unit.setStatus(req.getStatus());
        if (req.getType() != null) unit.setUnitType(req.getType());
        unitRepo.save(unit);
    }

    @Transactional
    public void deleteUnit(UUID uuid) {
        Unit unit = unitRepo.findByUuid(uuid).orElseThrow(() -> new EntityNotFoundException(MessageKey.UNIT_NOT_FOUND.getKey()));
        if (!checkEditPermission(unit.getSchoolBook())) {
            throw new PermissionForbidden(MessageKey.UNAUTHORIZED.getKey());
        }
        unit.setStatus(UnitStatus.INACTIVE);
        unitRepo.save(unit);
    }

    // task

    @Transactional
    public UUID createTask(ReqCreateTask req) {
        Unit unit = unitRepo.findByUuid(req.getUnitUuid()).orElseThrow(() -> new EntityNotFoundException(MessageKey.UNIT_NOT_FOUND.getKey()));
        boolean isAuth = checkEditPermission(unit.getSchoolBook());
        if(!isAuth){
            throw new PermissionForbidden(MessageKey.ACCESS_DENIED.getKey());
        }
        ExerciseTask task = new ExerciseTask();
        task.setUnit(unit);
        task.setTitle(req.getTitle());
        task.setTopic(req.getTopic());
        task.setSortOrder(req.getSortOrder());
        task.setSectionType(req.getSectionType());
        task.setStatus(ExerciseTaskStatus.ACTIVE);
        return taskRepo.save(task).getUuid();
    }

    @Transactional
    public List<ResVocabSet> createVocabTask(List<ReqCreateVocabSet> reqVs) {
        List<VocabularySet> vocabularySets = new ArrayList<>();
        for (ReqCreateVocabSet req : reqVs) {
            Unit unit = unitRepo.findByUuid(req.getUnitUuid())
                    .orElseThrow(() -> new EntityNotFoundException(MessageKey.UNIT_NOT_FOUND.getKey()));

            if (!checkEditPermission(unit.getSchoolBook())) {
                throw new PermissionForbidden(MessageKey.ORG_MISMATCH.getKey());
            }

            VocabularySet set = new VocabularySet();
            set.setUnit(unit);
            set.setTitle(req.getTitle());
            set.setSortOrder(req.getSortOrder());
            set.setStatus(req.getStatus() != null ? req.getStatus() : VocabularySetStatus.ACTIVE);
            vocabularySets.add(set);
        }
        return vocabularySetRepository.saveAll(vocabularySets).stream().map(ResVocabSet::new).toList();
    }

    @Transactional
    public UUID upsertVocabWord(ReqUpsertVocabWord req) {
        VocabularySet set = vocabularySetRepository.findByUuid(req.getSetUuid())
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.VOCAB_SET_NOT_FOUND.getKey()));

        if (!checkEditPermission(set.getUnit().getSchoolBook())) {
            throw new PermissionForbidden(MessageKey.ORG_MISMATCH.getKey());
        }

        VocabularyWord word = (req.getWordUuid() != null) ?
                vocabularyWordRepository.findByUuid(req.getWordUuid()).get() : new VocabularyWord();

        word.setWord(req.getWord());
        word.setTranslationUz(req.getTranslationUz());
        word.setTranslationRu(req.getTranslationRu());
        word.setTranslationTjk(req.getTranslationTjk());
        word.setTranslationKaa(req.getTranslationKaa());
        word.setTranslationKrg(req.getTranslationKrg());
        word.setDefinition(req.getDefinition());
        word.setAudioUrl(req.getAudioUrl());
        word.setAttachmentUrl(req.getAttachmentUrl());
        word.setExampleSentence(req.getExampleSentence());
        word.setTranscription(req.getTranscription());
        word.setPartOfSpeech(req.getPartOfSpeech());
        vocabularyWordRepository.save(word);

        Optional<VocabularyQuestion> existingLink = vocabularyQuestionRepository.findByVocabularyWordAndVocabularySetUuid(word, set.getUuid());
        VocabularyQuestion vq = existingLink.orElse(new VocabularyQuestion());
        vq.setVocabularySet(set);
        vq.setVocabularyWord(word);
        vq.setCoinReward(req.getCoinReward() != null ? req.getCoinReward() : 2);
        vq.setScoreReward(req.getScoreReward() != null ? req.getScoreReward() : 2);
        vq.setInstruction("Translate the word");
        vocabularyQuestionRepository.save(vq);

        return word.getUuid();
    }

    @Transactional
    public void updateTask(UUID uuid, ReqUpdateTask req) {
        ExerciseTask task = taskRepo.findByUuid(uuid).orElseThrow(() -> new EntityNotFoundException(MessageKey.EXERCISE_TASK_NOT_FOUND.getKey()));
        if (!checkEditPermission(task.getUnit().getSchoolBook())) {
            throw new PermissionForbidden(MessageKey.UNAUTHORIZED.getKey());
        }
        if (req.getTitle() != null) task.setTitle(req.getTitle());
        if (req.getTopic() != null) task.setTopic(req.getTopic());
        if (req.getSortOrder() != null) task.setSortOrder(req.getSortOrder());
        if (req.getStatus() != null) task.setStatus(req.getStatus());
        taskRepo.save(task);
    }
    @Transactional
    public void updateVocabTask(UUID uuid, ReqUpdateVocabSet req) {
        VocabularySet set = vocabularySetRepository.findByUuid(uuid).orElseThrow(() -> new EntityNotFoundException(MessageKey.VOCAB_SET_NOT_FOUND.getKey()));
        if (!checkEditPermission(set.getUnit().getSchoolBook())) {
            throw new PermissionForbidden(MessageKey.UNAUTHORIZED.getKey());
        }
        if (req.getTitle() != null) set.setTitle(req.getTitle());
        if (req.getSortOrder() != null) set.setSortOrder(req.getSortOrder());
        if (req.getStatus() != null) set.setStatus(req.getStatus());
        vocabularySetRepository.save(set);
    }

    @Transactional
    public void deleteVocabTask(UUID uuid) {
        VocabularySet task = vocabularySetRepository.findByUuid(uuid).orElseThrow(() -> new EntityNotFoundException(MessageKey.EXERCISE_TASK_NOT_FOUND.getKey()));
        boolean isAuth = checkEditPermission(task.getUnit().getSchoolBook());
        if(!isAuth){
            throw new PermissionForbidden(MessageKey.ACCESS_DENIED.getKey());
        }
        task.setStatus(VocabularySetStatus.INACTIVE);
        vocabularySetRepository.save(task);
    }

    @Transactional
    public void deleteTask(UUID uuid) {
        ExerciseTask task = taskRepo.findByUuid(uuid).orElseThrow(() -> new EntityNotFoundException(MessageKey.EXERCISE_TASK_NOT_FOUND.getKey()));
        boolean isAuth = checkEditPermission(task.getUnit().getSchoolBook());
        if(!isAuth){
            throw new PermissionForbidden(MessageKey.ACCESS_DENIED.getKey());
        }
        task.setStatus(ExerciseTaskStatus.INACTIVE);
        taskRepo.save(task);
    }





    //qs

    @Transactional
    public void addQuestionsToTask(List<ReqDetailedQuestionCreate> requests) {
        for (ReqDetailedQuestionCreate req : requests) {
            ExerciseTask task = taskRepo.findByUuid(req.getTaskUuid())
                    .orElseThrow(() -> new EntityNotFoundException(MessageKey.EXERCISE_TASK_NOT_FOUND.getKey()));

            if (!checkEditPermission(task.getUnit().getSchoolBook())) {
                throw new PermissionForbidden(MessageKey.ACCESS_DENIED.getKey());
            }

            if(task.getQuestionCount() >= 1 && (req.getType().equals(ExerciseType.SPEAKING) || req.getType().equals(ExerciseType.WRITING))){
                throw new ValidationException(MessageKey.EXERCISE_LIMIT_ONE.getKey());
            }

            ExerciseQuestion q = new ExerciseQuestion();
            q.setUuid(UUID.randomUUID());
            q.setType(req.getType());
            q.setInstruction(req.getInstruction());
            q.setCoinReward(req.getCoinReward() != null ? req.getCoinReward() : 0);
            q.setScoreReward(req.getScoreReward() != null ? req.getScoreReward() : 0);
            q.setExerciseTask(new ArrayList<>(List.of(task)));

            switch (req.getType()) {
                case GAP_FILL -> mapGapFill(q, req);
                case WRITING -> mapWriting(q, req);
                case SPEAKING -> mapSpeaking(q, req);
                case FIXING_ANSWER -> mapFixing(q, req);
                case SELECTION -> mapSelection(q, req);
                case MULTI_SELECT -> mapMultiSelect(q, req);
                case MATCHING -> mapMatching(q, req);
                case CIRCLE -> mapCircle(q, req);
                case TRACING -> mapTracing(q, req);
            }

            if (q.getContent() != null) {
                if (req.getAttachmentUrl() != null) q.getContent().setAttachmentUrl(req.getAttachmentUrl());
                if (req.getAttachmentMediaType() != null) q.getContent().setAttachmentMediaType(req.getAttachmentMediaType());
            }
            questionRepo.save(q);
        }
    }

    @Transactional
    public ResponseMessage updateQuestion(UUID uuid, ReqUpdateQuestion req) {
        ExerciseQuestion q = questionRepo.findByUuid(uuid).orElseThrow(() -> new EntityNotFoundException(MessageKey.EXERCISE_QUESTION_NOT_FOUND.getKey()));

        boolean hasAccess = q.getExerciseTask().stream()
                .anyMatch(t -> checkEditPermission(t.getUnit().getSchoolBook()));
        if (!hasAccess) throw new PermissionForbidden(MessageKey.ACCESS_DENIED.getKey());

        if (req.getInstruction() != null) q.setInstruction(req.getInstruction());
        if (req.getCoinReward() != null) q.setCoinReward(req.getCoinReward());
        if (req.getScoreReward() != null) q.setScoreReward(req.getScoreReward());

        if (req.getContent() != null) {
            q.setContent(req.getContent());

            if (req.getContent().getInstruction() != null) {
                q.setInstruction(req.getContent().getInstruction());
            }
        }

        if (req.getAnswerKey() != null) {
            q.setAnswerKey(req.getAnswerKey());
        }
        questionRepo.save(q);
        return new ResponseMessage("Question updated successfully");
    }

    @Transactional
    public void removeQuestionFromTask(UUID taskUuid, UUID questionUuid) {
        ExerciseTask task = taskRepo.findByUuid(taskUuid).orElseThrow(() -> new EntityNotFoundException(MessageKey.EXERCISE_TASK_NOT_FOUND.getKey()));
        boolean isAuth = checkEditPermission(task.getUnit().getSchoolBook());
        if(!isAuth){
            throw new PermissionForbidden(MessageKey.ACCESS_DENIED.getKey());
        }
        ExerciseQuestion question = questionRepo.findByUuid(questionUuid).orElseThrow(() -> new EntityNotFoundException(MessageKey.EXERCISE_QUESTION_NOT_FOUND.getKey()));

        question.getExerciseTask().remove(task);
        questionRepo.save(question);
    }

    @Transactional
    public ResponseMessage updateSchoolQuestion(UUID questionUuid, ReqUpdateQuestion req) {
        User user = userService.getCurrentUser(); // Add this line

        ExerciseQuestion question = questionRepo.findByUuid(questionUuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.EXERCISE_QUESTION_NOT_FOUND.getKey()));

        if (user.getRole() == UserRole.SYS_ADMIN) {
        } else {
            List<UUID> authorizedUuids = userScopeService.getAuthorizedSchoolUuids();
            boolean ownsContent = question.getExerciseTask().stream()
                    .map(task -> task.getUnit().getSchoolBook())
                    .anyMatch(book -> (book.getSchool() != null && authorizedUuids.contains(book.getSchool().getUuid())) ||
                            (book.getOrganization() != null && authorizedUuids.stream().anyMatch(sid -> schoolRepo.findByUuid(sid).get().getOrganization().equals(book.getOrganization()))));

            if (!ownsContent) {
                throw new PermissionForbidden(MessageKey.ORG_MISMATCH.getKey());
            }
        }

        if (req.getInstruction() != null) question.setInstruction(req.getInstruction());
        if (req.getCoinReward() != null) question.setCoinReward(req.getCoinReward());
        if (req.getScoreReward() != null) question.setScoreReward(req.getScoreReward());
        if (req.getContent() != null) question.setContent(req.getContent());
        if (req.getAnswerKey() != null) question.setAnswerKey(req.getAnswerKey());

        questionRepo.save(question);
        return new ResponseMessage("Question updated successfully.");
    }


    private void authorizeQuestionOwnership(ExerciseQuestion question) {
        User currentUser = userService.getCurrentUser();

        if (currentUser.getRole() == UserRole.SYS_ADMIN) return;

        UUID userSchoolId = userScopeService.getCurrentUserSchoolUuid();

        boolean ownsQuestion = question.getExerciseTask().stream()
                .map(task -> task.getUnit().getSchoolBook())
                .anyMatch(book -> book.getSchool() != null && book.getSchool().getUuid().equals(userSchoolId));

        if (!ownsQuestion) {
            throw new PermissionForbidden(MessageKey.ACCESS_DENIED.getKey());
        }
    }


    private void mapGapFill(ExerciseQuestion q, ReqDetailedQuestionCreate req) {
        GapFillContent content = new GapFillContent();
        content.setText(req.getTextWithPlaceholders());
        content.setType(ExerciseType.GAP_FILL);
        content.setInstruction(req.getInstruction());
        List<Map<String, GapFillContent.InputConfig>> inputs = new ArrayList<>();
        Map<String, List<String>> answers = new HashMap<>();
        for (ReqDetailedQuestionCreate.GapDef def : req.getGapDefinitions()) {
            GapFillContent.InputConfig cfg = new GapFillContent.InputConfig();
            cfg.setMode(def.getMode());
            cfg.setHint(def.getHint());
            cfg.setOptions(def.getOptions());
            inputs.add(Collections.singletonMap(def.getId(), cfg));
            answers.put(def.getId(), def.getCorrectAnswers());
        }
        content.setInputs(inputs);
        q.setContent(content);
        GapFillKey key = new GapFillKey();
        key.setType(ExerciseType.GAP_FILL);
        key.setAnswers(answers);
        q.setAnswerKey(key);
    }

    private void mapWriting(ExerciseQuestion q, ReqDetailedQuestionCreate req) {
        WritingContent content = new WritingContent();
        content.setType(ExerciseType.WRITING);
        content.setInstruction(req.getInstruction());
        content.setWritingQuestion(req.getPrompt());
        content.setMinWords(req.getMinWords());
        q.setContent(content);
    }

    private void mapSpeaking(ExerciseQuestion q, ReqDetailedQuestionCreate req) {
        SpeakingContent content = new SpeakingContent();
        content.setType(ExerciseType.SPEAKING);
        content.setInstruction(req.getInstruction());
        content.setSpeakingPrompt(req.getPrompt());
        content.setSeconds(req.getSecondsLimit());
        q.setContent(content);
    }

    private void mapFixing(ExerciseQuestion q, ReqDetailedQuestionCreate req) {
        // 1. Create the Content (What the student sees)
        FixingContent content = new FixingContent();
        content.setType(ExerciseType.FIXING_ANSWER);
        content.setInstruction(req.getInstruction());

        String wrongText = (req.getFixingWrongText() != null) ? req.getFixingWrongText() : req.getPrompt();
        content.setQuestionText(wrongText);
        q.setContent(content);

        FixingKey key = new FixingKey();
        key.setType(ExerciseType.FIXING_ANSWER);

        if (req.getFixingCorrectText() != null) {
            key.setCorrectText(req.getFixingCorrectText());
        } else if (req.getGapDefinitions() != null && !req.getGapDefinitions().isEmpty()) {
            key.setCorrectText(req.getGapDefinitions().get(0).getCorrectAnswers().get(0));
        }

        q.setAnswerKey(key);
    }

    private void mapSelection(ExerciseQuestion q, ReqDetailedQuestionCreate req) {
        SelectionContent content = new SelectionContent();
        content.setType(ExerciseType.SELECTION);
        content.setQuestion(req.getQuestionText());
        content.setOptions(req.getOptions().stream().map(o -> {
            Map<String, String> m = new HashMap<>();
            m.put("id", o.getId());
            if (o.getValue() != null) m.put("value", o.getValue());
            if (o.getImage() != null) m.put("image", o.getImage());
            return m;
        }).toList());
        q.setContent(content);

        SelectionKey key = new SelectionKey();
        key.setType(ExerciseType.SELECTION);
        req.getOptions().stream().filter(ReqDetailedQuestionCreate.OptionDef::isCorrect)
                .findFirst().ifPresent(o -> key.setCorrectOptionId(o.getId()));
        q.setAnswerKey(key);
    }

    private void mapMultiSelect(ExerciseQuestion q, ReqDetailedQuestionCreate req) {
        MultiSelectContent content = new MultiSelectContent();
        content.setType(ExerciseType.MULTI_SELECT);
        content.setQuestion(req.getQuestionText());
        content.setOptions(req.getOptions().stream().map(o -> {
            Map<String, String> m = new HashMap<>();
            m.put("id", o.getId());
            if (o.getValue() != null) m.put("value", o.getValue());
            if (o.getImage() != null) m.put("image", o.getImage());
            return m;
        }).toList());
        q.setContent(content);

        MultiSelectKey key = new MultiSelectKey();
        key.setType(ExerciseType.MULTI_SELECT);
        key.setCorrectOptionIds(req.getOptions().stream()
                .filter(ReqDetailedQuestionCreate.OptionDef::isCorrect)
                .map(ReqDetailedQuestionCreate.OptionDef::getId).toList());
        q.setAnswerKey(key);
    }

    private void mapMatching(ExerciseQuestion q, ReqDetailedQuestionCreate req) {
        MatchingContent content = new MatchingContent();
        content.setType(ExerciseType.MATCHING);
        content.setLeftItems(req.getLeftItems().stream().map(i -> {
            Map<String, String> m = new HashMap<>();
            m.put("id", i.getId());
            if (i.getValue() != null) m.put("value", i.getValue());
            if (i.getImage() != null) m.put("image", i.getImage());
            return m;
        }).toList());
        content.setRightItems(req.getRightItems().stream().map(i -> {
            Map<String, String> m = new HashMap<>();
            m.put("id", i.getId());
            if (i.getValue() != null) m.put("value", i.getValue());
            if (i.getImage() != null) m.put("image", i.getImage());
            return m;
        }).toList());
        q.setContent(content);

        MatchingKey key = new MatchingKey();
        key.setType(ExerciseType.MATCHING);
        Map<String, String> pairs = new HashMap<>();
        for (int i = 0; i < req.getLeftItems().size(); i++) {
            pairs.put(req.getLeftItems().get(i).getId(), req.getRightItems().get(i).getId());
        }
        key.setPairs(pairs);
        q.setAnswerKey(key);
    }

    private void mapCircle(ExerciseQuestion q, ReqDetailedQuestionCreate req) {
        uz.tune.mentourBiz.qs.CircleContent content = new uz.tune.mentourBiz.qs.CircleContent();
        content.setType(ExerciseType.CIRCLE);
        content.setParts(req.getCircleParts().stream().map(p -> {
            uz.tune.mentourBiz.qs.CircleContent.CircleWord w = new uz.tune.mentourBiz.qs.CircleContent.CircleWord();
            w.setWordId(p.getWordId());
            w.setSpace(p.isSpace());
            w.setChars(p.getChars().stream().map(c -> {
                uz.tune.mentourBiz.qs.CircleContent.CircleChar character = new uz.tune.mentourBiz.qs.CircleContent.CircleChar();
                character.setId(c.getId());
                character.setValue(c.getValue());
                return character;
            }).toList());
            return w;
        }).toList());
        q.setContent(content);

        uz.tune.mentourBiz.qs.CircleKey key = new uz.tune.mentourBiz.qs.CircleKey();
        key.setType(ExerciseType.CIRCLE);
        key.setCorrectCharIds(req.getCircleParts().stream()
                .flatMap(p -> p.getChars().stream())
                .filter(ReqDetailedQuestionCreate.OptionDef::isCorrect)
                .map(ReqDetailedQuestionCreate.OptionDef::getId).toList());
        q.setAnswerKey(key);
    }

    private void mapTracing(ExerciseQuestion q, ReqDetailedQuestionCreate req) {
        q.setContent(new uz.tune.mentourBiz.rest.payload.questionFormats.types.nodes.CircleTracingContent());
        ((uz.tune.mentourBiz.rest.payload.questionFormats.types.nodes.CircleTracingContent) q.getContent()).setTargetWord(req.getTargetWord());
        q.getContent().setType(ExerciseType.TRACING);

        uz.tune.mentourBiz.rest.repository.TracingKey key = new uz.tune.mentourBiz.rest.repository.TracingKey();
        key.setType(ExerciseType.TRACING);
        // Logic for placeholder mapping would go here if needed
        q.setAnswerKey(key);
    }
}