package uz.tune.mentourBiz.utils;

//@Component
//@RequiredArgsConstructor
//public class SchoolExamSettingsInitializer implements ApplicationRunner {
//
//    private final SchoolRepo schoolRepo;
//    private final SchoolExamSettingsRepo settingsRepo;
//
//    @Override
//    @Transactional
//    public void run(ApplicationArguments args) {
//        List<School> allSchools = schoolRepo.findAll();
//
//        for (School school : allSchools) {
//            if (settingsRepo.findBySchool_Uuid(school.getUuid()).isEmpty()) {
//                SchoolExamSettings defaults = new SchoolExamSettings();
//                defaults.setSchool(school);
//
//                defaults.setNoScreenshot(true);
//                defaults.setSeparateSection(false);
//                defaults.setAttemptLimit(1);
//                defaults.setFreezeScreen(true);
//                defaults.setFreezeTimer(120);
//
//                defaults.setTimeLimit(60);
//
//                settingsRepo.save(defaults);
//            }
//        }
//    }
//}