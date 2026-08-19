package uz.tune.mentourBiz.rest.service.library;
import uz.tune.mentourBiz.rest.enums.MessageKey;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.tune.mentourBiz.exception.EntityNotFoundException;
import uz.tune.mentourBiz.exception.PermissionForbidden;
import uz.tune.mentourBiz.rest.domain.Organization;
import uz.tune.mentourBiz.rest.domain.schoolManagement.libriary.LibraryItem;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.School;
import uz.tune.mentourBiz.rest.domain.userManagement.user.User;
import uz.tune.mentourBiz.rest.enums.UserRole;
import uz.tune.mentourBiz.rest.payload.ReqLibraryItem;
import uz.tune.mentourBiz.rest.payload.req.library.ReqGetLibraryItems;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;
import uz.tune.mentourBiz.rest.payload.studentRes.res.lib.ResLibraryItem;
import uz.tune.mentourBiz.rest.repository.SchoolDirectorRepo;
import uz.tune.mentourBiz.rest.repository.group.LevelRepository;
import uz.tune.mentourBiz.rest.repository.school.LibraryItemRepository;
import uz.tune.mentourBiz.rest.repository.school.SchoolRepo;
import uz.tune.mentourBiz.rest.service.helper.AuthToViewEntity;
import uz.tune.mentourBiz.rest.service.user.UserScopeService;
import uz.tune.mentourBiz.rest.service.user.UserService;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LibraryServiceImpl implements LibraryService {

    private final LibraryItemRepository libraryItemRepository;
    private final UserService userService;
    private final UserScopeService userScopeService;
    private final SchoolRepo schoolRepo;
    private final LevelRepository levelRepository;
    private final SchoolDirectorRepo schoolDirectorRepo;
    private final AuthToViewEntity authToViewEntity;


    @Transactional
    @Override
    public ResponseMessage upsertItem(ReqLibraryItem req, UUID existingUuid) {
        User user = userService.getCurrentUser();
        LibraryItem item = (existingUuid != null)
                ? libraryItemRepository.findByUuid(existingUuid).orElseThrow(() -> new EntityNotFoundException(MessageKey.SHOP_ITEM_NOT_FOUND.getKey()))
                : new LibraryItem();

        if (user.getRole().equals(UserRole.SYS_ADMIN) && Boolean.TRUE.equals(req.getIsGlobal())) {
            // Tier 1: Global System Content
            item.setGlobal(true);
            item.setSchool(null);
            item.setOrganization(null);
        } else {
            // Tier 2 & 3: Org or School Specific
            item.setGlobal(false);

            // Resolve the Organization "Owner"
            Organization org;
            if (user.getRole().equals(UserRole.SCHOOL_DIRECTOR)) {
                org = schoolDirectorRepo.findByUser(user)
                        .orElseThrow(() -> new EntityNotFoundException(MessageKey.DIRECTOR_NOT_FOUND.getKey()))
                        .getOrganization();
            } else {
                // School Admin/Teacher
                School userSchool = schoolRepo.findByUuid(userScopeService.getCurrentUserSchoolUuid())
                        .orElseThrow(() -> new EntityNotFoundException(MessageKey.SCHOOL_NOT_FOUND.getKey()));
                org = userSchool.getOrganization();
            }
            item.setOrganization(org);

            // Optional School targeting (Null = All Schools in Org)
            if (req.getSchoolUuid() != null) {
                School targetSchool = schoolRepo.findByUuid(req.getSchoolUuid())
                        .orElseThrow(() -> new EntityNotFoundException(MessageKey.SCHOOL_NOT_FOUND.getKey()));

                // Verify targeting permission for Director
                if (user.getRole().equals(UserRole.SCHOOL_DIRECTOR) &&
                        (targetSchool.getOrganization() == null || !targetSchool.getOrganization().equals(org))) {
                    throw new PermissionForbidden(MessageKey.ORG_MISMATCH.getKey());
                }
                item.setSchool(targetSchool);
            } else {
                item.setSchool(null); // Becomes Org-wide accessible
            }
        }

        item.setTitle(req.getTitle());
        item.setDescription(req.getDescription());
        item.setType(req.getType());
        item.setContentUrl(req.getContentUrl());

        if (req.getLevelUuid() != null) {
            item.setLevel(levelRepository.findByUuid(req.getLevelUuid())
                    .orElseThrow(() -> new EntityNotFoundException(MessageKey.LEVEL_NOT_FOUND.getKey())));
        }

        if (req.getParentUuid() != null) {
            item.setParent(libraryItemRepository.findByUuid(req.getParentUuid())
                    .orElseThrow(() -> new EntityNotFoundException(MessageKey.FOLDER_NOT_FOUND.getKey())));
        }

        libraryItemRepository.save(item);
        return new ResponseMessage("Library item successfully saved.");
    }
//
//    @Transactional(readOnly = true)
//    @Override
//    public List<ResBookLibrarySection> getBookMaterials(UUID bookUuid, LibraryItemType type) {
//        User user = userService.getCurrentUser();
//        UUID schoolUuid = userScopeService.getCurrentUserSchoolUuid();
//
//        List<SchoolBook> allowedBooks = schoolBookRepository.findAvailableForSchool(schoolUuid);
//        boolean hasAccess = allowedBooks.stream().anyMatch(b -> b.getUuid().equals(bookUuid));
//        if (!hasAccess && !user.getRole().equals(UserRole.SYS_ADMIN)) {
//            throw new PermissionForbidden("You do not have access to this book.");
//        }
//
//        List<Unit> units = unitRepository.findAllBySchoolBookUuid(bookUuid);
//
//        List<LibraryItem> items = libraryItemRepository.findByBookAndType(bookUuid, schoolUuid, type);
//
//        Map<UUID, List<ResLibraryItem>> groupedMaterials = items.stream()
//                .filter(i -> i.getUnit() != null)
//                .map(ResLibraryItem::new)
//                .collect(Collectors.groupingBy(
//                        m -> items.stream()
//                                .filter(ent -> ent.getUuid().equals(m.getItemUuid()))
//                                .findFirst().get().getUnit().getUuid()
//                ));
//
//        return units.stream()
//                .map(u -> new ResBookLibrarySection(
//                        u.getUuid(),
//                        u.getTitle(),
//                        u.getSortOrder(),
//                        groupedMaterials.getOrDefault(u.getUuid(), Collections.emptyList())
//                ))
//                .toList();
//    }


    @Transactional
    @Override
    public ResponseMessage deleteItem(UUID uuid) {
        LibraryItem item = libraryItemRepository.findByUuid(uuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.LIBRARY_ITEM_NOT_FOUND.getKey()));

        User currentUser = userService.getCurrentUser();

        if (currentUser.getRole().equals(UserRole.SYS_ADMIN)) {
            // Authorized
        }
        // 2. School-Specific Item: Use the Broad Access helper (Org-aware)
        else if (item.getSchool() != null) {
            authToViewEntity.authorizeActionUponSchoolBroadAccess(item.getSchool());
        }
        else if (item.getOrganization() != null) {
            if (currentUser.getRole().equals(UserRole.SCHOOL_DIRECTOR)) {
                Organization directorOrg = schoolDirectorRepo.findByUser(currentUser)
                        .orElseThrow(() -> new EntityNotFoundException(MessageKey.DIRECTOR_NOT_FOUND.getKey()))
                        .getOrganization();

                if (!item.getOrganization().getUuid().equals(directorOrg.getUuid())) {
                    throw new PermissionForbidden(MessageKey.ORG_MISMATCH.getKey());
                }
            } else {
                throw new PermissionForbidden(MessageKey.ACCESS_DENIED.getKey());
            }
        }
        // 4. Global Items: Protect from non-SysAdmins
        else if (item.isGlobal()) {
            throw new PermissionForbidden(MessageKey.ACCESS_DENIED.getKey());
        }

        libraryItemRepository.delete(item);
        return new ResponseMessage("Library item deleted successfully.");
    }



    @Override
    @Transactional(readOnly = true)
    public Page<ResLibraryItem> getLibraryItems(ReqGetLibraryItems req, Pageable pageable) {
        User user = userService.getCurrentUser();

        // 1. Resolve which schools to look at (Param > Header > All Authorized)
        UUID resolvedSchoolId = userScopeService.resolveSchoolUuid(req.getSchoolUuid());
        Collection<UUID> schoolUuids;
        UUID orgUuid = null;

        if (user.getRole() == UserRole.SYS_ADMIN) {
            if (resolvedSchoolId != null) {
                schoolUuids = List.of(resolvedSchoolId);
                // For SysAdmin, we fetch the org of that school to show shared items
                orgUuid = schoolRepo.findByUuid(resolvedSchoolId).map(s -> s.getOrganization().getUuid()).orElse(null);
            } else {
                // SysAdmin viewing everything
                return libraryItemRepository.findAll(pageable).map(ResLibraryItem::new);
            }
        } else {
            // Director or Admin
            if (resolvedSchoolId != null) {
                schoolUuids = List.of(resolvedSchoolId);
            } else {
                schoolUuids = userScopeService.getAuthorizedSchoolUuids();
            }

            if (user.getRole() == UserRole.SCHOOL_DIRECTOR) {
                orgUuid = schoolDirectorRepo.findByUser(user).get().getOrganization().getUuid();
            } else {
                UUID currentUserSchoolUuid = userScopeService.getCurrentUserSchoolUuid();
                Optional<School> byUuid = schoolRepo.findByUuid(currentUserSchoolUuid);
                if (byUuid.isPresent()) {
                    School school = byUuid.get();
                    if (school.getOrganization() != null) {
                        orgUuid = school.getOrganization().getUuid();
                    }
                }
            }
        }

        return libraryItemRepository.findLibraryAggregated(
                        req.getItemType(), req.getLevelId(), schoolUuids, orgUuid, req.getTitle(), pageable)
                .map(ResLibraryItem::new);
    }

    private void authorizeLibraryManagement(LibraryItem item) {
        User user = userService.getCurrentUser();
        if (user.getRole().equals(UserRole.SYS_ADMIN)) return;

        if (user.getRole().equals(UserRole.SCHOOL_DIRECTOR)) {
            uz.tune.mentourBiz.rest.domain.SchoolDirector director = schoolDirectorRepo.findByUser(user).get();
            if (item.getSchool() != null && item.getSchool().getOrganization().equals(director.getOrganization())) return;
            throw new PermissionForbidden(MessageKey.ORG_MISMATCH.getKey());
        }

        if (user.getRole().equals(UserRole.SCHOOL_ADMIN)) {
            UUID schoolUuid = userScopeService.getCurrentUserSchoolUuid();
            if (item.isGlobal()) {
                throw new PermissionForbidden(MessageKey.ACCESS_DENIED.getKey());
            }
            if (item.getSchool() == null || !item.getSchool().getUuid().equals(schoolUuid)) {
                throw new PermissionForbidden(MessageKey.ACCESS_DENIED.getKey());
            }
        } else {
            throw new PermissionForbidden(MessageKey.ACCESS_DENIED.getKey());
        }
    }
}