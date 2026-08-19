package uz.tune.mentourBiz.rest.service.shop.impl;
import uz.tune.mentourBiz.rest.enums.MessageKey;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.tune.mentourBiz.exception.EntityNotFoundException;
import uz.tune.mentourBiz.exception.PermissionForbidden;
import uz.tune.mentourBiz.exception.ValidationException;
import uz.tune.mentourBiz.rest.domain.shopManagement.ShopItem;
import uz.tune.mentourBiz.rest.domain.shopManagement.coins.ItemRedemption;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Student;
import uz.tune.mentourBiz.rest.domain.userManagement.user.User;
import uz.tune.mentourBiz.rest.enums.ItemRedemptionStatus;
import uz.tune.mentourBiz.rest.enums.UserRole;
import uz.tune.mentourBiz.rest.payload.res.ResOrderHistory;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;
import uz.tune.mentourBiz.rest.payload.res.shop.shopping.ResItemRdmpOne;
import uz.tune.mentourBiz.rest.repository.school.SchoolRepository;
import uz.tune.mentourBiz.rest.repository.shop.ItemRedemptionRepository;
import uz.tune.mentourBiz.rest.repository.shop.ShopItemRepository;
import uz.tune.mentourBiz.rest.repository.student.StudentRepo;
import uz.tune.mentourBiz.rest.service.helper.AuthToViewEntity;
import uz.tune.mentourBiz.rest.service.shop.ShoppingService;
import uz.tune.mentourBiz.rest.service.user.UserScopeService;
import uz.tune.mentourBiz.rest.service.user.UserService;

import java.util.Collection;
import java.util.List;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class ShoppingServiceImpl implements ShoppingService {

    private final UserScopeService userScopeService;
    private final UserService userService;
    private final StudentRepo studentRepository;
    private final ShopItemRepository shopItemRepository;
    private final StudentRepo studentRepo;
    private final ItemRedemptionRepository itemRedemptionRepository;
    private final SchoolRepository schoolRepository;
    private final AuthToViewEntity authToViewEntity;


    // students
    @Override
    public ResponseMessage purchase(UUID itemUuid, Integer count) {
        User currentUser = userService.getCurrentUser();
        Student student = studentRepo.findByUserUuid(currentUser.getUuid())
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.UNAUTHORIZED.getKey()));
        ShopItem item = shopItemRepository.findByUuid(itemUuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.SHOP_ITEM_NOT_FOUND.getKey()));
