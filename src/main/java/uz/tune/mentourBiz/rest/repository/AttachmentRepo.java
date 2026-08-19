package uz.tune.mentourBiz.rest.repository;

import uz.tune.mentourBiz.base.BaseRepository;
import uz.tune.mentourBiz.rest.domain.Attachment;

import java.util.Optional;
import java.util.UUID;

public interface AttachmentRepo extends BaseRepository<Attachment> {
    Optional<Attachment> findByUuid(UUID courseId);
}
