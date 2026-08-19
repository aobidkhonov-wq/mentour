package uz.tune.mentourBiz.rest.domain;

import jakarta.persistence.*;
import lombok.*;
import uz.tune.mentourBiz.base.BaseEntity;
import uz.tune.mentourBiz.rest.enums.Lang;
import java.util.UUID;


@Table(name = "school_regions")
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Region extends BaseEntity {
    @Column(name = "uuid")
    private UUID uuid = UUID.randomUUID();
    @Column(name = "name")
    private String name;
    @Column(name = "country")
    private String country;
    @Column(name = "phone_code")
    private String phoneCode;
    @Column(name = "currency")
    private String currency;
    @Enumerated(EnumType.STRING)
    @Column(name = "language")
    private Lang lang;
    @Column(name = "time_utc")
    private Integer timeUtc = 5;
}