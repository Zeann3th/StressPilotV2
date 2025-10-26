package dev.zeann3th.stresspilot.repository;

import dev.zeann3th.stresspilot.entity.EndpointEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EndpointRepository extends JpaRepository<EndpointEntity, Long> {
    @Query(
            """
                SELECT e FROM EndpointEntity e
                WHERE (:projectId IS NULL OR e.projectId = :projectId)
                AND (:name IS NULL OR LOWER(e.name) LIKE LOWER(CONCAT('%', :name, '%')))
            """
    )
    Page<EndpointEntity> findAllByCondition(@Param("projectId") Long projectId, @Param("name") String name, Pageable pageable);
}
