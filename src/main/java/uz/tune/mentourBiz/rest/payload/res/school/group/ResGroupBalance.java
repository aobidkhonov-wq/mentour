package uz.tune.mentourBiz.rest.payload.res.school.group;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.group.Group;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class ResGroupBalance {

    private UUID uuid;
    private String groupName;
    private ResTeacher teacher;

    private Long allStudentBalance;

    private Long overdueStudentsBalance;

    private Long overdueStudentsCount;

    public ResGroupBalance(Group group, long allStudentBalance, long overdueStudentsBalance, long overdueStudentsCount) {
        this.uuid = group.getUuid();
        this.groupName = group.getName();
        if (group.getTeacher() != null) {
            this.teacher = new ResTeacher(
                    group.getTeacher().getUser().getUuid(),
                    group.getTeacher().getUser().getFirstName() + " " + group.getTeacher().getUser().getLastName()
            );
        }
        this.allStudentBalance = allStudentBalance;
        this.overdueStudentsBalance = overdueStudentsBalance;
        this.overdueStudentsCount = overdueStudentsCount;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class ResTeacher {
        private UUID uuid;
        private String fullName;

        public ResTeacher(UUID uuid, String fullName) {
            this.uuid = uuid;
            this.fullName = fullName;
        }
    }
}
