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
import uz.tune.mentourBiz.rest.enums.ShopItemType;
import uz.tune.mentourBiz.rest.payload.req.ReqCreateItem;
import uz.tune.mentourBiz.rest.payload.req.ReqUpdateItem;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;
import uz.tune.mentourBiz.rest.payload.res.shop.ResShopItem;
import uz.tune.mentourBiz.rest.service.shop.ShopItemService;

import java.util.UUID;

@RestController
@RequestMapping(BaseURI.API1 + BaseURI.SHOP + BaseURI.ITEM)
@RequiredArgsConstructor
public class ShopItemEndpoint {

    private final ShopItemService shopItemService;


    @GetMapping("/{itemUuid}")
    public ResponseEntity<ResShopItem> getOne(@PathVariable UUID itemUuid) {
        return ResponseEntity.ok(shopItemService.getOne(itemUuid));
    }

    @GetMapping(BaseURI.ALL)
    public ResponseEntity<Page<ResShopItem>> getAll(@RequestParam(required = false) UUID schoolUuid, @RequestParam ShopItemType type,  @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(shopItemService.getAll(schoolUuid, type, pageable));
    }

    @PostMapping
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).ITEM_CREATE)")
    public ResponseEntity<ResponseMessage> createItem(@RequestBody ReqCreateItem request) {
        return ResponseEntity.ok(shopItemService.createItem(request));
    }


    @PatchMapping("/{itemUuid}")
    public ResponseEntity<ResponseMessage> updateItem(@PathVariable UUID itemUuid,@RequestBody ReqUpdateItem item) {
        return ResponseEntity.ok(shopItemService.updateItem(itemUuid, item));
    }

    @DeleteMapping("/{itemUuid}")
    public ResponseEntity<ResponseMessage> deleteItem(@PathVariable UUID itemUuid) {
        return ResponseEntity.ok(shopItemService.deleteItem(itemUuid));
    }
}
