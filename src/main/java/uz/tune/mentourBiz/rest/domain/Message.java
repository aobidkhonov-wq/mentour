package uz.tune.mentourBiz.rest.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uz.tune.mentourBiz.base.BaseEntity;
import uz.tune.mentourBiz.rest.enums.Lang;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "messages", uniqueConstraints = {
        @UniqueConstraint(name = "unique_message", columnNames = {"key", "lang"})
})
public class Message extends BaseEntity {

    @Column(name = "key")
    private String key;

    @Column(name = "lang")
    @Enumerated(EnumType.STRING)
    private Lang lang;

    @Column(name = "message", columnDefinition = "text")
    private String message;

}
