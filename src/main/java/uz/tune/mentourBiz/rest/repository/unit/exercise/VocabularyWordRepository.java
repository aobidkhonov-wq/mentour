package uz.tune.mentourBiz.rest.repository.unit.exercise;

import org.springframework.stereotype.Repository;
import uz.tune.mentourBiz.base.BaseRepository;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.vocabulary.VocabularyWord;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface VocabularyWordRepository extends BaseRepository<VocabularyWord> {
    Optional<VocabularyWord> findByUuid(UUID uuid);
}