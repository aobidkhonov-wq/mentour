package uz.tune.mentourBiz.rest.endpoint.shopItem;


import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uz.tune.mentourBiz.base.BaseURI;
import uz.tune.mentourBiz.rest.enums.ItemRedemptionStatus;
import uz.tune.mentourBiz.rest.payload.res.ResOrderHistory;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;
import uz.tune.mentourBiz.rest.payload.res.shop.shopping.ResItemRdmpOne;
import uz.tune.mentourBiz.rest.service.shop.ShoppingService;

import java.util.UUID;

@RestController
@RequestMapping(BaseURI.API1 + BaseURI.SHOP + BaseURI.ITEM)
@RequiredArgsConstructor
public class ShoppingEndpoint {

    private final ShoppingService shoppingService;

    // students

    @PostMapping(BaseURI.PURCHASE + "/{itemUuid}")
    public ResponseEntity<ResponseMessage> orderItem(@PathVariable UUID itemUuid, @RequestParam Integer count){
        return ResponseEntity.ok(shoppingService.purchase(itemUuid, count));
    }

    @GetMapping(BaseURI.MY + BaseURI.ORDERS)
    public ResponseEntity<Page<ResItemRdmpOne>> getMyOrders(Pageable pageable){
        return ResponseEntity.ok(shoppingService.getMyOrders(pageable));
    }

    @GetMapping("/history")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).ORDER_HISTORY_GET)")
    public ResponseEntity<Page<ResOrderHistory>> getOrderHistory(
            @RequestParam(required = false) ItemRedemptionStatus status,
            @RequestParam(required = false) UUID schoolId,
            @RequestParam(required = false) UUID groupUuid,
            @RequestParam(required = false) String productName,
            @RequestParam(required = false) String clientName,
            @RequestParam(required = false) UUID studentUuid,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        return ResponseEntity.ok(shoppingService.getOrderHistory(
                studentUuid, status, schoolId, groupUuid, productName, clientName, pageable));
    }


    @PostMapping(BaseURI.MY + BaseURI.ORDERS + BaseURI.CANCEL + "/{orderUuid}")
    public ResponseEntity<ResponseMessage> cancelMyOrder(@PathVariable UUID orderUuid){
        return ResponseEntity.ok(shoppingService.cancelMyOrder(orderUuid));
    }

    // admin

    @GetMapping(BaseURI.SCHOOLS + "/{schoolUuid}")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).ORDERS_FOR_SCHOOL_GET)")
    public ResponseEntity<Page<ResItemRdmpOne>> getOrdersForSchool(@PathVariable UUID schoolUuid, Pageable pageable){
        return ResponseEntity.ok(shoppingService.getOrdersForSchool(schoolUuid, pageable));
    }

    @GetMapping("/{orderUuid}")
    public ResponseEntity<ResItemRdmpOne> getOneOrderForSchool(@PathVariable UUID orderUuid){
        return ResponseEntity.ok(shoppingService.getOne(orderUuid));
    }


    // sc admin
    @PostMapping(BaseURI.APPROVE + "/{orderUuid}")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).ORDER_APPROVE)")
    public ResponseEntity<ResponseMessage> approveOrder(@PathVariable UUID orderUuid){
        return ResponseEntity.ok(shoppingService.approvePurchase(orderUuid));
    }

    @PostMapping(BaseURI.DECLINE + "/{orderUuid}")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).ORDER_DECLINE)")
    public ResponseEntity<ResponseMessage> declineOrder(@PathVariable UUID orderUuid){
        return ResponseEntity.ok(shoppingService.declinePurchase(orderUuid));
    }

}
