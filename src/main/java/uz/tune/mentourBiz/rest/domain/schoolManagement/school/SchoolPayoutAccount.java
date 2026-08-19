package uz.tune.mentourBiz.rest.domain.schoolManagement.school;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uz.tune.mentourBiz.base.BaseEntity;

import java.util.UUID;

@Entity
@Table(name = "school_payout_accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SchoolPayoutAccount extends BaseEntity {

    @Column(name = "uuid", unique = true, nullable = false)
    private UUID uuid = UUID.randomUUID();

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false, unique = true)
    private School school;

    @Column(name = "recipient_full_name", nullable = false)
    private String recipientFullName;

    @Column(name = "recipient_account", nullable = false, length = 20)
    private String recipientAccount;

    @Column(name = "recipient_pinfl", nullable = false, length = 14)
    private String recipientPinfl;

    @Column(name = "bank_mfo", nullable = false, length = 5)
    private String bankMfo;

    @Column(name = "contract_number", nullable = false, length = 15)
    private String contractNumber;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;
}