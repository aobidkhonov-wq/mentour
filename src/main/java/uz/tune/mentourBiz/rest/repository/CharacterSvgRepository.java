package uz.tune.mentourBiz.rest.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.tune.mentourBiz.rest.domain.CharacterSvg;

import java.util.Collection;
import java.util.List;

public interface CharacterSvgRepository extends JpaRepository<CharacterSvg, Long> {
    List<CharacterSvg> findAllByCharValueIn(Collection<String> charValues);
}