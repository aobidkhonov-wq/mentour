package uz.tune.mentourBiz.rest.service.exercise.handler;

import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.Unit;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Student;
import uz.tune.mentourBiz.rest.payload.studentRes.res.lesson.ResLessonSection;

public interface SectionHandler {
    ResLessonSection getSectionProgress(Student student, Unit unit);
}