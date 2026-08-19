package uz.tune.mentourBiz.rest.domain.userManagement.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uz.tune.mentourBiz.base.BaseEntity;
import uz.tune.mentourBiz.rest.enums.UserPermission;
import uz.tune.mentourBiz.rest.enums.UserRole;

import java.util.UUID;

@Table(name = "permissions")
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Permission extends BaseEntity {

    @Column(name = "uuid")
    private UUID uuid = UUID.randomUUID();

    @Column(name = "name")
    @Enumerated(EnumType.STRING)
    private UserPermission name;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "role")
    @Enumerated(EnumType.STRING)
    private UserRole role;
}
