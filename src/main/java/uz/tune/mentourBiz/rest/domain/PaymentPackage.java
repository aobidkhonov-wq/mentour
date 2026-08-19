package uz.tune.mentourBiz.rest.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import uz.tune.mentourBiz.base.BaseEntity;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.School;
import uz.tune.mentourBiz.rest.enums.FinanceEnums;

import java.util.UUID;

@Entity
@Table(name = "payment_packages")
@Getter
@Setter
public class PaymentPackage extends BaseEntity {
    private UUID uuid = UUID.randomUUID();
    private String name;
    private Long price;

    @Column(name = "payment_due_date")
    private Integer paymentDueDate;

    @Enumerated(EnumType.STRING)
    private FinanceEnums.FinanceCurrency currency = FinanceEnums.FinanceCurrency.UZS;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id")
    private School school;
}