package uz.tune.mentourBiz.rest.repository.unit.exercise.vocabulary;

import org.springframework.stereotype.Repository;
import uz.tune.mentourBiz.base.BaseRepository;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.vocabulary.VocabularySet;
import uz.tune.mentourBiz.rest.enums.VocabularySetStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VocabularySetRepository extends BaseRepository<VocabularySet> {
    List<VocabularySet> findAllByUnit_UuidAndStatusOrderBySortOrderAsc(UUID unitUuid, VocabularySetStatus status);
    List<VocabularySet> findAllByUnit_Uuid(UUID unitUuid);
    Optional<VocabularySet> findByUuid(UUID uuid);
}
