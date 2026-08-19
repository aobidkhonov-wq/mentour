package uz.tune.mentourBiz.rest.repository.transaction;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uz.tune.mentourBiz.base.BaseRepository;
import uz.tune.mentourBiz.rest.domain.shopManagement.coins.CoinTransaction;
import uz.tune.mentourBiz.rest.enums.TransactionType;

import java.util.Collection;
import java.util.UUID;

@Repository
public interface CoinTransactionRepository extends BaseRepository<CoinTransaction> {
    @Query("SELECT DISTINCT ct FROM CoinTransaction ct " +
            "JOIN ct.student s JOIN s.user u JOIN s.enrollments e " +
            "WHERE (COALESCE(:schoolUuids, NULL) IS NULL OR s.school.uuid IN :schoolUuids) " +
            "AND (:teacherUuid IS NULL OR (e.group.teacher.user.uuid = :teacherUuid AND e.status = 'ONGOING')) " +
            "AND (:type IS NULL OR ct.type = :type) " +
            "AND (CAST(:studentName AS string) IS NULL OR LOWER(CONCAT(u.firstName, ' ', u.lastName)) LIKE LOWER(CONCAT('%', CAST(:studentName AS string), '%'))) " +
            "AND (CAST(:givenBy AS string) IS NULL OR LOWER(ct.givenBy) LIKE LOWER(CONCAT('%', CAST(:givenBy AS string), '%')))")
    Page<CoinTransaction> findAllFilteredMulti(
            @Param("schoolUuids") Collection<UUID> schoolUuids,
            @Param("teacherUuid") UUID teacherUuid,
            @Param("type") TransactionType type,
            @Param("studentName") String studentName,
            @Param("givenBy") String givenBy,
            Pageable pageable);
}