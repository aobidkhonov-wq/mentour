package uz.tune.mentourBiz.rest.service.shop.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.tune.mentourBiz.exception.EntityNotFoundException;
import uz.tune.mentourBiz.exception.PermissionForbidden;
import uz.tune.mentourBiz.exception.ValidationException;
import uz.tune.mentourBiz.rest.domain.Attachment;
import uz.tune.mentourBiz.rest.domain.Organization;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.School;
import uz.tune.mentourBiz.rest.domain.shopManagement.ShopItem;
import uz.tune.mentourBiz.rest.domain.userManagement.user.User;
import uz.tune.mentourBiz.rest.enums.MessageKey;
import uz.tune.mentourBiz.rest.enums.ShopItemType;
import uz.tune.mentourBiz.rest.enums.UserRole;
import uz.tune.mentourBiz.rest.payload.req.ReqCreateItem;
import uz.tune.mentourBiz.rest.payload.req.ReqUpdateItem;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;
import uz.tune.mentourBiz.rest.payload.res.shop.ResShopItem;
import uz.tune.mentourBiz.rest.repository.AttachmentRepo;
import uz.tune.mentourBiz.rest.repository.SchoolDirectorRepo;
import uz.tune.mentourBiz.rest.repository.school.LibraryItemRepository;
import uz.tune.mentourBiz.rest.repository.school.SchoolRepo;
import uz.tune.mentourBiz.rest.repository.shop.ItemRedemptionRepository;
import uz.tune.mentourBiz.rest.repository.shop.ShopItemRepository;
import uz.tune.mentourBiz.rest.repository.student.StudentRepo;
import uz.tune.mentourBiz.rest.service.helper.AuthToViewEntity;
import uz.tune.mentourBiz.rest.service.shop.ShopItemService;
import uz.tune.mentourBiz.rest.service.user.UserScopeService;
import uz.tune.mentourBiz.rest.service.user.UserService;
import uz.tune.mentourBiz.utils.CoreUtils;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShopItemServiceImpl implements ShopItemService {
    private final UserService userService;
    private final UserScopeService userScopeService;
    private final ShopItemRepository shopItemRepository;
    private final AttachmentRepo attachmentRepo;
    private final StudentRepo studentRepo;
    private final ItemRedemptionRepository itemRedemptionRepository;
    private final SchoolRepo schoolRepo;
    private final LibraryItemRepository libraryItemRepository;
    private final SchoolDirectorRepo schoolDirectorRepo;
    private final AuthToViewEntity authToViewEntity;


    @Override
    @Transactional
    public ResponseMessage createItem(ReqCreateItem req) {
        User user = userService.getCurrentUser();

        if (req.getPrice() < 0) throw new ValidationException("Price cannot be negative");
        if (req.getQuantity() < 0) throw new ValidationException("Quantity cannot be negative");

        ShopItem item = new ShopItem();
        item.setName(req.getName());
        item.setDescription(req.getDescription());
        item.setPrice(req.getPrice());
        item.setQuantity(req.getQuantity());
        item.setType(req.getType() != null ? req.getType() : ShopItemType.OTHERS);
        item.setIsActive(true);

        if (req.getLogoId() != null) {
            attachmentRepo.findByUuid(req.getLogoId()).ifPresent(item::setAttachment);
        }

        // --- ROLE BASED SCOPING RULES ---

        if (user.getRole() == UserRole.SYS_ADMIN) {
            // SysAdmin: Can create for a specific school, a specific org, or Global
            if (req.getSchoolId() != null) {
                School target = schoolRepo.findByUuid(req.getSchoolId()).orElseThrow();
                item.setSchool(target);
                item.setOrganization(target.getOrganization());
            } else {
                // Both NULL = Global (Tier 1)
                item.setSchool(null);
                item.setOrganization(null);
            }
        }
        else if (user.getRole() == UserRole.SCHOOL_DIRECTOR) {
            Organization org = schoolDirectorRepo.findByUser(user)
                    .orElseThrow(() -> new EntityNotFoundException(MessageKey.DIRECTOR_NOT_FOUND.getKey()))
                    .getOrganization();

            item.setOrganization(org);
            if (req.getSchoolId() != null) {
                // Tier 3: Scoped to specific school in Org
                School target = schoolRepo.findByUuid(req.getSchoolId()).orElseThrow();
                if (!org.equals(target.getOrganization())) throw new PermissionForbidden(MessageKey.UNAUTHORIZED.getKey());
                item.setSchool(target);
            } else {
                // Tier 2: Scoped to whole Org (Shared)
                item.setSchool(null);
            }
        }
        else {
            // SCHOOL_ADMIN or TEACHER: Strictly limited to their own school
            UUID mySchoolUuid = userScopeService.getCurrentUserSchoolUuid();
            School mySchool = schoolRepo.findByUuid(mySchoolUuid).orElseThrow();

            item.setSchool(mySchool);
            item.setOrganization(mySchool.getOrganization());
        }

        shopItemRepository.save(item);
        return new ResponseMessage("Shop item created successfully.");
    }

    @Override
    public ResShopItem getOne(UUID itemUuid){
        User user = userService.getCurrentUser();
        ShopItem shopItem = shopItemRepository.findByUuid(itemUuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.SHOP_ITEM_NOT_FOUND.getKey()));
        authShopItem(user,shopItem);
        return new ResShopItem(shopItem);
    }


    @Override
    @Transactional(readOnly = true)
    public Page<ResShopItem> getAll(UUID schoolUuidParam, ShopItemType type, Pageable pageable) {
        User user = userService.getCurrentUser();

        // 1. Resolve Scope
        UUID resolvedSchoolId = userScopeService.resolveSchoolUuid(schoolUuidParam);
        Collection<UUID> schoolUuids;
        UUID orgUuid = null;

        if (user.getRole() == UserRole.SYS_ADMIN) {
            if (resolvedSchoolId != null) {
                schoolUuids = List.of(resolvedSchoolId);
                orgUuid = schoolRepo.findByUuid(resolvedSchoolId)
                        .map(s -> s.getOrganization() != null ? s.getOrganization().getUuid() : null)
                        .orElse(null);
            } else {
                return shopItemRepository.findAll(pageable).map(ResShopItem::new);
            }
        } else {
            if (resolvedSchoolId != null) {
                schoolUuids = List.of(resolvedSchoolId);
            } else {
                schoolUuids = userScopeService.getAuthorizedSchoolUuids();
            }

            if (user.getRole() == UserRole.SCHOOL_DIRECTOR) {
                orgUuid = schoolDirectorRepo.findByUser(user).get().getOrganization().getUuid();
            } else {
                UUID currentSchoolUuid = userScopeService.getCurrentUserSchoolUuid();
                if (currentSchoolUuid != null) {
                    orgUuid = schoolRepo.findByUuid(currentSchoolUuid)
                            .map(school -> school.getOrganization() != null ? school.getOrganization().getUuid() : null)
                            .orElse(null);
                }
            }
        }

        return shopItemRepository.findAllAggregated(schoolUuids, orgUuid, type, pageable)
                .map(ResShopItem::new);
    }


    @Override
    public ResponseMessage updateItem(UUID itemUuid,ReqUpdateItem req){
        User user = userService.getCurrentUser();

        School school = schoolRepo.findByUuid(req.getSchoolId())
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.SCHOOL_NOT_FOUND.getKey()));
        ShopItem shopItem = shopItemRepository.findByUuid(itemUuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.SHOP_ITEM_NOT_FOUND.getKey()));

        if(req.getQuantity() < 0) {
            throw new PermissionForbidden(MessageKey.ACCESS_DENIED.getKey());
        }
        if(req.getPrice() < 0) {
            throw new PermissionForbidden(MessageKey.ACCESS_DENIED.getKey());
        }

        authShopItem(user,shopItem);

        shopItem.setName(req.getName());
        shopItem.setDescription(req.getDescription());
        shopItem.setPrice(req.getPrice());
        shopItem.setQuantity(req.getQuantity());
        shopItem.setType(req.getType());
        shopItem.setIsActive(req.getIsActive());
        shopItem.setSchool(school);

        if(CoreUtils.isPresent(req.getLogoId())){
            Attachment attachment = attachmentRepo.findByUuid(req.getLogoId())
                    .orElseThrow(() -> new EntityNotFoundException(MessageKey.ATTACHMENT_NOT_FOUND.getKey()));
            shopItem.setAttachment(attachment);
        }
        shopItemRepository.save(shopItem);
        return new ResponseMessage("Success!");
    }

    @Transactional
    @Override
    public ResponseMessage deleteItem(UUID uuid) {
        ShopItem item = shopItemRepository.findByUuid(uuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.SHOP_ITEM_NOT_FOUND.getKey()));

        User user = userService.getCurrentUser();

        if (user.getRole().equals(UserRole.SYS_ADMIN)) {
            shopItemRepository.delete(item);
            return new ResponseMessage("Item deleted.");
        }

        Organization itemOrg = (item.getSchool() != null) ? item.getSchool().getOrganization() : item.getOrganization();

        if (user.getRole().equals(UserRole.SCHOOL_DIRECTOR)) {
            Organization directorOrg = schoolDirectorRepo.findByUser(user)
                    .orElseThrow(() -> new EntityNotFoundException(MessageKey.DIRECTOR_NOT_FOUND.getKey()))
                    .getOrganization();

            if (itemOrg != null && itemOrg.getUuid().equals(directorOrg.getUuid())) {
                shopItemRepository.delete(item);
                return new ResponseMessage("Item deleted by Director.");
            }
        }
        else if (user.getRole().equals(UserRole.SCHOOL_ADMIN) && item.getSchool() != null) {
            authToViewEntity.authorizeActionUponSchoolBroadAccess(item.getSchool());
            shopItemRepository.delete(item);
            return new ResponseMessage("Item deleted by Admin.");
        }

        throw new PermissionForbidden(MessageKey.ACCESS_DENIED.getKey());
    }

    public void authShopItem(User user, ShopItem shopItem) {
        if (user.getRole().equals(UserRole.SYS_ADMIN)) return;
        if (user.getRole().equals(UserRole.STUDENT)) throw new PermissionForbidden(MessageKey.UNAUTHORIZED.getKey());

        if (shopItem.getSchool() != null) {
            authToViewEntity.authorizeActionUponSchoolBroadAccess(shopItem.getSchool());
            return;
        }

        if (shopItem.getOrganization() != null) {
            Organization userOrg;
            if (user.getRole().equals(UserRole.SCHOOL_DIRECTOR)) {
                userOrg = schoolDirectorRepo.findByUser(user).get().getOrganization();
            } else {
                UUID schoolUuid = userScopeService.getCurrentUserSchoolUuid();
                userOrg = schoolRepo.findByUuid(schoolUuid).get().getOrganization();
            }

            if (shopItem.getOrganization().getUuid().equals(userOrg.getUuid())) {
                return;
            }
        }

        throw new PermissionForbidden(MessageKey.ACCESS_DENIED.getKey());
    }

}
