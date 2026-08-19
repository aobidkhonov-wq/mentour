//package uz.tune.mentourBiz.base;
//
//import lombok.RequiredArgsConstructor;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.stereotype.Component;
//import uz.tune.mentourBiz.rest.repository.RegionRepository;
//import uz.tune.mentourBiz.rest.repository.school.SchoolRepo;
//
//
//
//@Component
//@RequiredArgsConstructor
//public class DataInitializer implements CommandLineRunner {
//
//    private final SchoolRepo schoolRepo;
//    private final RegionRepository regionRepository;
//
//    @Override
//    public void run(String... args) throws Exception {
//        updateSchools();
//    }
//
//    private void updateSchools() {
//        schoolRepo.findAll().forEach(school -> {
//            if (school.getRegion() == null) {
//                school.setUtcOffset(5);
//            } else {
//                regionRepository.findById(school.getRegion().getId()).ifPresent(region -> {
//                    school.setUtcOffset(region.getTimeUtc());
//                    schoolRepo.save(school);
//                    System.out.println("Updated school: " + school.getName() + " with UTC offset: " + region.getTimeUtc());
//                });
//            }
//        });
//        System.out.println("---------Updated Schools----------");
//    }
//}