//todo make sure that student purcahses only from his school
        if (!item.getSchool().getUuid().equals(student.getSchool().getUuid())) {
            throw new PermissionForbidden(MessageKey.SHOP_WRONG_BRANCH.getKey());
        }

        if (item.getIsActive().equals(Boolean.FALSE)) {
            throw new PermissionForbidden(MessageKey.SHOP_ITEM_INACTIVE.getKey());
        }

        if (item.getQuantity() < count) {
            throw new EntityNotFoundException(MessageKey.SHOP_OUT_OF_STOCK.getKey());
        }

        long totalPrice = item.getPrice() * count;

        int updatedRows = studentRepo.subtractCoinsAtomic(student.getUuid(), totalPrice);

        if (updatedRows == 0) {
            throw new ValidationException(MessageKey.SHOP_LOW_COINS.getKey());
        }

        ItemRedemption itemRedemption = new ItemRedemption();
        itemRedemption.setShopItem(item);
        itemRedemption.setStatus(ItemRedemptionStatus.AWAITING_APPROVAL);
        itemRedemption.setStudent(student);
        itemRedemption.setCount(count);
        itemRedemptionRepository.save(itemRedemption);

        item.setQuantity(item.getQuantity() - count);
        if (item.getQuantity() == 0) {
            item.setIsActive(Boolean.FALSE);
        }
        shopItemRepository.save(item);

        return new ResponseMessage("Order is placed, please await!");
    }

    @Override
    public Page<ResItemRdmpOne> getMyOrders(Pageable pageable){
        User currentUser = userService.getCurrentUser();
        Student student = studentRepo.findByUserUuid(currentUser.getUuid())
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.UNAUTHORIZED.getKey()));

        Page<ItemRedemption> itemRedemptions = itemRedemptionRepository.findAllByStudent_Uuid(student.getUuid(), pageable);
        return itemRedemptions.map(i -> new ResItemRdmpOne(student, i.getShopItem(), i.getStatus(), i.getCount()));
    }

    @Override
    public ResponseMessage cancelMyOrder(UUID itemRdmpUuid){
        User currentUser = userService.getCurrentUser();
        Student student = studentRepo.findByUserUuid(currentUser.getUuid())
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.UNAUTHORIZED.getKey()));

        ItemRedemption itemRedemptions = itemRedemptionRepository.findByUuid(itemRdmpUuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.SHOP_ORDER_NOT_FOUND.getKey()));

        if(!itemRedemptions.getStudent().equals(student)){
            throw new PermissionForbidden(MessageKey.SHOP_ORDER_CANCEL_FORBIDDEN.getKey());
        }

        rollBack(itemRedemptions);
        return new ResponseMessage("You successfully cancelled the order. Refunds will be transferred to your balance");
    }


    @Override
    public Page<ResItemRdmpOne> getOrdersForSchool(UUID schoolUuid, Pageable pageable) {
        User currentUser = userService.getCurrentUser();

        UUID resolvedId = userScopeService.resolveSchoolUuid(schoolUuid);

        Collection<UUID> schoolUuids;
        if (resolvedId != null) {
            schoolUuids = List.of(resolvedId);
        } else {
            schoolUuids = userScopeService.getAuthorizedSchoolUuids();
        }

        Page<ItemRedemption> itemRedemptions = itemRedemptionRepository.findAllByShopItem_School_UuidIn(schoolUuids, pageable);
        return itemRedemptions.map(irmp ->
                new ResItemRdmpOne(irmp.getStudent(), irmp.getShopItem(), irmp.getStatus(), irmp.getCount()));
    }

    @Override
    public ResItemRdmpOne getOne(UUID itemRdmpUuid){
        User currentUser = userService.getCurrentUser();
        ItemRedemption itemRedemptions = itemRedemptionRepository.findByUuid(itemRdmpUuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.SHOP_ORDER_NOT_FOUND.getKey()));
        authShopItem(currentUser, itemRedemptions.getShopItem());

        return new ResItemRdmpOne(itemRedemptions.getStudent(),
                itemRedemptions.getShopItem(),
                itemRedemptions.getStatus(),
                itemRedemptions.getCount());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ResOrderHistory> getOrderHistory(UUID studentUuid, ItemRedemptionStatus status,
                                                 UUID schoolUuid, UUID classUuid,
                                                 String productName, String clientName,
                                                 Pageable pageable) {
        User currentUser = userService.getCurrentUser();

        Collection<UUID> authorizedUuids = userScopeService.getAuthorizedSchoolUuids();

        if (currentUser.getRole().equals(UserRole.STUDENT)) {
            Student student = studentRepo.findByUser(currentUser)
                    .orElseThrow(() -> new EntityNotFoundException(MessageKey.USER_NOT_FOUND.getKey()));
            return itemRedemptionRepository.findAllByStudent_Uuid(student.getUuid(), pageable)
                    .map(ResOrderHistory::new);
        }

        if (currentUser.getRole().equals(UserRole.SYS_ADMIN) && schoolUuid != null) {
            authorizedUuids = List.of(schoolUuid);
        }

        UUID teacherUuid = (currentUser.getRole().equals(UserRole.TEACHER)) ? currentUser.getUuid() : null;

        if (studentUuid != null) {
            Student targetStudent = studentRepo.findByUuid(studentUuid).orElseThrow();
            authToViewEntity.authorizeActionUponStudent(targetStudent);
            return itemRedemptionRepository.findAllByStudent_Uuid(studentUuid, pageable).map(ResOrderHistory::new);
        }

        // Note: Repository needs update to findAllOrdersFilteredMulti(Collection<UUID> ids, ...)
        return itemRedemptionRepository.findAllOrdersFilteredMulti(
                        status, authorizedUuids, classUuid, teacherUuid, productName, clientName, pageable)
                .map(ResOrderHistory::new);
    }


    // sc admin
    @Override
    @Transactional
    public ResponseMessage approvePurchase(UUID itemRdmpUuid) {
        User currentUser = userService.getCurrentUser();

        if (currentUser.getRole().equals(UserRole.TEACHER)) {
            throw new PermissionForbidden(MessageKey.UNAUTHORIZED.getKey());
        }

        ItemRedemption itemRedemptions = itemRedemptionRepository.findByUuid(itemRdmpUuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.SHOP_ORDER_NOT_FOUND.getKey()));

        authShopItem(currentUser, itemRedemptions.getShopItem());

        itemRedemptions.setStatus(ItemRedemptionStatus.APPROVED);
        itemRedemptionRepository.save(itemRedemptions);

        return new ResponseMessage("The order is approved!");
    }
    @Override
    public ResponseMessage declinePurchase(UUID itemRdmpUuid){
        User currentUser = userService.getCurrentUser();
        ItemRedemption itemRedemptions = itemRedemptionRepository.findByUuid(itemRdmpUuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.SHOP_ORDER_NOT_FOUND.getKey()));
        authShopItem(currentUser, itemRedemptions.getShopItem());


        itemRedemptions.setStatus(ItemRedemptionStatus.DECLINED);
        itemRedemptionRepository.save(itemRedemptions);

        rollBack(itemRedemptions);

        return new ResponseMessage("The order is declined!");
    }

    //helpers

    public void rollBack(ItemRedemption itemRedemptions){
        Student student = itemRedemptions.getStudent();
        ShopItem shopItem = itemRedemptions.getShopItem();

        student.setCoins(student.getCoins() + (shopItem.getPrice() * itemRedemptions.getCount()));
        shopItem.setQuantity(shopItem.getQuantity() + itemRedemptions.getCount());

        studentRepo.save(student);
        shopItemRepository.save(shopItem);
    }

    public void authShopItem(User user, ShopItem shopItem) {
        if (user.getRole().equals(UserRole.SYS_ADMIN)) return;

        List<UUID> authorizedUuids = userScopeService.getAuthorizedSchoolUuids();

        // Block students from admin-level item checks
        if (user.getRole().equals(UserRole.STUDENT)) {
            throw new PermissionForbidden(MessageKey.UNAUTHORIZED.getKey());
        }

        // Allow if the item's school is within the Director/Admin's authorized list
        if (shopItem.getSchool() == null || !authorizedUuids.contains(shopItem.getSchool().getUuid())) {
            throw new PermissionForbidden(MessageKey.ACCESS_DENIED.getKey());
        }
    }
}
