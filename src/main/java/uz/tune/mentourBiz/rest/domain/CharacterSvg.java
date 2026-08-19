package uz.tune.mentourBiz.rest.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import uz.tune.mentourBiz.base.BaseEntity;

import java.util.List;

@Entity
@Table(name = "character_svgs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CharacterSvg extends BaseEntity {

    @Column(name = "char_value", unique = true, nullable = false)
    private String charValue;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "svg_paths", columnDefinition = "jsonb")
    private List<String> svgPaths;
}
