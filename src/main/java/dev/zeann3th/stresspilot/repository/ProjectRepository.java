package dev.zeann3th.stresspilot.repository;

import dev.zeann3th.stresspilot.entity.ProjectEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectRepository extends JpaRepository<ProjectEntity, Long> {
    @Query(
        value =
        """
            SELECT * FROM projects
            WHERE (:name IS NULL OR LOWER(name) LIKE LOWER(CONCAT('%', :name, '%')))
        """,
        countQuery =
        """
            SELECT COUNT(*) FROM projects
            WHERE (:name IS NULL OR LOWER(name) LIKE LOWER(CONCAT('%', :name, '%')))
        """,
        nativeQuery = true
    )
    Page<ProjectEntity> findAllByCondition(@Param("name") String name, Pageable pageable);
}
