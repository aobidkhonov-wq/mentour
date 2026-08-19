package uz.tune.mentourBiz.rest.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uz.tune.mentourBiz.base.BaseEntity;
import uz.tune.mentourBiz.rest.model.AttachmentServerDto;

import java.util.UUID;

@Table(name = "attachments")
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Attachment extends BaseEntity {
    @Column(name = "uuid")
    private UUID uuid = UUID.randomUUID();

    @Column(name = "name")
    private String name;

    @Column(name = "original_name")
    private String originalName;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "size")
    private Long size;

    @Column(name = "full_path")
    private String fullPath;

    @Column(name = "user_id")
    private Long userId;

    public Attachment(AttachmentServerDto attachmentDto) {
        this.uuid = UUID.randomUUID();
        this.name = attachmentDto.name();
        this.originalName = attachmentDto.originalName();
        this.contentType = attachmentDto.contentType();
        this.size = attachmentDto.size();
        this.fullPath = attachmentDto.fullPath();
    }
}