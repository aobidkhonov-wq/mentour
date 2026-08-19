package uz.tune.mentourBiz.rest.repository.school;

//package uz.tune.mentourBiz.rest.repository.school;
//
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//import uz.tune.mentourBiz.base.BaseRepository;
//import uz.tune.mentourBiz.rest.domain.schoolManagement.school.SchoolClass;
//import uz.tune.mentourBiz.rest.enums.ClassStatus;
//
//import java.util.List;
//import java.util.Optional;
//import java.util.UUID;
//
//public interface SchoolClassRepo extends BaseRepository<SchoolClass> {
//    Optional<SchoolClass> findByUuid(UUID uuid);
//
//    List<SchoolClass> findAllBySchoolUuid(UUID schoolUUID);
//    Page<SchoolClass> findAllByStatus(ClassStatus status, Pageable pageable);
//
//    Optional<SchoolClass> findByReferralCode(UUID referralCode);
//}