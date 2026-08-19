package uz.tune.mentourBiz.rest.service.shop;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import uz.tune.mentourBiz.rest.enums.ShopItemType;
import uz.tune.mentourBiz.rest.payload.req.ReqCreateItem;
import uz.tune.mentourBiz.rest.payload.req.ReqUpdateItem;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;
import uz.tune.mentourBiz.rest.payload.res.shop.ResShopItem;

import java.util.UUID;

@Service
public interface ShopItemService {
    ResShopItem getOne(UUID itemUuid);
    Page<ResShopItem> getAll(UUID schoolUuid, ShopItemType type, Pageable pageable);
    ResponseMessage updateItem(UUID itemUuid, ReqUpdateItem req);
    ResponseMessage deleteItem(UUID itemUuid);
    ResponseMessage createItem(ReqCreateItem req);
}
