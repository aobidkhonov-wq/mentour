package uz.tune.mentourBiz.rest.payload.req;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Data;
import uz.tune.mentourBiz.rest.enums.FinanceEnums;

import java.util.List;
import java.util.UUID;

@Data
public class ReqPaymentPackage {
    private String name;
    private Long price;
    private Integer paymentDueDate;
    private List<UUID> courseUuid;

    @Enumerated(EnumType.STRING)
    private FinanceEnums.FinanceCurrency currency;

    private UUID schoolUuid;
}