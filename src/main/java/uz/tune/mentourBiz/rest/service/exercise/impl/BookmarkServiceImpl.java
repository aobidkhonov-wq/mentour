package uz.tune.mentourBiz.rest.service.exercise.impl;
import uz.tune.mentourBiz.rest.enums.MessageKey;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.tune.mentourBiz.exception.EntityNotFoundException;
import uz.tune.mentourBiz.exception.PermissionForbidden;
import uz.tune.mentourBiz.rest.domain.Organization;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.BookmarkedQuestion;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.exercise.ExerciseQuestion;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.group.Enrollment;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Student;
import uz.tune.mentourBiz.rest.domain.userManagement.user.User;
import uz.tune.mentourBiz.rest.enums.BookmarkStatus;
import uz.tune.mentourBiz.rest.enums.EnrollmentStatus;
import uz.tune.mentourBiz.rest.enums.UserRole;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;
import uz.tune.mentourBiz.rest.payload.res.bookmark.ResCreateBookmark;
import uz.tune.mentourBiz.rest.payload.studentReq.req.bookmark.ReqCreateBookmark;
import uz.tune.mentourBiz.rest.payload.studentRes.res.bookmark.ResGroupBookmarks;
import uz.tune.mentourBiz.rest.payload.studentRes.res.bookmark.ResTeacherBookmark;
import uz.tune.mentourBiz.rest.repository.SchoolDirectorRepo;
import uz.tune.mentourBiz.rest.repository.group.GroupRepository;
import uz.tune.mentourBiz.rest.repository.group.enrollment.EnrollmentRepository;
import uz.tune.mentourBiz.rest.repository.student.StudentRepo;
import uz.tune.mentourBiz.rest.repository.unit.UnitRepository;
import uz.tune.mentourBiz.rest.repository.unit.exercise.BookmarkedQuestionRepository;
import uz.tune.mentourBiz.rest.repository.unit.exercise.ExerciseQuestionRepository;
import uz.tune.mentourBiz.rest.repository.user.TeacherRepository;
import uz.tune.mentourBiz.rest.service.exercise.BookmarkService;
import uz.tune.mentourBiz.rest.service.user.UserScopeService;
import uz.tune.mentourBiz.rest.service.user.UserService;
import uz.tune.mentourBiz.rest.service.user.impl.UserScopeServiceImpl;
import uz.tune.mentourBiz.utils.CoreUtils;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookmarkServiceImpl implements BookmarkService {

    private final BookmarkedQuestionRepository bookmarkRepository;
    private final UserService userService;
    private final StudentRepo studentRepository;
    private final ExerciseQuestionRepository questionRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final GroupRepository groupRepository;
    private final UnitRepository unitRepository;
    private final UserScopeServiceImpl userScopeServiceImpl;
    private final TeacherRepository teacherRepository;
    private final SchoolDirectorRepo schoolDirectorRepo;
    private final UserScopeService userScopeService;

    @Override
    @Transactional
    public ResCreateBookmark createBookmark(ReqCreateBookmark request) {
        User currentUser = userService.getCurrentUser();
        Student student = studentRepository.findByUser_Uuid(currentUser.getUuid())
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.USER_NOT_FOUND.getKey()));

        Enrollment enrollment = enrollmentRepository.findTopByStudent_User_UuidAndStatus(currentUser.getUuid(), EnrollmentStatus.ONGOING)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.GROUP_NO_ENROLLMENT.getKey()));

        ExerciseQuestion question = questionRepository.findByUuid(request.getQuestionUuid())
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.EXERCISE_QUESTION_NOT_FOUND.getKey()));

        UUID savedBookmarkUUID;
        BookmarkedQuestion bq =  bookmarkRepository.findByStudentAndQuestion(student,question);

        if(CoreUtils.isPresent(bq)){
            return new ResCreateBookmark(bq.getUuid(), "Bookmark already saved!");
        }

        else{
            BookmarkedQuestion bookmark = new BookmarkedQuestion();
            bookmark.setStudent(student);
            bookmark.setQuestion(question);
            bookmark.setComment(request.getComment());
            bookmark.setStatus(BookmarkStatus.OPEN);
            bookmark.setGroup(enrollment.getGroup());
            BookmarkedQuestion saved = bookmarkRepository.save(bookmark);
            savedBookmarkUUID = saved.getUuid();
        }

        return new ResCreateBookmark(savedBookmarkUUID, "Bookmark created, your teacher will be notified soon!");
    }

    @Override
    @Transactional
    public ResponseMessage resolveBookmark(UUID bookmarkUuid) {
        BookmarkedQuestion bookmark = bookmarkRepository.findByUuid(bookmarkUuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.BOOKMARK_NOT_FOUND.getKey()));
        User currentUser = userService.getCurrentUser();

        List<BookmarkedQuestion> tobeResolved = bookmarkRepository.
                findAllByQuestion_UuidAndGroup_Uuid(bookmark.getQuestion().getUuid(), bookmark.getGroup().getUuid());

        for(BookmarkedQuestion bq : tobeResolved){
            if(!currentUser.getRole().equals(UserRole.SYS_ADMIN)) {
                if(currentUser.getRole().equals(UserRole.SCHOOL_DIRECTOR)) {
                    Organization org = schoolDirectorRepo.findByUser(currentUser).get().getOrganization();
                    if(!bq.getGroup().getBranch().getSchool().getOrganization().equals(org)) {
                        throw new PermissionForbidden(MessageKey.ACCESS_DENIED.getKey());
                    }
                } else {
                    UUID currentUserSchooluuid = userScopeService.getCurrentUserSchoolUuid();
                    if(!bq.getGroup().getBranch().getSchool().getUuid().equals(currentUserSchooluuid)){
                        throw new PermissionForbidden(MessageKey.ACCESS_DENIED.getKey());
                    }
                }
            }
        }

        bookmarkRepository.deleteAll(tobeResolved);
        return new ResponseMessage("All similar bookmarks for this group were resolved.");
    }

    //todo test it later, unique questions for group
    @Override
    @Transactional(readOnly = true)
    public List<ResGroupBookmarks> getBookmarksForGroup(UUID groupId) {
        if (!groupRepository.existsByUuid(groupId)) {
            throw new EntityNotFoundException(MessageKey.GROUP_NOT_FOUND.getKey());
        }

        List<BookmarkedQuestion> groupBookmarks = bookmarkRepository
                .findAllByGroup_UuidAndStatusOrderByCreatedAtDesc(groupId, BookmarkStatus.OPEN);

        Map<Student, List<BookmarkedQuestion>> bookmarksByStudent = groupBookmarks.stream()
                .collect(Collectors.groupingBy(BookmarkedQuestion::getStudent));

        return bookmarksByStudent.entrySet().stream()
                .map(entry -> {
                    Student student = entry.getKey();
                    User user = student.getUser();
                    List<BookmarkedQuestion> questions = entry.getValue();

                    ResGroupBookmarks resGroupBookmarks = new ResGroupBookmarks();
                    resGroupBookmarks.setStudentUuid(user.getUuid());
                    resGroupBookmarks.setStudentFullName(user.getFirstName() + " " + user.getLastName());

                    resGroupBookmarks.setBookmarks(questions.stream()
                            .map(ResTeacherBookmark::new)
                            .collect(Collectors.toList()));

                    return resGroupBookmarks;
                })
                .toList();
    }


    @Override
    @Transactional
    public ResponseMessage deleteBookmark(UUID bookmarkUuid) {
        BookmarkedQuestion bookmark = bookmarkRepository.findByUuid(bookmarkUuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.BOOKMARK_NOT_FOUND.getKey()));
        User user = userService.getCurrentUser();
        if(user.getRole().equals(UserRole.STUDENT)) {
            Student student = studentRepository.findByUserUuid(user.getUuid())
                    .orElseThrow(() -> new EntityNotFoundException(MessageKey.USER_NOT_FOUND.getKey()));
            if(!bookmark.getStudent().getUuid().equals(student.getUuid())) {
                throw new PermissionForbidden(MessageKey.ACCESS_DENIED.getKey());
            }
        }
        else if(!user.getRole().equals(UserRole.SYS_ADMIN)) {
            UUID userSchoolUuid = userScopeServiceImpl.getCurrentUserSchoolUuid();
            if(!bookmark.getGroup().getBranch().getSchool().getUuid().equals(userSchoolUuid)){
                throw new PermissionForbidden(MessageKey.ACCESS_DENIED.getKey());
            }
        }

        bookmarkRepository.delete(bookmark);
        return new ResponseMessage("Bookmark deleted successfully.");
    }
}