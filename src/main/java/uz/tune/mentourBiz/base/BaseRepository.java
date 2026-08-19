package uz.tune.mentourBiz.base;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface BaseRepository<R extends BaseEntity> extends JpaRepository<R, Long> {

}
