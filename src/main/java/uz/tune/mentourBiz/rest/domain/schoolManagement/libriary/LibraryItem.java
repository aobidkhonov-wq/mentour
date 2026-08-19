package uz.tune.mentourBiz.rest.domain.schoolManagement.libriary;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import uz.tune.mentourBiz.base.BaseEntity;
import uz.tune.mentourBiz.rest.domain.Organization;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.School;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.group.Level;
import uz.tune.mentourBiz.rest.enums.LibraryItemType;

import java.util.List;
import java.util.UUID;

@Table(name = "library_items")
@Entity
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class LibraryItem extends BaseEntity {
    @Column(name = "uuid" , unique = true, nullable = false)
    private UUID uuid = UUID.randomUUID();

    @Column(name = "title")
    private String title;

    @Column(name = "library_item_type")
    @Enumerated(EnumType.STRING)
    private LibraryItemType type;

    @Column(name = "content_url")
    private String contentUrl;

    @Column(name = "is_global")
    private boolean isGlobal = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private LibraryItem parent;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @OneToOne()
    @JoinColumn(name = "school_uid")
    private School school;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "level_id")
    private Level level;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LibraryItem> children;

}
