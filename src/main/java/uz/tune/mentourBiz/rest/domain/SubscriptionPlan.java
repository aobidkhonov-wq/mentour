package uz.tune.mentourBiz.rest.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uz.tune.mentourBiz.base.BaseEntity;
import uz.tune.mentourBiz.rest.enums.Currency;

import java.util.UUID;

@Entity
@Table(name = "subscription_plans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionPlan extends BaseEntity {

    @Column(name = "uuid", unique = true, nullable = false)
    private UUID uuid = UUID.randomUUID();

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Column(name = "price")
    private Long price;

    @Column(name = "currency")
    @Enumerated(EnumType.STRING)
    private Currency currency;

    @Column(name = "max_students", nullable = false)
    private Integer maxStudents;

//    @Column(name = "max_teachers")
//    private Integer maxTeachers;

//    @ElementCollection(targetClass = Subscriptions.class, fetch = FetchType.EAGER)
//    @CollectionTable(name = "plan_features", joinColumns = @JoinColumn(name = "plan_id"))
//    @Column(name = "feature")
//    @Enumerated(EnumType.STRING)
//    private Set<Subscriptions> includedFeatures;

    @Column(name = "is_active")
    private boolean isActive = true;
}