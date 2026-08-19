package uz.tune.mentourBiz.rest.repository;

import uz.tune.mentourBiz.base.BaseRepository;
import uz.tune.mentourBiz.rest.domain.Message;
import uz.tune.mentourBiz.rest.enums.Lang;

import java.util.Optional;

public interface MessageRepo extends BaseRepository<Message> {

    Optional<Message> findTopByKeyAndLang(String key, Lang lang);
}
