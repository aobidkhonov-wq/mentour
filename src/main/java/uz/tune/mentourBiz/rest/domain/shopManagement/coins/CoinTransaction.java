package uz.tune.mentourBiz.rest.domain.shopManagement.coins;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uz.tune.mentourBiz.base.BaseEntity;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Student;
import uz.tune.mentourBiz.rest.enums.TransactionType;


import java.util.UUID;

@Table(name = "coin_transactions")
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CoinTransaction extends BaseEntity {

    @Column(name = "uuid" , unique = true, nullable = false)
    private UUID uuid = UUID.randomUUID();

    @Column(name = "amount")
    private Integer amount;

    @Column(name = "comment")
    private String comment;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    private TransactionType type;

    @Column(name = "reason")
    private String reason;

    @Column(name = "given_by")
    private String givenBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_Id")
    private Student student;
}
