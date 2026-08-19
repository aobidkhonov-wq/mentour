package uz.tune.mentourBiz.rest.service.shop;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import uz.tune.mentourBiz.rest.enums.ItemRedemptionStatus;
import uz.tune.mentourBiz.rest.payload.res.ResOrderHistory;
import uz.tune.mentourBiz.rest.payload.res.shop.shopping.ResItemRdmpOne;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;

import java.util.UUID;

@Service
public interface ShoppingService {
    //students
    ResponseMessage purchase(UUID itemUuid, Integer count);
    Page<ResItemRdmpOne> getMyOrders(Pageable pageable);
    ResponseMessage cancelMyOrder(UUID itemRdmpUuid);


    Page<ResOrderHistory> getOrderHistory(UUID studentUuid, ItemRedemptionStatus status,
                                          UUID schoolUuid, UUID classUuid,
                                          String productName, String clientName,
                                          Pageable pageable);

    // admin
    Page<ResItemRdmpOne> getOrdersForSchool(UUID schoolUuid, Pageable pageable);
    ResItemRdmpOne getOne(UUID itemRdmpUuid);

    // sc admin
    ResponseMessage approvePurchase(UUID itemRdmpUuid);
    ResponseMessage declinePurchase(UUID itemRdmpUuid);

}
